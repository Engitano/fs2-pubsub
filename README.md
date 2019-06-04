# fs2 PubSub

##### A GCP PubSub client based on [fs2](https://fs2.io/guide.html)


Basic Usage:

```scala    
  val cfg = GrpcPubsubConfig.local(DefaultGcpProject, DefaultPubsubPort)

  val topicName        = "test-topic"
  val testSubscription = "test-sub"

  val setup: IO[Unit] = (Publisher[IO](cfg), Subscriber[IO](cfg)).tupled.use { pubsub =>
    val (pub, sub) = pubsub
    for {
      _ <- pub.createTopic(topicName)
      _ <- sub.createSubscription(testSubscription, topicName)
    } yield ()
  }

  val msgCount = 2000
  val program = (Publisher[IO](cfg), Subscriber[IO](cfg)).tupled.use { pubsub =>
    implicit val (pub, sub) = pubsub

    val publisher =
      Stream
        .emits[IO, Int](1 to msgCount)
        .toPubSub(topicName)

    val subscriber =
      sub.consume[Int](testSubscription)(r => r)

    setup *> subscriber
      .concurrently(publisher)
      .take(msgCount)
      .compile
      .toList
      .nested
      .map(_.body)
      .value
  }

  program.unsafeRunSync().map(_.right.get) shouldBe (1 to msgCount).toList
```