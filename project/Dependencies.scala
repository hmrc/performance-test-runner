import sbt._

object Dependencies {

  val compile: Seq[ModuleID] = Seq(
    "com.typesafe.play"    %% "play-json"                 % "2.9.4",
    "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.6.1"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"       %% "scalatest"    % "3.2.16" % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % Test
  )

}
