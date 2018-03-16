use nova;
DELIMITER $$
CREATE DEFINER=`root`@`%` PROCEDURE `delete_nflow_metadata`(in systemCategoryName varchar(255), in systemNflowName varchar(255))
BEGIN

DECLARE categoryId varchar(255);
DECLARE nflowId varchar(255);
DECLARE recordExists int default 0;
DECLARE output VARCHAR(4000) default '';

SELECT COUNT(*) into recordExists
FROM CATEGORY WHERE NAME = systemCategoryName;

 IF(recordExists >0) THEN
    SELECT HEX(id) into categoryId from CATEGORY WHERE NAME = systemCategoryName;
    SELECT COUNT(*) into recordExists FROM NFLOW WHERE NAME = systemNflowName and hex(category_id) = categoryId;
    SELECT CONCAT(output,'\n','CATEGORY ID IS :',HEX(categoryId)) into output;

    IF(recordExists >0) THEN

        SELECT hex(id) into nflowId FROM NFLOW WHERE NAME = systemNflowName and HEX(category_id) = categoryId;
        SELECT CONCAT(output,'\n','NFLOW ID IS :',HEX(nflowId)) into output;
        DELETE FROM FM_NFLOW WHERE hex(id) = nflowId;
        DELETE FROM NFLOW WHERE hex(id) = nflowId;
        SELECT  CONCAT(output,'\n','SUCCESSFULLY REMOVED THE NFLOW METADATA ',systemNflowName) into output;

    ELSE
    SELECT CONCAT(output,'\n','UNABLE TO FIND NFLOW ',systemNflowName) into output;
    END IF;
 ELSE
    SELECT CONCAT(output,'\n','UNABLE TO FIND CATEGORY ',systemCategoryName) into output;
END IF;


SELECT output;

END$$
DELIMITER ;
