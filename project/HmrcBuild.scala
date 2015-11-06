import sbt.Keys._
import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object HmrcBuild extends Build {

  val nameApp = "configuration-driven-simulation"

  val appDependencies = Seq(
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.0",
    "uk.gov.hmrc" %% "hmrctest" % "1.4.0" % "test",
    "org.pegdown" % "pegdown" % "1.5.0" % "test",

    "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.1.7" % "provided"
  )

  lazy val addressModel = Project(nameApp, file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      scalaVersion := "2.11.7",
      libraryDependencies ++= appDependencies,
      crossScalaVersions := Seq("2.11.7"),
      resolvers := Seq(
        Resolver.url("hmrc-releases",
          url("https://dl.bintray.com/hmrc/releases")),
        Resolver.bintrayRepo("hmrc", "releases"),
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
      )
    )
}

