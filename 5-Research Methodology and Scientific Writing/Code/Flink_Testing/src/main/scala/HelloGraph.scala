import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.scala._
import org.apache.flink.graph.scala.Graph
import org.apache.flink.graph.scala.Graph.fromDataSet
import org.apache.flink.graph.scala.utils.Tuple3ToEdgeMap
import org.apache.flink.graph.Edge
import org.apache.flink.graph.scala.Graph
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.JavaConversions._

object NetworkGraph {

  def main(args: Array[String]): Unit = {

    var srcVertexId: Long = 1L

    val env = ExecutionEnvironment.getExecutionEnvironment

    val edges: DataSet[Edge[Long, Double]] = env.readCsvFile[(Long, Long, Double)](
      "out.opsahl-powergrid.csv",
      fieldDelimiter = ",",
      lineDelimiter = "\n"
    ).map(new Tuple3ToEdgeMap[Long, Double]())
    val graph = Graph.fromDataSet[Long, Double, Double](edges, new InitVertices(srcVertexId), env)

    print(DateTime.now(DateTimeZone.UTC).getMillis() + "\n")
    println("Number of edges: " + graph.numberOfEdges())
    println("Number of vertices: " + graph.numberOfVertices())
    println(DateTime.now(DateTimeZone.UTC).getMillis() + "\n")

    graph.getVertices
      .collect()
      .foreach(v => println("Vertice: " + v.getId + " - " + v.getValue))

    graph.getEdges
      .collect()
      .foreach(e => println("Edge(" + e.getSource + "," + e.getTarget + ") " + e.getValue))

  }

  private final class InitVertices(srcId: Long) extends MapFunction[Long, Double] {

    override def map(id: Long) = {
      if (id.equals(srcId)) {
        0.0
      } else {
        Double.PositiveInfinity
      }
    }
  }

}