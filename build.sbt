name := "earthdawn-wiki"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "commons-io" % "commons-io" % "2.4",
  "org.apache.commons" % "commons-lang3" % "3.1",
  "org.apache.lucene" % "lucene-core" % "4.1.0",
  "org.apache.lucene" % "lucene-queryparser" % "4.1.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.1.0"
  //"pdf" % "pdf_2.10" % "0.4.1"
)     

play.Project.playJavaSettings
