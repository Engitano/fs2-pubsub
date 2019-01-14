name := "fs2-pubsub"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq (
  "io.grpc" % "grpc-netty-shaded" % "1.17.1",
  "io.grpc" % "grpc-auth" % "1.17.1",
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.google.auth" % "google-auth-library-oauth2-http" % "0.12.0",
  "org.lyranthe.fs2-grpc" % "java-runtime_2.12" % "0.4.0-M3",
  "co.fs2"        %% "fs2-core"         % "1.0.2",
  "org.typelevel" %% "cats-effect"      % "1.1.0",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test, it"

)
Defaults.itSettings
configs(IntegrationTest)
//enablePlugins(Fs2Grpc)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)