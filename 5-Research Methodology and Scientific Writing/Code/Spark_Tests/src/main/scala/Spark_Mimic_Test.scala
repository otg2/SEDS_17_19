import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx.lib.TriangleCount
import org.apache.spark.graphx.{Graph, VertexId}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
//import org.joda.time.{DateTime, DateTimeZone}
//
//import scala.util.MurmurHash
//import scala.util.hashing.MurmurHash3
//
//
//
//object Spark_Mimic_Test extends App{
//
//  val sparkConf = new SparkConf().setAppName("Spark_Mimic_Test").setMaster("local[*]")
//  val sc = new SparkContext(sparkConf)
//  val file = sc.textFile("out.opsahl-powergrid.csv")
//  val edgesRDD: RDD[(VertexId, VertexId)] = file.map(line => line.split(" ")).map(line =>
//    ((line(0).toInt, (line(1).toInt))))
//  // create a graph
//  val graph = Graph.fromEdgeTuples(edgesRDD, 1)
//
//  println(DateTime.now(DateTimeZone.UTC).getMillis() + "\n")
//
//  // you can see your graph
//  println(graph.numVertices)
//  println(graph.numEdges)
//  val triCounts = graph.triangleCount().vertices
//  println("Triangle Counts: " + triCounts.collect().mkString("\n"))
//  // val triangles = TriangleCount.run(graph)
//  println(DateTime.now(DateTimeZone.UTC).getMillis() + "\n")
//
// // triangles.triplets.collect.foreach(println)
//
//
//
//
//}
