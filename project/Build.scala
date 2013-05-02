import sbt._
import Keys._
import play.Project._

object PdfreakBuild extends Build {

  lazy val PdfreakProject = play.Project(
    BuildSettings.PROJECT_NAME).settings(
      BuildSettings.GLOBAL_SETTINGS ++
        Seq(
          libraryDependencies ++= (Dependencies.DEFAULTS ++ Dependencies.SCATTER ++ Dependencies.PLAYMATE ++ Dependencies.DATABASE ++ Dependencies.HASHING ++ Dependencies.XDOCREPORT),
          resolvers ++= (Resolvers.TYPESAFE ++ Resolvers.BINDSCHA),

          // Override doc task to generate one documentation for all subprojects 
          doc <<= Tasks.docTask(file("documentation/api")),
          aggregate in doc := false))

  object BuildSettings {
    val ORGANIZATION = "com.bindscha"
    val ORGANIZATION_NAME = "Bindschaedler"
    val ORGANIZATION_HOMEPAGE = "http://www.bindschaedler.com"

    val PROJECT_NAME = "PDFreak"
    val PROJECT_VERSION = "0.1.0"

    val INCEPTION_YEAR = 2012

    val PUBLISH_DOC = Option(System.getProperty("publish.doc")).isDefined

    val SCALA_VERSION = "2.10.0"
    val BINARY_SCALA_VERSION = CrossVersion.binaryScalaVersion(SCALA_VERSION)

    val SCALARIFORM_SETTINGS = com.typesafe.sbt.SbtScalariform.scalariformSettings

    val GLOBAL_SETTINGS: Seq[sbt.Project.Setting[_]] = Seq(
      organization := ORGANIZATION,
      organizationName := ORGANIZATION_NAME,
      organizationHomepage := Some(url(ORGANIZATION_HOMEPAGE)),

      version := PROJECT_VERSION,

      scalaVersion := SCALA_VERSION,
      scalaBinaryVersion := BINARY_SCALA_VERSION,

      publishArtifact in packageDoc := PUBLISH_DOC,

      scalacOptions ++= Seq("-Xlint", "-deprecation", "-unchecked", "-encoding", "utf8"),
      javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-g", "-encoding", "utf8")) ++ SCALARIFORM_SETTINGS
  }

  object Resolvers {
  
    val TYPESAFE_RELEASES = "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"
    val TYPESAFE_SNAPSHOTS = "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    val TYPESAFE = Seq(TYPESAFE_RELEASES) ++ (if (BuildSettings.PROJECT_VERSION.endsWith("SNAPSHOT")) Seq(TYPESAFE_SNAPSHOTS) else Nil)

    val BINDSCHA_RELEASES = "Bindscha Releases Repo" at "http://repo.bindschaedler.com/releases/"
    val BINDSCHA_SNAPSHOTS = "Bindscha Snapshots Repo" at "http://repo.bindschaedler.com/snapshots/"
    val BINDSCHA = Seq(BINDSCHA_RELEASES) ++ (if (BuildSettings.PROJECT_VERSION.endsWith("SNAPSHOT")) Seq(BINDSCHA_SNAPSHOTS) else Nil)
    
  }

  object Dependencies {
  
    val DEFAULTS = Seq(
      "commons-io" % "commons-io" % "2.4")

    val SCATTER = Seq(
      "com.bindscha" %% "scatter-random" % "0.1.0",
      "com.bindscha" %% "scatter-regex" % "0.1.0")

    val PLAYMATE = Seq(
      "com.bindscha" %% "playmate-auth" % "0.1.0",
      "com.bindscha" %% "playmate-navigation" % "0.1.0", 
      "com.bindscha" %% "playmate-boilerplate" % "0.1.0")

    val DATABASE = Seq(
      jdbc,
      anorm,
      "com.typesafe.slick" %% "slick" % "1.0.0",
      "com.typesafe.play" %% "play-slick" % "0.3.2", 
      "postgresql" % "postgresql" % "9.1-901.jdbc4")

    val HASHING = Seq(
      "org.mindrot" % "jbcrypt" % "0.3m")
      
	val XDOCREPORT = Seq(
	  "com.itextpdf" % "itextpdf" % "5.1.3", 
	  "fr.opensagres.xdocreport" % "fr.opensagres.xdocreport.template.velocity" % "1.0.1", 
	  "fr.opensagres.xdocreport" % "fr.opensagres.xdocreport.document.docx" % "1.0.1", 
	  "fr.opensagres.xdocreport" % "org.apache.poi.xwpf.converter.pdf" % "1.0.1")
	  
  }

  object Tasks {

    // ----- Generate documentation
    def docTask(docRoot: java.io.File, maximumErrors: Int = 10) = (dependencyClasspath in Test, compilers, streams) map { (classpath, compilers, streams) =>
      // Clear the previous version of the doc
      IO.delete(docRoot)

      // Grab all jars and source files
      val jarFiles = (file("app") ** ("*.scala" || "*.java") +++ (file("modules") ** ("*.jar"))).get
      val sourceFiles = (file("app") ** ("*.scala" || "*.java") +++ (file("modules") ** ("*.scala" || "*.java"))).get

      // Run scaladoc
      new Scaladoc(maximumErrors, compilers.scalac)(
        BuildSettings.PROJECT_NAME + " " + BuildSettings.PROJECT_VERSION + " - " + "Scala API",
        sourceFiles,
        classpath.map(_.data) ++ jarFiles,
        docRoot,
        Seq(
          "-external-urls:" + (Map(
            "scala" -> "http://www.scala-lang.org/api/current/") map (p => p._1 + "=" + p._2) mkString (";")),
          "-skip-packages", Seq(
            "controllers") mkString (":"),
          "-doc-footer", "Copyright (c) " +
            BuildSettings.INCEPTION_YEAR + "-" + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) +
            " " + BuildSettings.ORGANIZATION_NAME + ". All rights reserved.",
          "-diagrams"),
        streams.log)

      // Return documentation root
      docRoot
    }

  }

  // ----- Augment sbt.Project with a settings method that takes a Seq

  class ImprovedProject(val sbtProject: Project) {
    def settings(ss: Seq[sbt.Project.Setting[_]]): Project =
      sbtProject.settings(ss: _*)
  }

  implicit def project2improvedproject(sbtProject: Project): ImprovedProject = new ImprovedProject(sbtProject)
  implicit def improvedproject2project(improvedProject: ImprovedProject): Project = improvedProject.sbtProject

}
