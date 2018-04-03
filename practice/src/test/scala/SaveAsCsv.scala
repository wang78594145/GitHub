import org.apache.hadoop.mapred.Utils.OutputFileUtils.OutputFilesFilter
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}

object SaveAsCsv {
  def saveAsCsv(fileName:String,mySql:String): Unit ={
    val conf=new SparkConf().setMaster("local[4]").setAppName("MyPra")
//    val sc=new SparkContext(conf)

    val sessions=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val session = SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val input2=sessions.read.csv()
    input2.createOrReplaceTempView("")
    val input=session.read.csv(fileName)
    input.createOrReplaceTempView("CSVTable")
//    input.registerTempTable("CsvTable")
    val topTweets=session.sql(mySql).rdd.saveAsTextFile("")






  }
  def saveAsCsv2(fileName:String,mySql:String): Unit ={
    "select a,b,c from page where time = '1'"
    "/page/1.csv"
    val conf=new SparkConf().setAppName("mysc").setMaster("local[2]")
    val session=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val customSchema = StructType(Array(
      StructField("xing", StringType, true),
      StructField("name", StringType, true),
      StructField("age", IntegerType, true)))
    val input=session.read.format("com.databricks.spark.csv").option("header","true").option("inferSchema", "true").schema(customSchema).load("/Users/growingio/input")
    input.createOrReplaceTempView("readCsv")
    val read=session.sql(mySql).write.format("com.databricks.spark.csv").option("header","true").save("/Users/growingio/output")

  }




  def saveAsCsv3(outputFiles:String,mySql:String): Unit ={
    val conf=new SparkConf().setMaster("Csv3").setMaster("local[2]")
    val session=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val mySchema=StructType(Array(StructField("xing",StringType,true),StructField("name",StringType,true),StructField("age",IntegerType,true)))
    val text=session.read.format("csv").option("header","true").schema(mySchema).load("/Users/growingio/input")
    text.createOrReplaceTempView("myCsv")
    val selectedData=session.sql(mySql).write.format("csv").option("header","true").save(outputFiles)

  }
  def saveAsCsv4(output:String,mySql:String): Unit ={
    val conf=new SparkConf().setAppName("my").setMaster("local[4]")
    val session=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
    val mySchema=StructType(Array(StructField("xing",StringType,true),StructField("name",StringType,true),StructField("age",IntegerType,true)))
    val text=session.read.format("csv").option("header",true).schema(mySchema).load("/Users/growingio/input")
    text.createOrReplaceTempView("myCSV")
    val selecteData=session.sql(mySql).write.format("csv").option("","").save(output)
  }
//  def savaAsCsv5(output:String,mySql:String): Unit ={
//    val conf=new SparkConf().setMaster("local[4]").setAppName("myPra")
//    val session=SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()
//    val mySchema=StringType(Array(StructField("xing",StringType,true),StructField("name",StringType,true),StructField("age",IntegerType,true)))
//    val inputDf=session.read.format("csv").option("header",true).schema(schema = mySchema).load("/Users/growingio/input")
//    inputDf.createOrReplaceTempView("myView")
//    val selectData=session.sql(mySql).write.format("csv").option("header",true).save(output)
//
//  }
def connectHbaseByPhoenix(table: String, hostPort:String):Unit={
  val sc = new SparkContext("local", "phoenix-test")
  val sqlContext = new SQLContext(sc)
  val df = sqlContext.read.format("org.apache.phoenix.spark").options(Map("table" -> table, "zkUrl" ->hostPort)).load.registerTempTable(table)
  val sql="select * from "+table
  sqlContext.sql(sql).write.format("csv").option("header","true").save("/Users/growingio/tmp/wangxiaodong.csv")


}

  def main(args: Array[String]): Unit = {
//    saveAsCsv3("/Users/growingio/output","select age from myCsv")
     connectHbaseByPhoenix("US_POPULATION","localhost:2181")
  }



}
