lazy val root = (project in file("."))
  .settings(
    majorVersion := 6,
    name := "performance-test-runner",
    isPublicArtefact := true,
    scalaVersion := "3.3.5",
    //implicitConversions & postfixOps are Gatling recommended -language settings
    scalacOptions ++= Seq("-feature", "-language:implicitConversions", "-language:postfixOps"),
    libraryDependencies ++= Dependencies.compile ++ Dependencies.test
  )
