import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.graph.library.GSAConnectedComponents
import org.apache.flink.api.scala._
import org.apache.flink.graph.scala._
import org.apache.flink.graph.library.linkanalysis.PageRank

object PageRankFlink {

  def main(args: Array[String]): Unit = {
    print("Hello World")
    val env = ExecutionEnvironment.getExecutionEnvironment


    //var edges2 = env.readCsvFile[(Integer, Integer)]("/Users/mohamedgabr/Downloads/ego-twitter/out.ego-twitter_2.csv")

    var edges2 = env.readCsvFile[(Integer, Integer)]("/Users/mohamedgabr/Downloads/arenas-email/out.arenas-email_2.csv")

    val edges = edges2.map(x => (x._1, x._2, 1: Integer))


    var graph = Graph.fromTupleDataSet(edges, env)


     var graph2 = graph.mapVertices(v => 1: Integer)




    val before :Double= System.nanoTime()

    println("BEFORE")
    println(before)

    //val graphupdated3 = graph.run(new TriangleEnumerator[Integer, NullValue, NullValue])


    println("PAGERANK")
    val graphupdated4 = graph2.run(new PageRank[Integer, Integer, Integer](0.0001, 1000))


    //println("Connected Comps")
    //val graphupdated4 = graph2.run(new GSAConnectedComponents[Integer, Integer, Integer]( 10))


    println("Results")
    println(graphupdated4.collect())

    val after:Double = System.nanoTime()
    println("AFTER")
    println(after)
    val diff = after - before
    println("Time Difference: ")
    println(diff)

    //println(graphupdated4.collect())

  }
}