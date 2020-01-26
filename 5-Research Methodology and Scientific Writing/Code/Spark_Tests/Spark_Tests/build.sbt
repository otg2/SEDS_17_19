name := "Spark_Tests"

version := "0.1"

scalaVersion := "2.11.8"

val sparkVersion = "2.2.0"

val provided = "compile"

resolvers ++= Seq(
  "bintray-artifacts" at "https://dl.bintray.com/spark-packages/maven/"
)


libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.11" % sparkVersion,
  "org.apache.spark" % "spark-sql_2.11" % sparkVersion,
  "org.apache.spark" % "spark-graphx_2.11" % sparkVersion,
  "graphframes" % "graphframes" % "0.5.0-spark2.1-s_2.11",
  "org.apache.spark" %% "spark-streaming" % sparkVersion


)




