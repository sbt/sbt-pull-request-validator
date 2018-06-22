sbtPlugin := true

name := "sbt-pull-request-validator"
organization := "com.hpe.sbt"

// https://mvnrepository.com/artifact/org.kohsuke/github-api
libraryDependencies += "org.kohsuke" % "github-api" % "1.93"

scriptedLaunchOpts ++= Seq(
  "-Dplugin.version=" + version.value
)
scriptedBufferLog := false
