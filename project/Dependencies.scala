import sbt._

object Dependencies {

  private val compile = Seq(
    "com.typesafe.play" %% "play-json" % "2.8.2"
  )

  val compile2_12 =
    compile :+ "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.4.2" % "provided"

  val compile2_13 =
    compile :+ "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.5.1" % "provided"

  val test = Seq(
    "org.scalatest"       %% "scalatest"    % "3.2.12" % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % Test
  )

}
