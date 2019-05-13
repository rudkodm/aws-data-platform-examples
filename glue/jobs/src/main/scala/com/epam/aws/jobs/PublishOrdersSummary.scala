package com.epam.aws.jobs

import com.amazonaws.services.glue.util.{GlueArgParser, Job, JsonOptions}
import com.amazonaws.services.glue.{DynamicFrame, GlueContext}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConverters._

object PublishOrdersSummary {
  def main(sysArgs: Array[String]) {
    implicit lazy val spark: SparkSession = SparkSession.builder.getOrCreate()
    implicit lazy val sc: SparkContext = spark.sparkContext
    val glueContext: GlueContext = new GlueContext(sc)
    val args = GlueArgParser.getResolvedOptions(sysArgs, Seq("JOB_NAME").toArray)

    Job.init(
      args("JOB_NAME"),
      glueContext,
      args.asJava
    )

    val orders = glueContext.getCatalogSource(
      database = "prepared_area",
      tableName = "prepared_orders",
      redshiftTmpDir = "",
      transformationContext = "orders"
    ).getDynamicFrame()

    orders.toDF().createOrReplaceTempView("orders")

    val resDf = spark.sql("""
    SELECT
        cast(orderdate as DATE) as orderdate,
        count(distinct orderkey) AS orders,
        count(distinct custkey) AS customers,
        sum(cast (totalprice as DECIMAL(16,5))) AS net_sales
    FROM orders
    GROUP BY orderdate
    """)
    val res = DynamicFrame(resDf, glueContext)

    glueContext.getSinkWithFormat(
      connectionType = "s3",
      options = JsonOptions("""{"path": "s3://us-east-1-dev-data-lake/publish/orders_report"}"""),
      transformationContext = "sink",
      format = "parquet"
    ).writeDynamicFrame(res)

    Job.commit()
  }
}