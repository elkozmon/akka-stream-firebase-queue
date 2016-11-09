name := "akka-stream-firebase-queue"

organization := "com.elkozmon"

version := "1.0"

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq(
  "2.12.0",
  "2.11.8"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.12",
  "com.typesafe.akka" %% "akka-stream" % "2.4.12",
  "com.google.firebase" % "firebase-server-sdk" % "3.0.1",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
)

val gitHubUrl = "https://github.com/elkozmon/akka-stream-firebase-queue"

homepage := Some(url(gitHubUrl))

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
  <url>${gitHubUrl}</url>
  <licenses>
    <license>
      <name>MIT License</name>
      <url>http://www.opensource.org/licenses/mit-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:elkozmon/akka-stream-firebase-queue.git</url>
    <connection>scm:git:git@github.com:elkozmon/akka-stream-firebase-queue.git</connection>
  </scm>
  <developers>
    <developer>
      <id>elkozmon</id>
      <name>Lubos Kozmon</name>
      <timezone>+1</timezone>
      <url>https://elkozmon.com</url>
    </developer>
  </developers>
