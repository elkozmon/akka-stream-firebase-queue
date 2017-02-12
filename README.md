Akka Stream Firebase Queue 
==========================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.elkozmon/akka-stream-firebase-queue_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.elkozmon/akka-stream-firebase-queue_2.12)

Akka stream connector using Firebase as a message queue.

There is API for Scala and Java provided in packages `com.elkozmon.akka.firebase.scaladsl` and `com.elkozmon.akka.firebase.javadsl`. Factory methods reside in `Consumer` and `Producer` objects in those packages.

See ScalaDoc for more information on Consumers and Producers.

Usage example
-------

```scala
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Keep}
import com.elkozmon.akka.firebase.Document
import com.elkozmon.akka.firebase.scaladsl._
import com.google.firebase.database.FirebaseDatabase

object Test {

  implicit val mat: Materializer = ???

  val transform: Flow[Document, Document, NotUsed] = ???
  
  val parallelism: Int = 512

  val consumer = Consumer.asyncSource(
    sourceNode = FirebaseDatabase.getInstance().getReference("my-source"),
    bufferSize = 256
  )

  val producer = Producer.asyncFlow(
    targetNode = FirebaseDatabase.getInstance().getReference("my-sink")
  )

  val (consumerControl, futureDone) = consumer
    .mapAsync(parallelism)(identity)
    .via(transform)
    .via(producer)
    .mapAsync(parallelism)(identity)
    .toMat(Sink.ignore)(Keep.both)
    .run()
}
```
