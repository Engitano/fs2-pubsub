# fs2 PubSub

##### A GCP PubSub client based on [fs2](https://fs2.io/guide.html)

Very much in flux.

Do not use this library!

Basic Usage:

```scala
      val cfg = GrpcPubsubConfig.local(DefaultGcpProject, DefaultPubsubPort)

      val topicName        = "test-topic"
      val testSubscription = "test-sub"
      val msgCount         = 2000

      def businessLogic(i: Int)              = IO.unit
      def deadLetter(p: PubSubResponse[Int]) = IO.unit

      val program = (Publisher[IO](cfg), Subscriber[IO](cfg)).tupled.use { pubsub =>
        implicit val (pub, sub) = pubsub

        val setup =
          pub.createTopic(topicName) *>
            sub.createSubscription(testSubscription, topicName)

        val publish =
          Stream
            .emits[IO, Int](1 to msgCount)
            .toPubSub(topicName)

        val subscribe =
          sub.consume[Int](testSubscription) { s =>
            s.evalTap(
                msg =>
                  msg.body match {
                    case Right(i) => businessLogic(i)
                    case _        => deadLetter(msg)
                  }
              )
          }

        setup *> subscribe
          .concurrently(publish)
          .take(msgCount)
          .compile
          .toList
          .nested
          .map(_.body)
          .value
      }

      program.unsafeRunSync().map(_.right.get) shouldBe (1 to msgCount).toList
```