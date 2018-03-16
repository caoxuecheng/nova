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
CREATE or replace function abandon_nflow_jobs(in nflow varchar(255), in exitMessage varchar(255), in user_name varchar(255))
  RETURNS integer AS $$
BEGIN

if user_name is null THEN
user_name := 'dladmin';
end if;


INSERT INTO NOVA_ALERT_CHANGE(alert_id,state,change_time,user_name,description)
SELECT a.id, 'HANDLED', round(EXTRACT (EPOCH FROM now()::timestamp)::float*1000), user_name, concat('Abandon all failed jobs for nflow ',nflow)
FROM BATCH_JOB_EXECUTION_CTX_VALS ctx
INNER JOIN BATCH_JOB_EXECUTION e on e.JOB_EXECUTION_ID = ctx.JOB_EXECUTION_ID
INNER JOIN BATCH_JOB_INSTANCE ON BATCH_JOB_INSTANCE.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
INNER JOIN NFLOW f on f.id = BATCH_JOB_INSTANCE.NFLOW_ID
INNER JOIN NOVA_ALERT a on a.entity_id = f.id and a.entity_type = 'NFLOW'
    WHERE BATCH_JOB_INSTANCE.JOB_NAME in ( SELECT checkNflow.NAME
    FROM NFLOW_CHECK_DATA_NFLOWS c
    inner join NFLOW f on f.name = nflow and c.NFLOW_ID = f.id
    inner join NFLOW checkNflow on checkNflow.id = c.CHECK_DATA_NFLOW_ID
    UNION
    SELECT nflow  )
  AND e.STATUS = 'FAILED'
  AND ctx.KEY_NAME = 'Nova Alert Id'
and a.id = cast(substr(ctx.STRING_VAL,1,strpos(ctx.STRING_VAL,':')-1) as UUID)
and a.state = 'UNHANDLED'
and a.type ='http://nova.io/alert/job/failure';

UPDATE NOVA_ALERT
set state = 'HANDLED'
where id in (SELECT cast(substr(STRING_VAL,1,strpos(STRING_VAL,':')-1) as UUID)
FROM BATCH_JOB_EXECUTION_CTX_VALS ctx
INNER JOIN BATCH_JOB_EXECUTION e on e.JOB_EXECUTION_ID = ctx.JOB_EXECUTION_ID
 INNER JOIN BATCH_JOB_INSTANCE
    ON BATCH_JOB_INSTANCE.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
    WHERE BATCH_JOB_INSTANCE.JOB_NAME in ( SELECT checkNflow.NAME
    FROM NFLOW_CHECK_DATA_NFLOWS c
    inner join NFLOW f on f.name = nflow and c.NFLOW_ID = f.id
    inner join NFLOW checkNflow on checkNflow.id = c.CHECK_DATA_NFLOW_ID
    UNION
    SELECT nflow)
  AND e.STATUS = 'FAILED'
  AND ctx.KEY_NAME = 'Nova Alert Id');


UPDATE BATCH_JOB_EXECUTION
SET
  STATUS = 'ABANDONED',
  EXIT_MESSAGE = CONCAT_WS(chr(10),EXIT_MESSAGE,exitMessage)
FROM BATCH_JOB_INSTANCE
WHERE BATCH_JOB_INSTANCE.JOB_INSTANCE_ID = BATCH_JOB_EXECUTION.JOB_INSTANCE_ID
AND BATCH_JOB_INSTANCE.JOB_NAME in ( SELECT checkNflow.NAME
                                       FROM NFLOW_CHECK_DATA_NFLOWS c
                                         inner join NFLOW f on f.name = nflow and c.NFLOW_ID = f.id
                                         inner join NFLOW checkNflow on checkNflow.id = c.CHECK_DATA_NFLOW_ID
                                       UNION
                                       SELECT nflow)
      AND STATUS = 'FAILED';

--   need to return a value for this procedure calls to work with spring-data-jpa repositories and named queries
return 1;

END;
$$ LANGUAGE plpgsql;
