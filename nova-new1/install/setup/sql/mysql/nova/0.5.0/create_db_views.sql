use nova;

/**
 View that maps a Check Data Nflow to its corresponding Job Nflow
 */
CREATE OR REPLACE VIEW CHECK_DATA_TO_NFLOW_VW as
SELECT NFLOW_ID NFLOW_ID, f2.NAME as NFLOW_NAME, check_nflows.CHECK_DATA_NFLOW_ID as NOVA_NFLOW_ID, f.NAME as NOVA_NFLOW_NAME
 FROM NFLOW_CHECK_DATA_NFLOWS check_nflows
 INNER JOIN NFLOW f on f.ID = check_nflows.CHECK_DATA_NFLOW_ID
 INNER JOIN NFLOW f2 on f2.ID = check_nflows.NFLOW_ID
 WHERE f.NFLOW_TYPE = 'CHECK'
UNION ALL
SELECT ID,NAME,id, NAME from NFLOW
WHERE NFLOW_TYPE = 'NFLOW';


/**
Get the health of the nflow merging the Check data job health into the correct nflow for summarizing the counts
 */
CREATE OR REPLACE VIEW BATCH_NFLOW_SUMMARY_COUNTS_VW AS
SELECT f.NFLOW_ID as NFLOW_ID,f.NFLOW_NAME as NFLOW_NAME,
       count(e2.JOB_EXECUTION_ID) as ALL_COUNT,
       count(case when e2.status <>'ABANDONED' AND (e2.status = 'FAILED' or e2.EXIT_CODE = 'FAILED') then 1 else null end) as FAILED_COUNT,
       count(case when e2.status <>'ABANDONED' AND (e2.EXIT_CODE = 'COMPLETED') then 1 else null end) as COMPLETED_COUNT,
       count(case when e2.status = 'ABANDONED'then 1 else null end) as ABANDONED_COUNT,
        count(case when e2.status IN('STARTING','STARTED')then 1 else null end) as RUNNING_COUNT
FROM   BATCH_JOB_EXECUTION e2
INNER JOIN BATCH_JOB_INSTANCE i on i.JOB_INSTANCE_ID = e2.JOB_INSTANCE_ID
INNER JOIN CHECK_DATA_TO_NFLOW_VW f on f.NOVA_NFLOW_ID = i.NFLOW_ID
group by f.nflow_id, f.nflow_name;


/**
Get the nflow and the last time it completed
 */
CREATE OR REPLACE  VIEW LATEST_NFLOW_JOB_END_TIME_VW AS
    SELECT f.id as NFLOW_ID, MAX(e.END_TIME) END_TIME
    FROM
       BATCH_JOB_EXECUTION e
       INNER JOIN BATCH_JOB_INSTANCE i on i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
       INNER JOIN NFLOW f on f.id = i.NFLOW_ID
       GROUP by f.id;


/**
Latest JOB EXECUTION grouped by Nflow
 */
CREATE OR REPLACE VIEW LATEST_NFLOW_JOB_VW AS
          SELECT f.id as NFLOW_ID, MAX(e.JOB_EXECUTION_ID) JOB_EXECUTION_ID
    FROM
       BATCH_JOB_EXECUTION e
       INNER JOIN BATCH_JOB_INSTANCE i on i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
       INNER JOIN NFLOW f on f.id = i.NFLOW_ID
       GROUP by f.id;


/**
 get the nflow and the latest job that has been finished
 */
CREATE OR REPLACE VIEW `nova`.`LATEST_FINISHED_NFLOW_JOB_VW` AS
SELECT f.ID as NFLOW_ID,f.NAME as NFLOW_NAME,
       f.NFLOW_TYPE as NFLOW_TYPE,
       e.JOB_EXECUTION_ID as JOB_EXECUTION_ID,
       i.JOB_INSTANCE_ID as JOB_INSTANCE_ID,
       e.START_TIME,
       e.END_TIME,
       e.STATUS,
       e.EXIT_CODE,
       e.EXIT_MESSAGE
FROM   BATCH_JOB_EXECUTION e
INNER JOIN BATCH_JOB_INSTANCE i on i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
INNER JOIN NFLOW f on f.ID = i.NFLOW_ID
inner JOIN LATEST_NFLOW_JOB_END_TIME_VW maxJobs
                             on maxJobs.NFLOW_ID = f.ID
                             and maxJobs.END_TIME =e.END_TIME;



CREATE OR REPLACE VIEW NFLOW_HEALTH_VW AS
SELECT summary.NFLOW_ID as NFLOW_ID,
	   summary.NFLOW_NAME as NFLOW_NAME,
       e.JOB_EXECUTION_ID as JOB_EXECUTION_ID,
       i.JOB_INSTANCE_ID as JOB_INSTANCE_ID,
       e.START_TIME,
       e.END_TIME,
       e.STATUS,
       e.EXIT_CODE,
       e.EXIT_MESSAGE,
       summary.FAILED_COUNT,
       summary.COMPLETED_COUNT,
       summary.ABANDONED_COUNT,
       summary.ALL_COUNT,
       summary.RUNNING_COUNT
FROM   BATCH_JOB_EXECUTION e
INNER JOIN BATCH_JOB_INSTANCE i on i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
inner join BATCH_NFLOW_SUMMARY_COUNTS_VW summary on summary.NFLOW_ID = i.NFLOW_ID
inner JOIN LATEST_NFLOW_JOB_VW maxJobs
                             on maxJobs.NFLOW_ID = summary.NFLOW_ID
                             and maxJobs.JOB_EXECUTION_ID =e.JOB_EXECUTION_ID;
