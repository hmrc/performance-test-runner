import sbt._

object Dependencies {

  val compile =
    Seq(
      "uk.gov.hmrc"       %% "logback-json-logger" % "4.2.0",
      "com.typesafe.play" %% "play-json"           % "2.6.10"
    )
  val test    =
    Seq("org.scalatest" %% "scalatest" % "3.0.5" % Test, "org.pegdown" % "pegdown" % "1.6.0" % Test)

}
