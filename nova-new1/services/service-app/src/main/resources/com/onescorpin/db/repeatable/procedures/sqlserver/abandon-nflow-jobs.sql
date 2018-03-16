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

CREATE PROCEDURE [dbo].[abandon_nflow_jobs](@nflow varchar(255), @exitMessage varchar(255), @username varchar(255), @res integer OUTPUT)
AS

IF (@username IS NULL)
  SET @username = 'dladmin';


INSERT INTO NOVA_ALERT_CHANGE(alert_id,state,change_time,user_name,description)
SELECT a.id, 'HANDLED', FLOOR((CAST(GetUTCDate() as float) - 25567.0) * 86400000.0), @username, concat('Abandon all failed jobs for nflow ', @nflow)
FROM BATCH_JOB_EXECUTION_CTX_VALS ctx
INNER JOIN BATCH_JOB_EXECUTION e on e.JOB_EXECUTION_ID = ctx.JOB_EXECUTION_ID
INNER JOIN BATCH_JOB_INSTANCE ON BATCH_JOB_INSTANCE.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
INNER JOIN NFLOW f on f.id = BATCH_JOB_INSTANCE.NFLOW_ID
INNER JOIN NOVA_ALERT a on a.entity_id = f.id and a.entity_type = 'NFLOW'
  WHERE BATCH_JOB_INSTANCE.JOB_NAME in (
    SELECT checkNflow.NAME
    FROM NFLOW_CHECK_DATA_NFLOWS c
    INNER JOIN NFLOW f on f.name = @nflow and c.NFLOW_ID = f.id
    INNER JOIN NFLOW checkNflow on checkNflow.id = c.CHECK_DATA_NFLOW_ID
    UNION
    SELECT @nflow
  )
  AND e.STATUS = 'FAILED'
  AND ctx.KEY_NAME = 'Nova Alert Id'
and a.id = CONVERT(BINARY(16), REPLACE(SUBSTRING(ctx.STRING_VAL, 1, CHARINDEX(':', ctx.STRING_VAL) - 1), '-', ''), 2)
and a.state = 'UNHANDLED'
and a.type ='http://nova.io/alert/job/failure';

UPDATE NOVA_ALERT
SET state = 'HANDLED'
WHERE id in (
  SELECT CONVERT(BINARY(16), REPLACE(SUBSTRING(ctx.STRING_VAL, 1, CHARINDEX(':', ctx.STRING_VAL) - 1), '-', ''), 2)
  FROM BATCH_JOB_EXECUTION_CTX_VALS ctx
  INNER JOIN BATCH_JOB_EXECUTION e on e.JOB_EXECUTION_ID = ctx.JOB_EXECUTION_ID
  INNER JOIN BATCH_JOB_INSTANCE
    ON BATCH_JOB_INSTANCE.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
  WHERE BATCH_JOB_INSTANCE.JOB_NAME in (
    SELECT checkNflow.NAME
    FROM NFLOW_CHECK_DATA_NFLOWS c
    inner join NFLOW f on f.name = @nflow and c.NFLOW_ID = f.id
    inner join NFLOW checkNflow on checkNflow.id = c.CHECK_DATA_NFLOW_ID
    UNION
    SELECT @nflow
  )
  AND e.STATUS = 'FAILED'
  AND ctx.KEY_NAME = 'Nova Alert Id'
);


UPDATE BATCH_JOB_EXECUTION
SET
  STATUS = 'ABANDONED',
  EXIT_MESSAGE = CONCAT_WS(CHAR(10),EXIT_MESSAGE,@exitMessage)
FROM BATCH_JOB_INSTANCE
WHERE BATCH_JOB_INSTANCE.JOB_INSTANCE_ID = BATCH_JOB_EXECUTION.JOB_INSTANCE_ID
AND BATCH_JOB_INSTANCE.JOB_NAME in (
  SELECT checkNflow.NAME
  FROM NFLOW_CHECK_DATA_NFLOWS c
  inner join NFLOW f on f.name = @nflow and c.NFLOW_ID = f.id
  inner join NFLOW checkNflow on checkNflow.id = c.CHECK_DATA_NFLOW_ID
  UNION
  SELECT @nflow
)
AND STATUS = 'FAILED';

--   need to return a value for this procedure calls to work on postgresql and with spring-data-jpa repositories and named queries
set @res = 1;
