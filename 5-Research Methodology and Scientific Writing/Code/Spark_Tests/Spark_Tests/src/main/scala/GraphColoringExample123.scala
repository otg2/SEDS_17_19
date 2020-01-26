import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.sql.functions._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx.{Graph, VertexRDD, Edge => GXEdge}
import org.apache.spark.sql.types.{IntegerType, LongType}
import org.graphframes.GraphFrame

import scala.collection.mutable.ArrayBuffer
import scala.io.Source



object GraphColoringExample123 {

  import GraphColoring._

  def main(args: Array[String]): Unit = {


    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)


    val conf = new SparkConf().setAppName("example").setMaster("local[*]")
    val sc = SparkContext.getOrCreate(conf)
    val sqlContext = SQLContext.getOrCreate(sc)

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)



    // def src = Source.fromFile("/Users/mohamedgabr/Downloads/flixster/out.flixster.csv")

    val src = Source.fromFile("/Users/mohamedgabr/Downloads/arenas-jazz/out.arenas-jazz_2.csv")

    ///val src = Source.fromFile("/Users/mohamedgabr/Downloads/arenas-pgp/out.arenas-pgp.csv")



    var rowss = ArrayBuffer[(String, String, Double)]()
    var rows5 = ArrayBuffer[(Int, String, Double, Double, Int, Int, Int)]()
    var rows3 = ArrayBuffer[String]()
    var count2 = 0


    src.getLines.foreach { line =>
      var ll = line.split(",").map(_.trim())
      def row = (ll{0}, ll{1}, 0.5)
      rowss += row
      rows3 += ll{0}
      rows3 += ll{1}
    }
    var rows2 = rowss.toList
    rows3 = rows3.distinct


    for (a <- 0 to rows3.length-1)
      {
        val fe = (rows3{a}.toInt, "A", 0.0, 0.0, 0, 0, a)
        rows5 += fe
      }




    // Create graphical model g of size 3 x 3.
    val nodes = sqlContext
      .createDataFrame(rows5.toList)
      .toDF("index", "mark", "longitude", "latitude", "outDegree", "inDegree","colorInt")

    // val createPointClassUdf = udf((long: Double, lat: Double) => Point(long, lat))
    val toLongUdf = udf((i: Int) => i.toLong)
    // val sumUdf = udf((i1:Int,i2:Int) => i1+i2)

    /*
    val vertices = nodes.withColumn("coordinates", createPointClassUdf(col("longitude"), col("latitude")))
      .withColumn("id", toLongUdf(col("index"))).drop(col("index"))
        .withColumn("color", toLongUdf(col("colorInt")))
        .withColumn("degree", sumUdf(col("inDegree"), col("outDegree")))
          .sort(col("color").desc)
      */

    val id = (1 to nodes.count.toInt).toSeq.map(i => i.toLong)
    val rows = nodes.rdd.collect.zip(id).map { case (r, index) => Row.fromSeq(r.toSeq :+ index) }
    val struct = nodes.schema.add("color", LongType, false)

    val vertices = sqlContext.createDataFrame(sc.parallelize(rows), struct)
      .withColumn("id", toLongUdf(col("index"))).drop(col("index"))

    val connections =
      sqlContext.createDataFrame(rows2)
        .toDF("srcIndex", "dstIndex", "dist")

    val edges = connections.withColumn("src",toLongUdf(col("srcIndex"))).withColumn("dst", toLongUdf(col("dstIndex")))
      .select(col("src"),col("dst"),col("dist"))
    val g = GraphFrame(vertices, edges).cache()

    // Run BP for 5 iterations.
    println("Original Graph Model :")
    vertices.printSchema()

    println("Edges: ")
    edges.printSchema()

    val g2 = colorGraphReductionFastest(g,4)
    g2.vertices.show()
    g2.edges.show()

    val diffUdf = udf((i1:Int,i2:Int) => if (i1==i2) 1 else 0)
    val count =  g2.edges.join(g2.vertices, g2.edges.col("src").equalTo(g2.vertices.col("id")),"inner")

      .withColumn("srcColor", g2.vertices.col("finalColor"))
      .select(col("srcColor"),col("dst"), col("src"))
      .join(g2.vertices, g2.edges.col("dst").equalTo(g2.vertices.col("id")),"inner")
      .withColumn("dstColor", g2.vertices.col("finalColor"))
      .select(col("srcColor"),col("dst"), col("src"),col("dstColor"))
      .withColumn("diffColor",diffUdf(col("srcColor"),col("dstColor")))
      .select("diffColor").agg(sum(col("diffColor"))).collect()(0)(0).asInstanceOf[Long]

    println("Count = ", count)

    val msgs = List(1,2,4,5).map(i => Msg(1 << i)).fold(Msg(0))(chooseColorMsg)
    println("original:", msgs)
    println("result" ,getMinColor(msgs))

    // Done with Coloring
    sc.stop()
  }

}
