Akka Stream Firebase Queue 
==========================

Akka stream connector using Firebase as a message queue.

There is API for Scala and Java provided in packages `com.elkozmon.akka.firebase.scaladsl` and `com.elkozmon.akka.firebase.javadsl`. Factory methods reside in `Consumer` and `Producer` objects in those packages.

Consumer
--------

Consumer consumes child nodes in lexicographical order of their keys so they can be directly used with Firebase collections with auto-generated ids. 

Child nodes are **consumed and removed from the database in transaction**, so multiple consumers can safely consume the same node in parallel with **exactly-once** guarantee (but in such case its better idea to partition messages under separate nodes to avoid contention).

Producer
--------

Producer stores documents under key as given in `Document` object instance unless null in which case it uses auto-generated Firebase id.

Usage example
-------

```scala
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep}
import com.elkozmon.akka.firebase.Document
import com.elkozmon.akka.firebase.scaladsl._
import com.google.firebase.database.FirebaseDatabase

object Test {

  implicit val mat: Materializer = ???

  val transform: Flow[Document, Document, NotUsed] = ???

  val consumer = Consumer.bufferedSource(
    sourceNode = FirebaseDatabase.getInstance().getReference("my-source"),
    bufferSize = 32
  )

  val producer = Producer.asyncSink(
    targetNode = FirebaseDatabase.getInstance().getReference("my-sink")
  )

  val (consumerControl, futureDone) = consumer
    .via(transform)
    .toMat(producer)(Keep.both)
    .run()
}
```
