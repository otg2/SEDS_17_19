import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx.GraphLoader
object PageRank {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val conf = new SparkConf().setAppName("example").setMaster("local[1]")
    val sc = SparkContext.getOrCreate(conf)

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)


    // val graph = GraphLoader.edgeListFile(sc, "/Users/mohamedgabr/Downloads/ego-twitter/out.ego-twitter.csv")


    val graph = GraphLoader.edgeListFile(sc, "/Users/mohamedgabr/Downloads/arenas-email/out.arenas-email.csv")


    print("PAGERANK")
    val before:Double = System.nanoTime()


    val cc = graph.staticPageRank(10, 0.0001).vertices.collect()


    println(cc.deep)

    val after:Double = System.nanoTime()

    val difference = after - before

    println("Time Elapsed: ")
    println(difference)

    println()

  }

}
