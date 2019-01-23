addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.19")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
//addSbtPlugin("org.lyranthe.fs2-grpc" % "sbt-java-gen" % "0.3.0")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.8.2"
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")