package twitterstreaming

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import twitter4j._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.api.java.{JavaReceiverInputDStream, JavaStreamingContext}
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.receiver.Receiver
import twitter4j.auth.{Authorization, OAuthAuthorization}
import twitter4j.conf.ConfigurationBuilder
import scala.collection.mutable._
import SentimentAnalysisUtils._
import java.time._
import java.time.format._

object SparkTwitterStreaming {
  def main(args: Array[String]) {
    //if (args.length < 4) {
    //System.err.println("Usage: TwitterStreaming <ConsumerKey><ConsumerSecret><accessToken><accessTokenSecret>" + "[<filters>]")
    //System.exit(1)
    //}

    // Set logging level if log4j not configured (override by adding log4j.properties to classpath)
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    //val spark = SparkSession.builder.master("local[*]").appName("TwitterStreaming").getOrCreate()
    val conf = new SparkConf().setAppName("TwitterStreaming").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, Seconds(5))
    val sqc = new SQLContext(sc)

    System.setProperty("twitter4j.oauth.consumerKey", "JYeCb8RfdpZxi4NL68XvlBJFB")
    System.setProperty("twitter4j.oauth.consumerSecret", "kO17s5pldOWeChj57kaOBepBFRjKpcS79bapISDXTcd7kJl0am")
    System.setProperty("twitter4j.oauth.accessToken", "1128218748-pSfig5cyOIilLTGBNRVyca9PEIoCfOG1eurm02N")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "mtHWtozu3qdmOhFaoeH3tXxKgspBAYJdDMh9xXKrvWP1l")

    val schema = StructType(
      StructField("Date", StringType, false) ::
        StructField("Tweet", StringType, false) ::
        StructField("MainSentiment", StringType, false) ::
        StructField("AvgSentiment", StringType, false) ::
        StructField("WeightedSentiment", StringType, false) :: Nil
    )

    val tweetFilterQuery = new FilterQuery()
    tweetFilterQuery.track("Apple", "iPhone", "iPad", "iOS", "iWatch", "Macbook", "macOS", "AirPods", "Tim Cook", "Steve Jobs", "Mojave")
    tweetFilterQuery.language("en")
    //val stream = createStream(ssc, tweetFilterQuery)
    val tweets = ssc.receiverStream(new TwitterReceiver(tweetFilterQuery))
    val filteredTweets = tweets.filter(!_.isRetweet).
      map(status => (status.getCreatedAt.toInstant.toString, parse_text(status.getText))).
      filter(tuple => tuple._2.toLowerCase.contains("apple")
        || tuple._2.toLowerCase.contains("iphone")
        || tuple._2.toLowerCase.contains("ipad")
        || tuple._2.toLowerCase.contains("ios")
        || tuple._2.toLowerCase.contains("iwatch")
        || tuple._2.toLowerCase.contains("macbook")
        || tuple._2.toLowerCase.contains("macos")
        || tuple._2.toLowerCase.contains("airpods")
        || tuple._2.toLowerCase.contains("tim cook")
        || tuple._2.toLowerCase.contains("steve jobs")
        || tuple._2.toLowerCase.contains("mojave")).
      map(tuple => (tuple._1, tuple._2, detectSentiment(tuple._2))).
      map(tuple3 => Row(tuple3._1, tuple3._2, tuple3._3(0), tuple3._3(1), tuple3._3(2)))

    filteredTweets.print()
    filteredTweets.foreachRDD(rdd => sqc.createDataFrame(rdd, schema).write.mode("append").save("data"))

    val sentiment = filteredTweets.flatMap(x => List(((x(2), "M"), 1), ((x(3), "A"), 1), ((x(4), "W"), 1)))
    val sentimentWindows = sentiment.reduceByKeyAndWindow((a: Int, b: Int) => (a + b), Seconds(60 * 60 * 3), Seconds(60 * 60 * 1))

    val mainSentiment = sentimentWindows.filter(_._1._2 == "M").map { case ((result, "M"), count) => (count, result) }.transform(_.sortByKey(false))

    val averageSentiment = sentimentWindows.filter(_._1._2 == "A").map { case ((result, "A"), count) => (count, result) }.transform(_.sortByKey(false))

    val weightedSentiment = sentimentWindows.filter(_._1._2 == "W").map { case ((result, "W"), count) => (count, result) }.transform(_.sortByKey(false))

    mainSentiment.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\nCurrent time " + LocalDateTime.now.format(DateTimeFormatter.ofPattern("YYYYMMdd_HHmmss")) + ", main sentiment result in last 3 hours:")
      topList.foreach { case (count, result) => println("%s (%s tweets)".format(result, count)) }
      rdd.take(1)(0) match {
        case (count, "NEGATIVE") => println("The prediction for next hour according to main sentiment results is going down")
        case (count, "POSITIVE") => println("The prediction for next hour according to main sentiment results is going up")
        case (count, "VERY_NEGATIVE") => println("The prediction for next hour according to main sentiment results is going down")
        case (count, "VERY_POSITIVE") => println("The prediction for next hour according to main sentiment results is going up")
        case (count, "NEUTRAL") => println("The prediction for next hour according to main sentiment results is being stable")
        case (count, "NOT_UNDERSTOOD") => println("The prediction for next hour according to main sentiment results is not valid")
      }
    })

    averageSentiment.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\nCurrent time " + LocalDateTime.now.format(DateTimeFormatter.ofPattern("YYYYMMdd_HHmmss")) + ", average sentiment result in last 3 hours:")
      topList.foreach { case (count, result) => println("%s (%s tweets)".format(result, count)) }
      rdd.take(1)(0) match {
        case (count, "NEGATIVE") => println("The prediction for next hour according to average sentiment results is going down")
        case (count, "POSITIVE") => println("The prediction for next hour according to average sentiment results is going up")
        case (count, "VERY_NEGATIVE") => println("The prediction for next hour according to average sentiment results is going down")
        case (count, "VERY_POSITIVE") => println("The prediction for next hour according to average sentiment results is going up")
        case (count, "NEUTRAL") => println("The prediction for next hour according to average sentiment results is being stable")
        case (count, "NOT_UNDERSTOOD") => println("The prediction for next hour according to average sentiment results is not valid")
      }
    })

    weightedSentiment.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\nCurrent time " + LocalDateTime.now.format(DateTimeFormatter.ofPattern("YYYYMMdd_HHmmss")) + ", weighted sentiment result in last 3 hours:")
      topList.foreach { case (count, result) => println("%s (%s tweets)".format(result, count)) }
      rdd.take(1)(0) match {
        case (count, "NEGATIVE") => println("The prediction for next hour according to weighted sentiment results is going down")
        case (count, "POSITIVE") => println("The prediction for next hour according to weighted sentiment results is going up")
        case (count, "VERY_NEGATIVE") => println("The prediction for next hour according to weighted sentiment results is going down")
        case (count, "VERY_POSITIVE") => println("The prediction for next hour according to weighted sentiment results is going up")
        case (count, "NEUTRAL") => println("The prediction for next hour according to weighted sentiment results is being stable")
        case (count, "NOT_UNDERSTOOD") => println("The prediction for next hour according to weighted sentiment results is not valid")
      }
    })


    ssc.start()
    ssc.awaitTermination()
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

  class TwitterReceiver(query: FilterQuery) extends Receiver[Status](StorageLevel.MEMORY_AND_DISK_SER) {

    @volatile private var stopped = false
    @volatile private var twitterStream: TwitterStream = _

    def onStart() {
      new Thread("Twitter Receiver") {
        override def run() {
          receive()
        }
      }.start()
    }

    private def receive() {

      try {
        val newTwitterStream = new TwitterStreamFactory().getInstance()
        //var arraylist: ArrayBuffer[Status] = ArrayBuffer();
        newTwitterStream.addListener(new StatusListener {
          def onStatus(status: Status): Unit = {
            //arraylist = arraylist :+status
            //println("received a tweet")
            store(status)
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
            if (!stopped) {
              restart("Error receiving tweets", ex)
            }
          }
        })

        newTwitterStream.filter(query)
        setTwitterStream(newTwitterStream)
        stopped = false
        println("Twitter receiver started")

      } catch {
        case e: Exception => restart("Error restarting Twitter stream", e)
      }
    }

    private def setTwitterStream(newTwitterStream: TwitterStream) = synchronized {
      if (twitterStream != null) {
        twitterStream.shutdown()
      }
      twitterStream = newTwitterStream
    }

    def onStop() {
      println("Twitter receiver stopped")
      setTwitterStream(null)
      stopped = true
    }
  }

}
