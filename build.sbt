lazy val root = (project in file("."))
  .settings(
    majorVersion := 6,
    name := "performance-test-runner",
    isPublicArtefact := true,
    scalaVersion := "2.13.12",
    //implicitConversions & postfixOps are Gatling recommended -language settings
    scalacOptions ++= Seq("-feature", "-language:implicitConversions", "-language:postfixOps"),
    libraryDependencies ++= Dependencies.compile ++ Dependencies.test
  )
