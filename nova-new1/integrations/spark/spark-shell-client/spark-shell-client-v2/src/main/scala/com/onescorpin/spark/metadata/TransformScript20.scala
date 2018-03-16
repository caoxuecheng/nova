package com.onescorpin.spark.metadata

import com.onescorpin.policy.rest.model.FieldPolicy
import com.onescorpin.spark.dataprofiler.Profiler
import com.onescorpin.spark.datavalidator.DataValidator
import com.onescorpin.spark.model.AsyncTransformResponse
import com.onescorpin.spark.rest.model.TransformResponse
import com.onescorpin.spark.{DataSet, SparkContextService}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.slf4j.LoggerFactory

/** Wraps a transform script into a function that can be evaluated.
  *
  * @param destination the name of the destination Hive table
  * @param sqlContext  the Spark SQL context
  */
abstract class TransformScript20(destination: String, policies: Array[FieldPolicy], validator: DataValidator, profiler: Profiler, sqlContext: SQLContext, sparkContextService: SparkContextService) extends TransformScript(destination, policies, validator, profiler) {

    private[this] val log = LoggerFactory.getLogger(classOf[TransformScript])

    /** Evaluated and cached transform script. */
    private[this] lazy val dataSet = {
        // Cache data frame
        val cache = dataFrame.cache
        cache.registerTempTable(destination)

        // Build response object
        sparkContextService.toDataSet(cache)
    }

    /** Evaluates this transform script and stores the result in a Hive table. */
    def run(): AsyncTransformResponse = {
        new QueryResultCallable20().toAsyncResponse(dataSet)
    }

    /** Evaluates the transform script.
      *
      * @return the transformation result
      */
    protected[metadata] def dataFrame: DataFrame

    /** Fetches or re-generates the results of the parent transformation, if available.
      *
      * @return the parent results
      */
    protected def parent: DataFrame = {
        try {
            sqlContext.read.table(parentTable)
        }
        catch {
            case e: Exception =>
                log.trace("Exception reading parent table: {}", e.toString)
                log.debug("Parent table not found: {}", parentTable)
                parentDataFrame
        }
    }

    /** Re-generates the parent transformation.
      *
      * @return the parent transformation
      */
    protected override def parentDataFrame: DataFrame = {
        throw new UnsupportedOperationException
    }

    override protected def toDataSet(rows: RDD[Row], schema: StructType): DataSet = {
        sparkContextService.toDataSet(sqlContext, rows, schema)
    }

    /** Stores the `DataFrame` results in a [[com.onescorpin.discovery.schema.QueryResult]] and returns the object. */
    private class QueryResultCallable20 extends QueryResultCallable {
        override def call(): TransformResponse = {
            toResponse(dataSet)
        }
    }

}
