use nova;

SET FOREIGN_KEY_CHECKS=0;

insert into nova.AUDIT_LOG select * from onescorpin.AUDIT_LOG;

insert into nova.BATCH_EXECUTION_CONTEXT_VALUES select * from onescorpin.BATCH_EXECUTION_CONTEXT_VALUES;

insert into nova.BATCH_JOB_EXECUTION select * from onescorpin.BATCH_JOB_EXECUTION;

insert into nova.BATCH_JOB_EXECUTION_CTX_VALS select * from onescorpin.BATCH_JOB_EXECUTION_CTX_VALS;

insert into nova.BATCH_JOB_EXECUTION_PARAMS select * from onescorpin.BATCH_JOB_EXECUTION_PARAMS;

-- insert into nova.BATCH_JOB_EXECUTION_SEQ select * from onescorpin.BATCH_JOB_EXECUTION_SEQ;

insert into nova.BATCH_JOB_INSTANCE select * from onescorpin.BATCH_JOB_INSTANCE;

-- insert into nova.BATCH_JOB_SEQ select * from onescorpin.BATCH_JOB_SEQ;

insert into nova.BATCH_NIFI_JOB select * from onescorpin.BATCH_NIFI_JOB;

insert into nova.BATCH_NIFI_STEP select * from onescorpin.BATCH_NIFI_STEP;

insert into nova.BATCH_STEP_EXECUTION select * from onescorpin.BATCH_STEP_EXECUTION;

insert into nova.BATCH_STEP_EXECUTION_CTX_VALS select * from onescorpin.BATCH_STEP_EXECUTION_CTX_VALS;

-- insert into nova.BATCH_STEP_EXECUTION_SEQ select * from onescorpin.BATCH_STEP_EXECUTION_SEQ;

insert into nova.NFLOW select * from onescorpin.NFLOW;

insert into nova.NFLOW_CHECK_DATA_NFLOWS select * from onescorpin.NFLOW_CHECK_DATA_NFLOWS;

-- insert into nova.GENERATED_KEYS select * from onescorpin.GENERATED_KEYS;

update nova.GENERATED_KEYS
set VALUE_COLUMN = (SELECT VALUE_COLUMN FROM onescorpin.GENERATED_KEYS WHERE PK_COLUMN='JOB_EXECUTION_ID')
WHERE PK_COLUMN='JOB_EXECUTION_ID';

update nova.GENERATED_KEYS
set VALUE_COLUMN = (SELECT VALUE_COLUMN FROM onescorpin.GENERATED_KEYS WHERE PK_COLUMN='JOB_INSTANCE_ID')
WHERE PK_COLUMN='JOB_INSTANCE_ID';

update nova.GENERATED_KEYS
set VALUE_COLUMN = (SELECT VALUE_COLUMN FROM onescorpin.GENERATED_KEYS WHERE PK_COLUMN='STEP_EXECUTION_ID')
WHERE PK_COLUMN='STEP_EXECUTION_ID';

insert into nova.NOVA_ALERT select * from onescorpin.NOVA_ALERT;

insert into nova.NOVA_ALERT_CHANGE select * from onescorpin.NOVA_ALERT_CHANGE;

insert into nova.NOVA_VERSION select * from onescorpin.NOVA_VERSION;

CREATE TABLE IF NOT EXISTS  nova.MODESHAPE_REPOSITORY (
  ID varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  LAST_CHANGED timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONTENT longblob NOT NULL,
  PRIMARY KEY (ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into nova.MODESHAPE_REPOSITORY select * from onescorpin.MODESHAPE_REPOSITORY;

insert into nova.NIFI_EVENT select * from onescorpin.NIFI_EVENT;

insert into nova.NIFI_NFLOW_PROCESSOR_STATS select * from onescorpin.NIFI_NFLOW_PROCESSOR_STATS;

insert into nova.NIFI_RELATED_ROOT_FLOW_FILES select * from onescorpin.NIFI_RELATED_ROOT_FLOW_FILES;

insert into nova.SLA_ASSESSMENT select * from onescorpin.SLA_ASSESSMENT;

insert into nova.SLA_METRIC_ASSESSMENT select * from onescorpin.SLA_METRIC_ASSESSMENT;

insert into nova.SLA_OBLIGATION_ASSESSMENT select * from onescorpin.SLA_OBLIGATION_ASSESSMENT;

SET FOREIGN_KEY_CHECKS=1;

commit;
