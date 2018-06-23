
lazy val a = project.in(file("a")).settings(commonSettings)
lazy val b = project.in(file("b")).settings(commonSettings)
lazy val c = project.in(file("c")).settings(commonSettings)
lazy val d = project.in(file("d")).settings(commonSettings)

lazy val root = project.in(file(".")).settings(commonSettings).aggregate(a, b, c, d)

val detectRun = taskKey[Unit]("")
val detectRunAll = taskKey[Unit]("")
def commonSettings = Seq(
  detectRun := {
    val targetFile = target.value / "ran"
    IO.write(targetFile, "Test ran")
  },
  prValidatorTasks := Seq(detectRun)
)
ThisBuild / prValidatorTargetBranch := "targetBranch"

ThisBuild / validatePullRequest / includeFilter := (GlobFilter("*.include")
  || ValidatePullRequest.PathGlobFilter("**/inc/*")) || ValidatePullRequest.PathGlobFilter("a/include/this")

ThisBuild / validatePullRequest / excludeFilter := (ValidatePullRequest.PathGlobFilter("**/exclude/*") ||
  ValidatePullRequest.PathGlobFilter("b/dont.include"))

