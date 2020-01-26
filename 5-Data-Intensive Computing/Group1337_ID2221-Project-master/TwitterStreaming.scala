package twitterstreaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import twitter4j._
import org.apache.spark.streaming.twitter._
import org.apache.log4j.{Level, Logger}
import SentimentAnalysisUtils._

import org.apache.spark.sql.types._
import org.apache.spark.sql._

object TwitterStreaming {
  def main(args: Array[String]) {
    //if (args.length < 4) {
    //System.err.println("Usage: TwitterStreaming <ConsumerKey><ConsumerSecret><accessToken><accessTokenSecret>" + "[<filters>]")
    //System.exit(1)
    //}

    // Set logging level if log4j not configured (override by adding log4j.properties to classpath)
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    //val conf = new SparkConf().setAppName("TwitterStreaming").setMaster("local[*]")//.getOrCreate()
    val spark = SparkSession.builder.master("local[*]").appName("TwitterStreaming").getOrCreate()

    //val ssc = new StreamingContext(spark.sparkContext, Seconds(5))
    System.setProperty("twitter4j.oauth.consumerKey", "JYeCb8RfdpZxi4NL68XvlBJFB")
    System.setProperty("twitter4j.oauth.consumerSecret", "kO17s5pldOWeChj57kaOBepBFRjKpcS79bapISDXTcd7kJl0am")
    System.setProperty("twitter4j.oauth.accessToken", "1128218748-pSfig5cyOIilLTGBNRVyca9PEIoCfOG1eurm02N")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "mtHWtozu3qdmOhFaoeH3tXxKgspBAYJdDMh9xXKrvWP1l")

    // Set the saving schema for DF, empty array, get the context and create a counter
    val schema = StructType(
      StructField("Date", StringType, false) ::
        StructField("Tweet", StringType, false) ::
        StructField("MainSentiment", StringType, false) ::
        StructField("AvgSentiment", StringType, false) ::
        StructField("WeightedSentiment", StringType, false) :: Nil
    )
    var arraylist: Array[(String, String, String, String, String)] = Array();
    val sc = spark.sparkContext

    // -----
    val listener = new StatusListener() {
      def onStatus(status: Status): Unit = {
        if (!status.isRetweet) {
          val content = parse_text(status.getText)
          if (content.toLowerCase.contains("apple")
            || content.toLowerCase.contains("iphone")
            || content.toLowerCase.contains("ipad")
            || content.toLowerCase.contains("ios")
            || content.toLowerCase.contains("iwatch")
            || content.toLowerCase.contains("macbook")
            || content.toLowerCase.contains("macos")
            || content.toLowerCase.contains("airpods")
            || content.toLowerCase.contains("tim cook")
            || content.toLowerCase.contains("steve jobs")
            || content.toLowerCase.contains("mojave")) {
            var sentiments: List[String] = detectSentiment(content)
            System.out.println(status.getCreatedAt.toInstant.toString + " / " + content + " / " + sentiments(0) + " / " + sentiments(1) + " / " + sentiments(2))
            arraylist = arraylist :+ (status.getCreatedAt.toInstant.toString, content, sentiments(0), sentiments(1), sentiments(2))

            if (arraylist.size > 10) {
              println("SAVING TWEETS TO PARQUET")
              // Create rdd from array and change rdd to df
              val rdd = sc.parallelize(arraylist).map(x => Row(x._1, x._2, x._3, x._4, x._5))
              val tweetDF = spark.createDataFrame(rdd, schema)
              // append DF to parquet fle
              tweetDF.write.mode("append").save("data")
              // Clear array
              arraylist = Array()
            }
          }
        }
      }

      def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {
        System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId)
      }

      def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {
        System.out.println("Got track limitation notice:" + numberOfLimitedStatuses)
      }

      def onScrubGeo(userId: Long, upToStatusId: Long): Unit = {
        System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId)
      }

      def onStallWarning(warning: StallWarning): Unit = {
        System.out.println("Got stall warning:" + warning)
      }

      def onException(ex: Exception): Unit = {
        ex.printStackTrace()
      }
    }
    val twitterStream = new TwitterStreamFactory().getInstance()
    twitterStream.addListener(listener)
    val tweetFilterQuery = new FilterQuery()
    tweetFilterQuery.track("Apple", "iPhone", "iPad", "iOS", "iWatch", "Macbook", "macOS", "AirPods", "Tim Cook", "Steve Jobs", "Mojave")
    tweetFilterQuery.language("en")
    twitterStream.filter(tweetFilterQuery)
    //twitterStream.saveAsTextFiles("tweets", "json")
    //tweets.print()

    //ssc.start()
    //ssc.awaitTermination()
  }

  def parse_text(text: String): String = {
    val urlReg = """https://t\.co/[a-zA-Z0-9]+""".r
    val replacedUrl = urlReg.replaceAllIn(text, "URL")
    val userReg = """@[a-zA-Z0-9_]+""".r
    val replacedUser = userReg.replaceAllIn(replacedUrl, "USER")
    val replacedReturn = replacedUser.replaceAll(System.lineSeparator(), " ")
    val notAbcReg = """[^ 'a-zA-Z0-9,.?!]""".r
    val newT = notAbcReg.replaceAllIn(replacedReturn, "")
    //System.out.println("Orig:" + text)
    //System.out.println("New:" + newT)
    newT
  }
}
