lazy val root = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .settings(
    majorVersion := 5,
    name := "performance-test-runner",
    isPublicArtefact := true,
    scalaVersion := "2.13.8",
    //implicitConversions & postfixOps are Gatling recommended -language settings
    scalacOptions ++= Seq("-feature", "-language:implicitConversions", "-language:postfixOps"),
    libraryDependencies ++= Dependencies.compile ++ Dependencies.test
  )
