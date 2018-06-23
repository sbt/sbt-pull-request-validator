
lazy val a = project.in(file("a"))
  .settings(commonSettings)
lazy val b = project.in(file("b")).dependsOn(a)
  .settings(commonSettings)
lazy val c = project.in(file("c"))
  .settings(commonSettings)

lazy val root = project.in(file(".")).settings(commonSettings).aggregate(a, b, c)

val detectRun = taskKey[Unit]("")
def commonSettings = Seq(
  detectRun := {
    val targetFile = target.value / "ran"
    IO.write(targetFile, "Test ran")
  },
  prValidatorTasks := Seq(detectRun)
)
ThisBuild / prValidatorTargetBranch := "targetBranch"
