lazy val root = (project in file("."))
  .settings(
    majorVersion := 6,
    name := "performance-test-runner",
    isPublicArtefact := true,
    crossScalaVersions := Seq("2.13.16", "3.3.6"),
    scalaVersion := crossScalaVersions.value.head,
    // implicitConversions & postfixOps are Gatling recommended -language settings
    scalacOptions ++= Seq("-feature", "-language:implicitConversions", "-language:postfixOps"),
    libraryDependencies ++= Dependencies.compile ++ Dependencies.test
  )
