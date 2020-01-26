import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.sql.functions._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx.{Graph, GraphLoader, VertexRDD, Edge => GXEdge}
import org.apache.spark.sql.types.{IntegerType, LongType}
import org.graphframes.GraphFrame

import scala.collection.mutable.ArrayBuffer
import scala.io.Source



object ConnectedComps {


  def main(args: Array[String]): Unit = {

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)


    val conf = new SparkConf().setAppName("example").setMaster("local[*]")
    val sc = SparkContext.getOrCreate(conf)

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    println("Connected Components")


    val graph = GraphLoader.edgeListFile(sc, "/Users/mohamedgabr/Downloads/arenas-jazz/out.arenas-jazz.csv")


    val before:Double = System.nanoTime()


    val cc = graph.connectedComponents(1000).vertices.collect()


    println(cc.deep)
    val after:Double = System.nanoTime()

    val difference = after - before

    println("CONNECTED COMPS")

    println("Time Elapsed: ")
    println(difference)

    println()



  }


  }