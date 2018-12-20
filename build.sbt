import sbt.Keys._
import sbt._
import uk.gov.hmrc.versioning.SbtGitVersioning

val libName = "performance-test-runner"

val appDependencies =
  Seq(
    "uk.gov.hmrc"       %% "logback-json-logger"  % "4.2.0",
    "com.typesafe.play" %% "play-json"            % "2.6.10",
    "org.scalatest"     %% "scalatest"            % "3.0.5" % Test,
    "org.pegdown"       %  "pegdown"              % "1.6.0" % Test
  )

lazy val root = Project(libName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 3,
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12", "2.12.8"),
    libraryDependencies ++= appDependencies,
    libraryDependencies := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor == 12 =>
          libraryDependencies.value ++ Seq(
            "io.gatling.highcharts"  %  "gatling-charts-highcharts" % "2.3.1" % "provided"
          )
        case Some((2, scalaMajor)) if scalaMajor == 11 =>
          libraryDependencies.value ++ Seq(
            "io.gatling.highcharts"  %  "gatling-charts-highcharts" % "2.2.5" % "provided"
          )
        case _ =>
          libraryDependencies.value
      }
    },
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.typesafeRepo("releases")
    )
  )
