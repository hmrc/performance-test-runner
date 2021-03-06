import sbt._

object Dependencies {

  val compile =
    Seq(
      "com.typesafe.play"    %% "play-json"                 % "2.6.10",
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.4.2" % "provided"
    )
  val test    =
    Seq("org.scalatest" %% "scalatest" % "3.0.5" % Test, "org.pegdown" % "pegdown" % "1.6.0" % Test)

}
