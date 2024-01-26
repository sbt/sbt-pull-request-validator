lazy val `sbt-pull-request-validator` = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)

sbtPlugin := true
enablePlugins(SbtPlugin)

lazy val scala212 = "2.12.18"
ThisBuild / crossScalaVersions := Seq(scala212)
ThisBuild / scalaVersion := scala212

organization := "com.github.sbt"

homepage := Some(url("https://github.com/sbt/sbt-pull-request-validator"))
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

organizationName := "Hewlett Packard Enterprise"
startYear := Some(2018)

// https://mvnrepository.com/artifact/org.kohsuke/github-api
libraryDependencies += "org.kohsuke" % "github-api" % "1.93"

scriptedLaunchOpts ++= Seq(
  "-Dplugin.version=" + version.value
)

// Bintray
bintrayOrganization := Some("sbt")
bintrayRepository := "sbt-plugin-releases"
bintrayPackage := "sbt-pull-request-validator"
bintrayReleaseOnPublish := false

// Release
import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("test"),
  releaseStepCommandAndRemaining("scripted"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publish"),
  releaseStepTask(`sbt-pull-request-validator` / bintrayRelease),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted")))

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
// Remove this when we set up publishing via sbt-ci-release
ThisBuild / githubWorkflowPublishTargetBranches := Seq()

ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest")

ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("8"),
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17"),
  JavaSpec.temurin("21")
)

// Necessary to setup git so that sbt-scripted tests can run on github actions
ThisBuild / githubWorkflowBuildPreamble := Seq(
  WorkflowStep.Run(
    commands = List(
      "git config --global init.defaultBranch main",
      """git config --global user.email "sbt-pull-request-validator@github.com"""",
      """git config --global user.name "Sbt Pull Request Validator""""
    ),
    name = Some("Setup git")
  )
)
