import sbt._
import Keys._

object build extends Build {
  type Settings = Def.Setting[_]

  lazy val project = Project(
    id = "eff",
    base = file("."),
    settings = Defaults.coreDefaultSettings ++
               dependencies             ++
               projectSettings          ++
               compilationSettings      ++
               testingSettings
    )

  lazy val dependencies: Seq[Settings] =
    Seq[Settings](libraryDependencies ++=
        depend.scalaz   ++
        depend.specs2   ++
        depend.disorder
        )

  lazy val projectSettings: Seq[Settings] = Seq(
    name := "eff",
    version in ThisBuild := "1.0.0",
    organization := "org.specs2",
    scalaVersion := "2.11.7")

  lazy val compilationSettings: Seq[Settings] = Seq(
    javacOptions ++= Seq("-Xmx3G", "-Xms512m", "-Xss4m"),
    maxErrors := 20,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:_"),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

  lazy val testingSettings: Seq[Settings] = Seq(
    initialCommands in (Test, console) := "import org.specs2._",
    testFrameworks := Seq(TestFrameworks.Specs2),
    logBuffered := false,
    cancelable := true,
    javaOptions += "-Xmx3G"
  )

}
