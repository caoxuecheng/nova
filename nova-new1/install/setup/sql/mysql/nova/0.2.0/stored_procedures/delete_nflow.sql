use nova;
DELIMITER $$
CREATE DEFINER=`root`@`%` PROCEDURE `delete_nflow`(in category varchar(255), in nflow varchar(255))
BEGIN

CALL delete_nflow_jobs(category,nflow);
CALL delete_nflow_metadata(category,nflow);

END$$
DELIMITER ;
