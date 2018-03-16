SET SQL_MODE='ALLOW_INVALID_DATES';
use nova;
delimiter //

create procedure update_to_050()

begin

CREATE TABLE IF NOT EXISTS `AUDIT_LOG` (
  `id` binary(16) NOT NULL,
  `create_time` timestamp,
  `user` varchar(100) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `log_type` varchar(45) DEFAULT NULL,
  `entity_id` varchar(45), 
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;


/**
If the VARCHAR version of the NFLOW_ID was added by mistake remove the column.
 */
IF EXISTS(SELECT table_name
            FROM INFORMATION_SCHEMA.COLUMNS
           WHERE table_schema = 'nova'
             AND table_name = 'BATCH_JOB_INSTANCE' and column_name = 'NFLOW_ID' and lower(DATA_TYPE) = 'varchar') THEN

   ALTER TABLE BATCH_JOB_INSTANCE
             DROP COLUMN NFLOW_ID;
END IF;

/**
Add the NFLOW_ID column
 */
IF NOT EXISTS(SELECT table_name
            FROM INFORMATION_SCHEMA.COLUMNS
           WHERE table_schema = 'nova'
             AND table_name = 'BATCH_JOB_INSTANCE' and column_name = 'NFLOW_ID') then
/** Adds the NFLOW column to the INSTANCE TABLE */
ALTER TABLE BATCH_JOB_INSTANCE
ADD COLUMN `NFLOW_ID` BINARY(16) NULL;
end if;




/**
CREATE the table to store the Check data nflow refs
 */
CREATE TABLE IF NOT EXISTS NFLOW_CHECK_DATA_NFLOWS (
  `NFLOW_ID` BINARY(16) NOT NULL COMMENT '',
  `CHECK_DATA_NFLOW_ID` BINARY(16) NOT NULL COMMENT '',
  PRIMARY KEY (`NFLOW_ID`, `CHECK_DATA_NFLOW_ID`));

/**
Set the Nflow ID to the respective JOB_INSTANCE table
 */
UPDATE BATCH_JOB_INSTANCE i
INNER JOIN NFLOW f on f.NAME = i.JOB_NAME
SET i.NFLOW_ID = f.ID;



END//

delimiter ;

-- Execute the procedure
call update_to_050();

-- Drop the procedure
drop procedure update_to_050;
