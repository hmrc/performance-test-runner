lazy val root = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 3,
    name := "performance-test-runner",
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.12.12",
    crossScalaVersions := Seq("2.11.12", "2.12.12"),
    libraryDependencies ++= Dependencies.compile ++ Dependencies.test,
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
    }
  )