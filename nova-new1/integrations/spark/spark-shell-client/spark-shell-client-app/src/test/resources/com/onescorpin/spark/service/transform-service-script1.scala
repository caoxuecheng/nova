class Transform (destination: String, policies: Array[com.onescorpin.policy.rest.model.FieldPolicy], validator: com.onescorpin.spark.datavalidator.DataValidator, profiler: com.onescorpin.spark.dataprofiler.Profiler, sqlContext: org.apache.spark.sql.SQLContext, sparkContextService: com.onescorpin.spark.SparkContextService) extends com.onescorpin.spark.metadata.TransformScript(destination, policies, validator, profiler, sqlContext, sparkContextService) {
override def dataFrame: org.apache.spark.sql.DataFrame = {sqlContext.range(1,10)}
}
new Transform(tableName, policies, validator, profiler, sqlContext, sparkContextService).run()
