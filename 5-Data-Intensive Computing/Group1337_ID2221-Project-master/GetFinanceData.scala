package twitterstreaming

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.SQLContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.apache.spark.sql.functions._
import java.sql.Date
import java.text.SimpleDateFormat
import org.apache.log4j.{Level, Logger}

import scala.io.Source._


object GetFinanceData {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)
    val spark = SparkSession.builder.master("local[*]").appName("TwitterStreaming").getOrCreate()

    println("Args" + args.length)

    var tempArr = Array("", "", "", "")
    for (x <- 0 until args.length) {
      tempArr(x) = args(x)
    }

    val twitterDate = Option(tempArr(0)).filterNot(_.isEmpty).getOrElse("2018-10-11")
    val twitterTime = Option(tempArr(1)).filterNot(_.isEmpty).getOrElse("08:00")
    val addHours_Twitter = Option(tempArr(2)).filterNot(_.isEmpty).getOrElse("3").toInt
    val addHours_Yahoo = Option(tempArr(3)).filterNot(_.isEmpty).getOrElse("1").toInt + addHours_Twitter

    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    val twitterDate_Parsed = dateFormat.parse(twitterDate + " " + twitterTime)

    val hour = 3600 * 1000;
    val twitterDate_Start = dateFormat.format(new Date(twitterDate_Parsed.getTime() + 0 * hour)).toString;
    val twitterDate_End = dateFormat.format(new Date(twitterDate_Parsed.getTime() + addHours_Twitter * hour)).toString;

    val yahooDate_Start = dateFormat.format(new Date(twitterDate_Parsed.getTime() + (addHours_Twitter - 1) * hour)).toString;
    val yahooDate_End = dateFormat.format(new Date(twitterDate_Parsed.getTime() + addHours_Yahoo * hour)).toString;

    println("")
    println("Checking Twitter data from " + twitterDate_Start + " to " + twitterDate_End)
    println("Comparing results on Yahoo from " + yahooDate_Start + " to " + yahooDate_End)
    println("")

    val predictResult = get_twitter_results(spark, twitterDate_Start, twitterDate_End)
    if (predictResult != ("STOP", "STOP", "STOP")) {
      val realResult = get_yahoo_finance_intraday(spark, yahooDate_Start, yahooDate_End, "AAPL")

      if (realResult != "STOP") {

        println("Tweet results are:" + predictResult)
        println("Yahoo data is:" + realResult)

        println("Using the Main sentiment analysis")
        if (realResult == predictResult._1) {
          println("Our prediction was correct! :) ")
        }
        else {
          println("Our prediction was incorrect! :( ")
        }
        println("Using the Average sentiment analysis")

        if (realResult == predictResult._2) {
          println("Our prediction was correct! :) ")
        }
        else {
          println("Our prediction was incorrect! :( ")
        }
        println("Using the Weighted sentiment analysis")

        if (realResult == predictResult._3) {
          println("Our prediction was correct! :) ")
        }
        else {
          println("Our prediction was incorrect! :( ")
        }
      }
    }

  }

  def get_twitter_results(spark: SparkSession, startDate: String, endDate: String): (String, String, String) = {
    // Apple - AAPL
    // Microsoft - MSFT
    try {
      import spark.implicits._

      // Load parquet data to table
      val loadDF = spark.read.parquet("data")
      loadDF.registerTempTable("myData")
      // Cast the datetime to timestamp format and add to table
      val sqlDF = spark.sql("select * from myData").withColumn("Datetime", (col("Date").cast("timestamp")))
      //sqlDF.show()
      // Filter on data we are interested in
      val dateFilter = sqlDF.filter(sqlDF("Datetime") > lit(startDate) && sqlDF("Datetime") < lit(endDate))
      // Group by sentiment key and count
      val mapFilter_Main = dateFilter.map(x => (x(2).toString, 1))

      // Order descending and pick the top row
      var newSQL_Main = mapFilter_Main.groupBy(mapFilter_Main("_1")).agg(sum(mapFilter_Main("_2"))).orderBy(desc("sum(_2)"))
      newSQL_Main = newSQL_Main.withColumnRenamed("_1", "Main Sentiments").withColumnRenamed("sum(_2)", "Count")
      newSQL_Main.show()
      val finalResult_Main = newSQL_Main.limit(1).select("Main Sentiments").as[String].collect()(0).toString

      val mapFilter_Avg = dateFilter.map(x => (x(3).toString, 1))
      var newSQL_Avg = mapFilter_Avg.groupBy(mapFilter_Avg("_1")).agg(sum(mapFilter_Avg("_2"))).orderBy(desc("sum(_2)"))
      newSQL_Avg = newSQL_Avg.withColumnRenamed("_1", "Avg Sentiments").withColumnRenamed("sum(_2)", "Count")
      newSQL_Avg.show()
      val finalResult_Avg = newSQL_Avg.limit(1).select("Avg Sentiments").as[String].collect()(0).toString

      val mapFilter_Weight = dateFilter.map(x => (x(4).toString, 1))
      var newSQL_Weight = mapFilter_Weight.groupBy(mapFilter_Weight("_1")).agg(sum(mapFilter_Weight("_2"))).orderBy(desc("sum(_2)"))
      newSQL_Weight = newSQL_Weight.withColumnRenamed("_1", "Weighted Sentiments").withColumnRenamed("sum(_2)", "Count")
      newSQL_Weight.show()
      val finalResult_Weight = newSQL_Weight.limit(1).select("Weighted Sentiments").as[String].collect()(0).toString
      // return the result
      (finalResult_Main, finalResult_Avg, finalResult_Weight)
    } catch {
      case _: Throwable => {
        println("Warning: Could not process Twitter data for timestamps " + startDate + " to " + endDate)
        val defValue = ("STOP", "STOP", "STOP")
        defValue
      }
    }
  }

  def get_yahoo_finance_intraday(spark: SparkSession, startDate: String, endDate: String, ticker: String, interval: String = "1h", days: Int = 7): String = {
    try {
      // Get data from URL
      val uri = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker + "?range=" + days.toString() + "d&interval=" + interval
      println("URL used " + uri)
      val result = getRestContent(uri)

      // Read and import to dataframe
      //val spark = SparkSession.builder().appName("SparkSQL").master("local").getOrCreate()
      import spark.implicits._
      val df = spark.read.json(Seq(result).toDS)

      // Expand json object to find indicators
      val mainJSON = df.select("chart.*").select(explode(col("result")))
      val dataArray = mainJSON.select("col.*")
      val indicators = dataArray.select("indicators.*").select(explode(col("quote"))).select("col.*")

      // Convert the lists to int or doubles
      val volume: Array[Int] = indicators.select("volume").first.getList(0).toArray().map(x => x.toString.toInt)
      val close: Array[Double] = indicators.select("close").first.getList(0).toArray().map(x => x.toString.toDouble)
      // Convert list of timestamp to a dateformat
      val timestamps: Array[String] = dataArray.select("timestamp").first.getList(0).toArray().map(x => x.toString.trim()).map(x => new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date(((x).toLong * 1000L + 4 * 3600 * 1000))))

      // Zip to tuple
      val zipped = timestamps.zip(close).zip(volume).map {
        case ((a, b), c) => (a, b, c)
      }

      // Paralellize to new DF
      val sc = spark.sparkContext
      val newDF = sc.parallelize(zipped).toDF("timestamp", "close", "volume")

      val res = newDF.filter(newDF("timestamp") >= startDate && newDF("timestamp") <= endDate).select("close").collect
      val diff = res(res.length - 1).getDouble(0) - res(0).getDouble(0)

      println("Price at start was " + res(0).getDouble(0).toString)
      println("Price at end was " + res(res.length - 1).getDouble(0).toString)
      println("Difference is " + diff.toString)

      var retValue = "POSITIVE"
      if (diff < 0) {
        retValue = "NEGATIVE"
      }
      retValue
    }
    catch {
      case _: Throwable => {
        println("Warning: Could not process yahoo intraday data for timestamps " + startDate + " to " + endDate)
        val defValue = "STOP"
        defValue
      }
    }
  }


  // Get JSON data from rest safely
  def getRestContent(url: String): String = {
    val httpClient = new DefaultHttpClient()
    val httpResponse = httpClient.execute(new HttpGet(url))
    val entity = httpResponse.getEntity()
    var content = ""
    if (entity != null) {
      val inputStream = entity.getContent()
      content = fromInputStream(inputStream).getLines.mkString
      inputStream.close
    }
    httpClient.getConnectionManager().shutdown()
    return content
  }

}
