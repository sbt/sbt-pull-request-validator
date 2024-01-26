lazy val a = project.in(file("a")).settings(commonSettings)

lazy val root = project.in(file(".")).settings(commonSettings).aggregate(a)

val detectRun = taskKey[Unit]("")
val detectRunAll = taskKey[Unit]("")
def commonSettings = Seq(
  detectRun := {
    val targetFile = target.value / "ran"
    IO.write(targetFile, "Test ran")
  },
  prValidatorTasks := Seq(detectRun),
  detectRunAll := {
    val targetFile = target.value / "ran-all"
    IO.write(targetFile, "Test ran")
  },
  prValidatorBuildAllTasks := Seq(detectRunAll)
)
ThisBuild / prValidatorTargetBranch := "targetBranch"
ThisBuild / prValidatorTravisNonPrEnforcedBuildAll := false
