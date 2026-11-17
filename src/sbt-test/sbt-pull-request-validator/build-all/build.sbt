import sbtcompat.PluginCompat._

lazy val a = project.in(file("a")).settings(commonSettings)

lazy val root = project.in(file(".")).settings(commonSettings).aggregate(a)

val detectRun = taskKey[Unit]("")
val detectRunAll = taskKey[Unit]("")
def commonSettings = Seq(
  // Keep marker paths stable; these tasks deliberately perform uncached side effects.
  target := baseDirectory.value / "target",
  detectRun := Def.uncached {
    val targetFile = target.value / "ran"
    IO.write(targetFile, "Test ran")
  },
  prValidatorTasks := Seq(detectRun),
  detectRunAll := Def.uncached {
    val targetFile = target.value / "ran-all"
    IO.write(targetFile, "Test ran")
  },
  prValidatorBuildAllTasks := Seq(detectRunAll)
)
ThisBuild / prValidatorTargetBranch := "targetBranch"
ThisBuild / prValidatorTravisNonPrEnforcedBuildAll := false
