// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.1.1")

// Use sbt-scalariform for source code formatting
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.1")

