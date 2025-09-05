lazy val `sbt-pull-request-validator` = project in file(".")

sbtPlugin := true
enablePlugins(SbtPlugin)

lazy val scala212 = "2.12.20"
ThisBuild / crossScalaVersions := Seq(scala212)
ThisBuild / scalaVersion := scala212

organization := "com.github.sbt"

homepage := Some(url("https://github.com/sbt/sbt-pull-request-validator"))
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

startYear := Some(2018)

ThisBuild / developers := List(
  Developer(
    "sbt-pull-request-validator",
    "Sbt Pull Request Validator Contributors",
    "",
    url("https://github.com/sbt/sbt-pull-request-validator/graphs/contributors")
  )
)

// https://mvnrepository.com/artifact/org.kohsuke/github-api
libraryDependencies += "org.kohsuke" % "github-api" % "1.330"

scriptedLaunchOpts ++= Seq(
  "-Dplugin.version=" + version.value
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-encoding",
  "UTF-8"
)

scalacOptions ++= {
  if (insideCI.value) {
    val log = sLog.value
    log.info("Running in CI, enabling Scala2 optimizer")
    Seq(
      "-opt-inline-from:<sources>",
      "-opt:l:inline"
    )
  } else Nil
}

// So that publishLocal doesn't continuously create new versions
def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val snapshotSuffix =
    if (out.isSnapshot()) "-SNAPSHOT"
    else ""
  out.ref.dropPrefix + snapshotSuffix
}

def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer timestamp d}"

ThisBuild / version := dynverGitDescribeOutput.value.mkVersion(versionFmt, fallbackVersion(dynverCurrentDate.value))
ThisBuild / dynver := {
  val d = new java.util.Date
  sbtdynver.DynVer.getGitDescribeOutput(d).mkVersion(versionFmt, fallbackVersion(d))
}

ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted")))

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("main"))
  )
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest")

ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("8"),
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17"),
  JavaSpec.temurin("21")
)

ThisBuild / githubWorkflowBuildMatrixExclusions +=
  MatrixExclude(Map("java" -> "temurin@8", "os" -> "macos-latest"))

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
