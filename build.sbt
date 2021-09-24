lazy val root = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .settings(
    majorVersion := 5,
    name := "performance-test-runner",
    makePublicallyAvailableOnBintray := true,
    scalaVersion := "2.12.12",
    //implicitConversions & postfixOps are Gatling recommended -language settings
    scalacOptions ++= Seq("-feature", "-language:implicitConversions", "-language:postfixOps"),
    libraryDependencies ++= Dependencies.compile ++ Dependencies.test
  )
