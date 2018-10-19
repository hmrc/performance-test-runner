import sbt.Keys._
import sbt._
import uk.gov.hmrc.versioning.SbtGitVersioning

val libName = "performance-test-runner"


val appDependencies = Seq(
  "uk.gov.hmrc"           %% "logback-json-logger"       % "3.1.0",
  "uk.gov.hmrc"           %% "hmrctest"                  % "1.4.0" % "test",
  "org.pegdown"           %  "pegdown"                   % "1.6.0" % "test",
  "io.gatling.highcharts" %  "gatling-charts-highcharts" % "2.2.5" % "provided",
  "com.typesafe.play"     %% "play-json"                 % "2.6.2"
)

lazy val root = Project(libName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 3,
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.11.7",
    libraryDependencies ++= appDependencies,
    crossScalaVersions := Seq("2.11.7"),
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.typesafeRepo("releases")
    )
  )