import sbt._

object Dependencies {

  def resolvers(): Seq[MavenRepository] = Seq(Resolver.sonatypeRepo("releases"), Resolver.bintrayRepo("engitano", "maven"))

  def apply(): Seq[ModuleID] = Seq(
    "io.grpc" % "grpc-netty-shaded" % "1.21.0",
    "io.grpc" % "grpc-auth" % "1.21.0",
    "com.engitano" %% "fs2-google-cloud-pubsub-v1" % "0.1.5",
    "com.google.auth" % "google-auth-library-oauth2-http" % "0.12.0",
    "org.typelevel" %% "cats-effect" % "1.1.0",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test, it",
    "com.whisk" %% "docker-testkit-scalatest" % "0.9.8" % "it",
    "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.8" % "it",
    "com.whisk" %% "docker-testkit-impl-docker-java" % "0.9.8" % "it"
  )
}
