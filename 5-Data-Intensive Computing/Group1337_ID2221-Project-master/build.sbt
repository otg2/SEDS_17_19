name := "spark_twitter"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.11" % "2.3.1",
  "org.apache.spark" % "spark-tags_2.11" % "2.3.1",
  "org.apache.spark" % "spark-sql_2.11" % "2.3.1",
  "org.apache.spark" % "spark-hive_2.11" % "2.3.1",  
  "org.apache.spark" % "spark-mllib_2.11" % "2.3.1",    
  "org.apache.spark" % "spark-streaming_2.11" % "2.3.1",
  "org.twitter4j" % "twitter4j-core" % "4.0.7",
  "org.apache.bahir" %% "spark-streaming-twitter" % "2.2.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.1" classifier "models"
)

