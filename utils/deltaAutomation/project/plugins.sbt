logLevel := Level.Warn

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.10")

addSbtPlugin("org.scalariform"  % "sbt-scalariform" % "1.6.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.4.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4")

