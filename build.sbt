val scala2_12 = "2.12.15"
val scala2_13 = "2.13.7"

lazy val root = (project in file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .settings(
    majorVersion := 5,
    name := "performance-test-runner",
    isPublicArtefact := true,
    scalaVersion := scala2_12,
    crossScalaVersions := Seq(scala2_12, scala2_13),
    //implicitConversions & postfixOps are Gatling recommended -language settings
    scalacOptions ++= Seq("-feature", "-language:implicitConversions", "-language:postfixOps"),
    libraryDependencies ++= (CrossVersion.partialVersion(Keys.scalaVersion.value) match {
      case Some((2, 12)) => Dependencies.compile2_12
      case _             => Dependencies.compile2_13
    }),
    libraryDependencies ++= Dependencies.test
  )
