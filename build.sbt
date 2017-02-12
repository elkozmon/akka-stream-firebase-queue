name := "akka-stream-firebase-queue"

organization := "com.elkozmon"

version := "2.1"

scalaVersion := crossScalaVersions.value.head

crossScalaVersions := Seq(
  "2.12.1",
  "2.11.8"
)

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard",
  "-Ywarn-dead-code"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.16",
  "com.typesafe.akka" %% "akka-stream" % "2.4.16",
  "com.google.firebase" % "firebase-server-sdk" % "3.0.3",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
)

homepage := Some(url("https://github.com/elkozmon/akka-stream-firebase-queue"))

licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
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
