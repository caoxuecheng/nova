-- -
-- #%L
-- nova-service-app
-- %%
-- Copyright (C) 2017 Onescorpin
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
-- -
CREATE OR REPLACE FUNCTION nova_job_execution_alerts() RETURNS INTEGER as $$
 DECLARE exit_loop BOOLEAN;
 DECLARE lastUpdated   BIGINT;
 DECLARE alertDescription VARCHAR(255);
 DECLARE alertContent TEXT;
 DECLARE nflowId UUID;
 DECLARE jobExecutionId BIGINT;
 DECLARE alertId UUID;
 DECLARE jobExecutionAlertIdCount INTEGER;
 DECLARE alertManagerId varchar(50) DEFAULT '885917627';




DECLARE job_cursor CURSOR FOR
select e.LAST_UPDATED,CONCAT('Failed Job ',e.JOB_EXECUTION_ID,' for nflow ',f.name) as alert_description,CONCAT('{"type":"java.lang.Long","value":"',e.JOB_EXECUTION_ID,'"}') as alert_content,
f.ID as NFLOW_ID, e.JOB_EXECUTION_ID as JOB_EXECUTION_ID
FROM BATCH_JOB_EXECUTION e
inner join BATCH_JOB_INSTANCE i on i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
inner join NFLOW f on f.id = i.NFLOW_ID
WHERE e.STATUS = 'FAILED';

BEGIN

 OPEN job_cursor;
   -- start looping
   LOOP
   FETCH job_cursor into lastUpdated,alertDescription,alertContent,nflowId,jobExecutionId;
   EXIT WHEN NOT FOUND;

 alertId := uuid_in(md5(random()::text || now()::text)::cstring);

INSERT INTO NOVA_ALERT
(id,
type,
level,
state,
create_time,
description,
cleared,
content,
sub_type,
entity_type,
entity_id)
values(alertId, 'http://nova.io/alert/job/failure','FATAL','UNHANDLED',lastUpdated,alertDescription,'N',alertContent,'Entity','NFLOW',nflowId);

INSERT INTO NOVA_ALERT_CHANGE(alert_id,state,change_time,user_name,description)
values(alertId,'UNHANDLED',lastUpdated,'dladmin',alertDescription);

SELECT COUNT(*) into jobExecutionAlertIdCount FROM BATCH_JOB_EXECUTION_CTX_VALS
WHERE KEY_NAME = 'Nova Alert Id'
and JOB_EXECUTION_ID = jobExecutionId;

IF(jobExecutionAlertIdCount = 0) then
INSERT INTO BATCH_JOB_EXECUTION_CTX_VALS(JOB_EXECUTION_ID,TYPE_CD,KEY_NAME,STRING_VAL,CREATE_DATE,ID)
values(jobExecutionId,'STRING','Nova Alert Id',concat(cast(alertId as VARCHAR(50)),':',alertManagerId),now(),uuid_in(md5(random()::text || now()::text)::cstring));
END IF;

 END LOOP;
  CLOSE job_cursor;
 return 1;
END;
$$ LANGUAGE plpgsql;
