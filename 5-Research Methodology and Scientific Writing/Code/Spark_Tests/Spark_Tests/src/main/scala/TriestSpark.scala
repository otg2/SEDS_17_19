//import org.apache.spark.{Accumulator, SparkConf}
//import org.apache.spark.streaming.{Seconds, StreamingContext}
//
//import scala.collection.mutable.{ArrayBuffer, Map}
//
//
//
//
//
//
//@volatile private var sample: Accumulator[ArrayBuffer[Edge]] = null
//@volatile private var t: Accumulator[Int] = null
//@volatile private var tau_u: Accumulator[Map[Long, Float]] = null
//@volatile private var tau: Accumulator[Float] = null
//lazy val addition = (a: Float, b: Float) => a+b
//lazy val subtraction = (a: Float, b: Float) => a-b
//lazy val r = scala.util.Random
//
//
//
//override def map(input: (Int,Long,Long)): String = {
//
//  val input2 = Edge(input._2, input._3)
//
//  if (t.value() == null)
//  {
//    print("I am updating 1")
//    t.update(0)
//  }
//  if (tau.value() == null)
//  {
//    print("I am updating 2")
//    tau.update(0.0f)
//  }
//
//  if (tau_u.value() == null)
//  {
//    print("I am updating 3")
//    tau_u.update(mutable.Map())
//  }
//  if (sample.value() == null)
//  {
//    print("I am updating 4")
//    sample.update(new ArrayBuffer[Edge](0))
//  }
//
//  // access the state values
//  val tmpT = t.value+1
//  var tmpSample : ArrayBuffer[Edge] = sample.value
//
//
//
//  // Triest-Imp: update counters in all cases
//
//  updateCounters(input._2, input._3, addition,  tmpSample, math.max(1, (tmpT-1).toFloat*(tmpT-2).toFloat/(M*(M-1).toFloat)))
//
//
//  // Sample edge decides if the new edge will be added to the reservoir or not and does it
//  // Call sample edge
//  if (sampleEdge2(input2, tmpT, tmpSample))
//  {
//
//    tmpSample +=input2
//
//
//
//  }
//
//  // update the state
//
//  t.update(tmpT)
//  sample.update(tmpSample)
//
//  // val epsilon =  math.max(1, (tmpT * (tmpT - 1) * (tmpT - 2)).toDouble/(M* (M-1) * (M-2)).toDouble)
//  // To print out the approximation of # of triangles
//
//  val epsilon =  math.max(1, (tmpT * (tmpT - 1) * (tmpT - 2)).toDouble/(M* (M-1) * (M-2)).toDouble)
//  val firsttau  = "The global count is " + tau.value.toString + "\n "
//
//  // Number of local triangles
//
//  var localtau:String = ""
//  for (u <- tau_u.value.keys)
//  {
//
//    localtau = localtau + u.toString + " has a value: " + (tau_u.value()(u) * epsilon).toString  + "\n"
//  }
//
//  // The number of local triangles is not output here because it prints out too much info
//
//  firsttau
//
//
//}
//
//def sampleEdge2(e: Edge, t: Long, sample: ArrayBuffer[Edge]) : Boolean = {
//  if (t <= M){
//    // While the sample is not full, add edges
//    return true
//  }
//  else if(flipBiasedCoin(t.toInt))
//  {
//    // Select a random edge in the sample to be replaced
//    val index = r.nextInt(M - 1)
//    val Edge(uPrime, vPrime) = sample(index)
//    sample -= sample(index)
//    //updateCounters(uPrime, vPrime, subtraction,  sample, 1)
//    return true
//  }
//  return false
//}
//
//
//
//
//// True with M/t chance
//def flipBiasedCoin(step: Int) : Boolean = {
//  r.nextFloat() <  M.toFloat/step.toFloat
//}
//
//def updateCounters(u: Long, v: Long, opt: (Float, Float) => Float, sample: ArrayBuffer[Edge], value:Float) : Unit = {
//  // Compute neighborhood
//  val neighbUV = neighborhood(u, sample.toList).intersect(neighborhood(v, sample.toList))
//
//  for (c <- neighbUV) {
//
//    // Update the tau values
//
//    var tmpTau_u = tau_u.value()
//    val tmpTau = opt(tau.value(), value)
//    // Update the tau values
//    addOrUpdate(tmpTau_u, c, value, opt)
//    addOrUpdate(tmpTau_u, u, value, opt)
//    addOrUpdate(tmpTau_u, v, value, opt)
//
//    tau_u.update(tmpTau_u)
//    tau.acc(tmpTau)
//
//    print(c)
//
//  }
//
//}
//
//def addOrUpdate(tau_u: mutable.Map[Long, Float], key: Long, value: Float, opt: (Float, Float) => Float): Unit = {
//  tau_u.get(key) match {
//    case Some(v) => tau_u(key)=opt(v,value)
//    case None => tau_u(key)=value
//  }
//}
//
//// Returns the neighborhood of s in the graph u
//def neighborhood(s:Long, u:List[Edge]) : Set[Long] = {
//
//  u match {
//    case Edge(`s`,l)::xs => neighborhood(s, xs) + l
//    case Edge(z,`s`)::xs => neighborhood(s, xs) + z
//    case _::xs => neighborhood(s, xs)
//    case Nil => Set()
//  }
//
//}
//// To initialize the State Variables
//
//override def open(parameters: Configuration): Unit = {
//
//  import org.apache.flink.streaming.api.scala._
//  t = getRuntimeContext.getState(
//    new ValueStateDescriptor[Int]("Time Stamp/ Step", createTypeInformation[Int]))
//
//  tau = getRuntimeContext.getState(
//    new ValueStateDescriptor[Float]("Global Triangle Count", createTypeInformation[Float]))
//
//  tau_u = getRuntimeContext.getState(
//    new ValueStateDescriptor[mutable.Map[Long, Float]]("Local Triangle Count", createTypeInformation[mutable.Map[Long, Float]]))
//
//
//  sample = getRuntimeContext.getState(
//    new ValueStateDescriptor[ArrayBuffer[Edge]]("Reservoir", createTypeInformation[ArrayBuffer[Edge]]))
//
//
//}
//}
//
//
//
//
//
//
//
//
//case class Edge(from: Long, to: Long)
//
//object CountTriangles extends App {
//
//  val conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
//  val ssc = new StreamingContext(conf, Seconds(1))
//
//  val lines = ssc.socketTextStream("localhost", 9999)
//
//  val accum = ssc.
//
//  val stream = ssc.fileStream[Int, Int, Int]("/Users/mohamedgabr/Desktop/out.ucidata-zachary.csv")
//
//
//
//
//  ssc.start()             // Start the computation
//  ssc.awaitTermination()  // Wait for the computation to terminate
//
//
//}