use nova;
Delimiter //
create procedure check_data_job_relationships()

begin

DECLARE output VARCHAR(4000) DEFAULT '';

DECLARE checkNflowName VARCHAR(50);
DECLARE nflowName VARCHAR(50);

DECLARE cnt INT DEFAULT 0;

/* flag to determine if the cursor is complete */
DECLARE cursorDone INT DEFAULT 0;


DECLARE cur CURSOR FOR
SELECT distinct i.JOB_NAME as 'CHECK_NFLOW_NAME', x.STRING_VAL as 'NFLOW_NAME'
FROM BATCH_JOB_EXECUTION_PARAMS x
inner join (SELECT JOB_EXECUTION_ID FROM nova.BATCH_JOB_EXECUTION_PARAMS
WHERE KEY_NAME = 'jobType'
AND STRING_VAL = 'CHECK') c on c.JOB_EXECUTION_ID = x.JOB_EXECUTION_ID
inner Join BATCH_JOB_EXECUTION e on e.JOB_EXECUTION_ID = x.JOB_EXECUTION_ID
inner join BATCH_JOB_INSTANCE i on e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID
WHERE x.KEY_NAME = 'nflow';

DECLARE checkDataNflowsCur CURSOR FOR
SELECT distinct f.NAME
FROM BATCH_JOB_EXECUTION_PARAMS x
inner join (SELECT JOB_EXECUTION_ID FROM nova.BATCH_JOB_EXECUTION_PARAMS
WHERE KEY_NAME = 'jobType'
AND STRING_VAL = 'CHECK') c on c.JOB_EXECUTION_ID = x.JOB_EXECUTION_ID
inner Join BATCH_JOB_EXECUTION e on e.JOB_EXECUTION_ID = x.JOB_EXECUTION_ID
inner join BATCH_JOB_INSTANCE i on e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID
inner join NFLOW f on f.id = i.NFLOW_ID;


DECLARE CONTINUE HANDLER FOR NOT FOUND SET cursorDone = 1;

OPEN cur;
read_loop: LOOP

    FETCH cur INTO checkNflowName,nflowName;
    IF cursorDone THEN
        LEAVE read_loop;
    END IF;
   /**
   RELATE the nflows together
    */
    SELECT count(*)
    into cnt
    FROM NFLOW_CHECK_DATA_NFLOWS chk
    INNER JOIN NFLOW f on f.id = chk.nflow_id
    left join NFLOW c  on c.id = chk.check_data_nflow_id
    where f.NAME = nflowName
    AND c.NAME = checkNflowName;

    if(cnt = 0 ) then
   INSERT INTO NFLOW_CHECK_DATA_NFLOWS(`NFLOW_ID`,`CHECK_DATA_NFLOW_ID`)
   SELECT f.ID, c.ID from NFLOW f, NFLOW c
   where f.NAME = nflowName
   AND c.NAME = checkNflowName;
    end if;
END LOOP;
CLOSE cur;


SET cursorDone = 0;

OPEN checkDataNflowsCur;
read_loop: LOOP

    FETCH checkDataNflowsCur INTO checkNflowName;
    IF cursorDone THEN
        LEAVE read_loop;
    END IF;
    UPDATE NFLOW SET NFLOW_TYPE = 'CHECK'
    WHERE NFLOW.NAME = checkNflowName;

END LOOP;
CLOSE checkDataNflowsCur;



END//

-- Execute the procedure
call check_data_job_relationships();

-- Drop the procedure
drop procedure check_data_job_relationships;
