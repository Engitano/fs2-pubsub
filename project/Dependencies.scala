import sbt._

object Dependencies {

  def resolvers() = Seq(
    Resolver.sonatypeRepo("releases")
  )

  def apply() = Seq(
    "io.grpc" % "grpc-netty-shaded" % "1.17.1",
    "io.grpc" % "grpc-auth" % "1.17.1",
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "com.google.auth" % "google-auth-library-oauth2-http" % "0.12.0",
    "org.lyranthe.fs2-grpc" % "java-runtime_2.12" % "0.4.0-M3",
    "co.fs2"        %% "fs2-core"         % "1.0.2",
    "org.typelevel" %% "cats-effect"      % "1.1.0",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test, it",
    "com.whisk" %% "docker-testkit-scalatest" % "0.9.8" % "it",
    "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.8" % "it",
    "com.whisk" %% "docker-testkit-impl-docker-java" % "0.9.8" % "it"
  )
}
