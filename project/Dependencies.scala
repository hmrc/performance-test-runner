import sbt._

object Dependencies {

  private val gatlingVersion = "3.6.1"

  // The `config` and `gatling-test-framework` libraries are provided so as to be available transitively to services
  // running performance tests using standard HMRC approach
  val compile: Seq[ModuleID] = Seq(
    "com.typesafe"          % "config"                    % "1.4.2",
    "io.gatling"            % "gatling-test-framework"    % gatlingVersion,
    "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"       %% "scalatest"    % "3.2.16" % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % Test
  )

}
