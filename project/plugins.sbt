addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.19")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.8.2"
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")