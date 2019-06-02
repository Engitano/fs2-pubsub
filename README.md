# fs2 PubSub

##### A GCP PubSub client based on [fs2](https://fs2.io/guide.html)


Basic Usage:

```scala
      import com.engitano.fs2pubsub.syntax._
      val topicName = "test-topic"
      val testSubscription = "test-sub"
      implicit val intSerializer = Serializer.from[IO, Int](i => BigInt(i).toByteArray)
      implicit val intDeserializer = Deserializer.from[IO, Int](b => BigInt(b).toInt)
  
      val config = GrpcPubsubConfig.local(DefaultGcpProject, DefaultPubsubPort)
  
      val program = Publisher[IO](config).use { implicit pub =>
        Subscriber[IO](config).use { implicit sub =>
  
          def businessLogic(i: WithAckId[Int]) = IO.delay(println(i))
  
          val publishStream = Stream.emits[IO, Int](1 to 1000).toPubSub(topicName)
          val subscribeStream = sub.consume[Int](testSubscription) { intStream =>
            intStream.evalTap(businessLogic)
          }
  
          (publishStream >> subscribeStream).compile.drain.attempt
        }
      }
  
      program.unsafeRunSync()
```