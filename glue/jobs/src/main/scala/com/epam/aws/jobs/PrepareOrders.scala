package com.epam.aws.jobs

import com.amazonaws.services.glue.util.{GlueArgParser, Job, JsonOptions}
import com.amazonaws.services.glue.{ChoiceOption, GlueContext}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession

import scala.collection.JavaConverters._

object PrepareOrders {
  def main(sysArgs: Array[String]) {
    implicit lazy val spark: SparkSession = SparkSession.builder.getOrCreate()
    implicit lazy val sc: SparkContext = spark.sparkContext
    val glueContext: GlueContext = new GlueContext(sc)
    val args = GlueArgParser.getResolvedOptions(
      sysArgs,
      Array("JOB_NAME")
    )

    Job.init(
      args("JOB_NAME"),
      glueContext,
      args.asJava
    )

    val datasource0 = glueContext.getCatalogSource(
      database = "raw_area",
      tableName = "raw_orders",
      redshiftTmpDir = "",
      transformationContext = "datasource0"
    ).getDynamicFrame()

    val applymapping1 = datasource0.applyMapping(mappings = Seq(
      ("o_orderkey", "long", "orderkey", "long"),
      ("o_custkey", "long", "custkey", "long"),
      ("o_orderstatus", "string", "orderstatus", "string"),
      ("o_totalprice", "double", "totalprice", "double"),
      ("o_orderdate", "string", "orderdate", "string"),
      ("o_orderpriority", "string", "orderpriority", "string"),
      ("o_clerk", "string", "clerk", "string"),
      ("o_shippriority", "long", "shippriority", "long"),
      ("o_comment", "string", "comment", "string")
    ),
      caseSensitive = false,
      transformationContext = "applymapping1"
    )

    val resolvechoice2 = applymapping1.resolveChoice(
      choiceOption = Some(ChoiceOption("make_struct")),
      transformationContext = "resolvechoice2"
    )

    val dropnullfields3 = resolvechoice2.dropNulls(
      transformationContext = "dropnullfields3"
    )

    glueContext.getSinkWithFormat(
      connectionType = "s3",
      options = JsonOptions("""{"path": "s3://us-east-1-dev-data-lake/prepared/orders"}"""),
      transformationContext = "datasink4",
      format = "parquet"
    ).writeDynamicFrame(dropnullfields3)

    Job.commit()
  }
}