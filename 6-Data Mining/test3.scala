import org.apache.spark.SparkContext

import scala.io.Source._
import java.sql.Date
import java.text.SimpleDateFormat

import scala.io.Source
import java.io.File
import scala.collection.mutable.ListBuffer

val transactions = sc.textFile("data/data.txt").cache()
val singletonCount = transactions.flatMap(line => line.split(" ")).map(word => (word, 1)).reduceByKey((a, b) => a + b)
val keyInSet = transactions.map(x => x.split(" ").toSet)

val singles = singletonCount.collect
val tranSet = keyInSet.collect

/////////



import scala.io.Source._

import scala.io.Source
import java.io.File
import scala.collection.mutable.ListBuffer


def addValToList(mrgList : List[String]) =
{
    mrgList.groupBy(identity).mapValues(_.size)
}


def getAllPairs(amFilter : List[List[String]], k : Int) = {
    var l = ListBuffer[String]()
    for( i <-0 until amFilter.size){
        for(j <- 0 until amFilter(i).size){
            l += amFilter(i)(j)
        }
    }
    l = l.distinct
    var superset = ListBuffer[List[String]]()
    for( i <- 0 to l.size - k){
        for( j <- i + 1 to l.size - (k - 1)){
            var t = ListBuffer(l(i))
            for ( n <- 0 until k - 1){
                t += l(j + n)
            }
            superset += t.toList
        }
    }
    superset
}

def ruleGen(aList : (List[String], String), appear : Int, t: Double, mapTT : Map[List[String],Int]) 
: (String, Double) = 
{
    val key = aList._1
    val aVal = aList._2
    val passHold = appear.toDouble / mapTT.getOrElse(key,100000).toDouble
    var aRule = ""
    if(passHold >= t)
    {
        // TODO: iterate list and gen alist(0),aList(1)...
        var tmpString = ""
        for(i <- 0 until key.size)
        {
            tmpString += key(i) + ","
        }
        tmpString = tmpString.substring(0,tmpString.length-1)
        aRule = tmpString + " => " + aVal
        
    }
    ((aRule,passHold))
    
}

def getAllPairs2(stringList : List[String]) : List[(List[String],String)] = {
    
    val tmpList = new ListBuffer[(List[String],String)]()
    for(i <- 0 until stringList.size)
    {
        val keyList = new ListBuffer[String]()
        val currKey = stringList(i)
        for(j <- 0 until stringList.size)
        {
            if(i != j)
            {
                keyList += stringList(j)
            }
        }
        tmpList += ((keyList.toList, currKey))
    }
    tmpList.toList
}
        // NYTT        // NYTT        // NYTT        // NYTT
def countList(aList : scala.collection.immutable.Set[String], checkList : Array[scala.collection.immutable.Set[String]] ) = {
    
    val g = aList
    println("checking" + g)
    val appearCount = checkList.map{case x => g.subsetOf(x)}.filter(_ == true).size
    (aList,appearCount)
}
        // NYTT        // NYTT        // NYTT        // NYTT

//def Apriori(singles : String, k: Int, aList : List[List[String]], s: Int) = {         // NYTT
def Apriori(singles : Array[(String,Int)], trans: Array[scala.collection.immutable.Set[String]], k: Int,  s: Int) = {         // NYTT
    // 1 scan
    val bucket = trans.size
    println("Transactions found: " + bucket)
    
    val amFilter = singles.filter(_._2 >= s)
    println("Items above s: " + s + " are " + amFilter.size)
    
    val currPair = amFilter.map{case x => List(x._1)}.toList
    println("CURR")
    //println(currPair)
    val currPairrm = getAllPairs(currPair,2)
    val currPairr = currPairrm.map{case x => x.toSet}
    println("pairs are" + currPairr.size)
    var mapTestMap = currPairr.map(x => countList(x,trans)).toMap
	println("done mapptestmap")
    val c2urrPair = mapTestMap.filter(x => x._2 >= s).toList
    println("K2"  + " is: " + c2urrPair)
    //currPair = currPairr
    if(false)//for(i <- 2 to k)
    {
        //val i = 2
        //var currPairr = getAllPairs(currPair,i)
        
        //var mapTestMap = currPairr.map{case x => countList(x,aList)}.toMap
        //val c2urrPair = mapTestMap.filter(x => x._2 >= s).toList
        //println("K" + i + " is: " + currPair)
        //currPair = currPairr
    }
}
              
              
val k = 3
//val singles = singletonCount.collect
//val tranSet = keyInSet.collect
// NYTT INPUT
println("enter")
val aFina = Apriori(singles,tranSet,k,1000)

