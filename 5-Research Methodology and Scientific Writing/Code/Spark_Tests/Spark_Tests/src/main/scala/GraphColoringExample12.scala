import org.apache.log4j.{Level, Logger}
import org.apache.spark
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.{SparkConf, SparkContext, sql}
import org.apache.spark.graphx.{Graph, VertexRDD, Edge => GXEdge}
import org.apache.spark.sql.types._
import org.graphframes.GraphFrame
import org.apache.spark.sql.SparkSession
import spark._



object GraphColoringExample12 {

  import GraphColoring._

  def main(args: Array[String]): Unit = {




    val sc = org.apache.spark.sql.SparkSession.builder
      .master("local[2]")
      .appName("Example")
      .getOrCreate


    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    def df = sc.read.csv("/Users/mohamedgabr/Downloads/arenas-jazz/out.arenas-jazz.csv")


    val df2 = df.select("_c0")

    var df3 = df.select("_c1")

    val newnames = Seq("vertex_no")

    val df2Renamed = df2.toDF(newnames: _*)
    val df3Renamed = df3.toDF(newnames: _*)


    val df4  = df2Renamed.union(df3Renamed).distinct()

    val newname = Seq("id")

    val df4Renamed = df4.toDF(newname: _*)

    import sc.implicits._


    def list = (sc.sparkContext.parallelize(1 to 198)).toDF("color")

    val list2 = Seq(1 to 198)


    val df55 = df4Renamed.withColumn("color", list("color"))

    print(df55.printSchema())







    //
//    // Run BP for 5 iterations.
//    println("Original Graph Model :")
//    vertices.printSchema()
//
//    println("Edges: ")
//    edges.printSchema()
//
//    val g2 = colorGraphReductionFastest(g,4)
//    g2.vertices.show()
//    g2.edges.show()
//
//    val diffUdf = udf((i1:Int,i2:Int) => if (i1==i2) 1 else 0)
//    val count =  g2.edges.join(g2.vertices, g2.edges.col("src").equalTo(g2.vertices.col("id")),"inner")
//
//      .withColumn("srcColor", g2.vertices.col("finalColor"))
//      .select(col("srcColor"),col("dst"), col("src"))
//      .join(g2.vertices, g2.edges.col("dst").equalTo(g2.vertices.col("id")),"inner")
//      .withColumn("dstColor", g2.vertices.col("finalColor"))
//      .select(col("srcColor"),col("dst"), col("src"),col("dstColor"))
//      .withColumn("diffColor",diffUdf(col("srcColor"),col("dstColor")))
//      .select("diffColor").agg(sum(col("diffColor"))).collect()(0)(0).asInstanceOf[Long]
//
//    println("Count = ", count)
//
//    val msgs = List(1,2,4,5).map(i => Msg(1 << i)).fold(Msg(0))(chooseColorMsg)
//    println("original:", msgs)
//    println("result" ,getMinColor(msgs))
//
//    // Done with Coloring
//    sc.stop()
  }

}
