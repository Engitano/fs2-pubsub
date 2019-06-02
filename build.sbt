import sbt.configs

lazy val root = (project in file("."))
  .aggregate(`fs2-pubsub`)

val majorVersion = SettingKey[String]("major version")
val minorVersion = SettingKey[String]("minor version")
val patchVersion = SettingKey[Option[String]]("patch version")

Global / majorVersion := "0"
Global / minorVersion := "1"
Global / patchVersion := Some("0")

lazy val `fs2-pubsub` = (project in file("."))
  .settings(
    Common(),
    name := "fs2-pubsub",
    version := s"${majorVersion.value}.${minorVersion.value}${patchVersion.value
      .fold("")(p => s".$p")}",
    resolvers ++= Dependencies.resolvers(),
    libraryDependencies ++= Dependencies(),
    bintrayOrganization := Some("engitano"),
    bintrayPackageLabels := Seq("pubsub", "fs2"),
    Defaults.itSettings ++ headerSettings(IntegrationTest) ++ automateHeaderSettings(
      IntegrationTest
    ),
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
  )
  .configs(IntegrationTest)
