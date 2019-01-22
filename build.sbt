import sbt.configs

lazy val root = (project in file("."))
  .aggregate(`fs2-pubsub-core`)


lazy val `fs2-pubsub-core` = (project in file("core"))
  .settings(
    Common(),
    name := "fs2-pubsub-core",
    resolvers ++= Dependencies.resolvers(),
    libraryDependencies ++= Dependencies(),
    Defaults.itSettings ++ headerSettings(IntegrationTest) ++ automateHeaderSettings(IntegrationTest),
    PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
  )
  .configs(IntegrationTest)
