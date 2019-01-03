name := "root-project"

lazy val a = project.in(file("modules/a"))
  .settings(commonSettings)
lazy val b = project.in(file("modules/b")).dependsOn(a)
  .settings(commonSettings)
lazy val c = project.in(file("modules/c"))
  .settings(commonSettings)
lazy val d = project.in(file("modules/extra-dir/d")).dependsOn(a)
  .settings(commonSettings)

lazy val root = project.in(file(".")).settings(commonSettings).aggregate(a, b, c, d)

val detectRun = taskKey[Unit]("")
def commonSettings = Seq(
  detectRun := {
    val targetFile = target.value / "ran"
    IO.write(targetFile, "Test ran")
  },
  prValidatorTasks := Seq(detectRun)
)
ThisBuild / prValidatorTargetBranch := "targetBranch"
ThisBuild / prValidatorTravisNonPrEnforcedBuildAll := false