import sbt._

object Dependencies {

  val compile: Seq[ModuleID] = Seq(
    "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.6.1"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"       %% "scalatest"    % "3.2.16" % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % Test
  )

}
