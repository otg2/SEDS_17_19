import org.apache.flink.graph.{Edge, Vertex}
import org.apache.flink.graph.scala.NeighborsFunctionWithVertexValue
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object Hello {

    def main(args: Array[String]) {
      val env = StreamExecutionEnvironment.getExecutionEnvironment

      // create a stream using socket



      // user-defined function to select the neighbors which have edges with weight > 0.5
      final class SelectLargeWeightNeighbors extends NeighborsFunctionWithVertexValue[Long, Long, Double,
        (Vertex[Long, Long], Vertex[Long, Long])] {

        override def iterateNeighbors(vertex: Vertex[Long, Long],
                                      neighbors: Iterable[(Edge[Long, Double], Vertex[Long, Long])],
                                      out: Collector[(Vertex[Long, Long], Vertex[Long, Long])]) = {

          for (neighbor <- neighbors) {
            if (neighbor._1.getValue() > 0.5) {
              out.collect(vertex, neighbor._2)
            }
          }
        }
      }

    }
}
