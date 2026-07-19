libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("com.github.sbt" % "sbt-ci-release"     % "1.12.0")
addSbtPlugin("com.github.sbt" % "sbt-header"         % "5.11.0")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"       % "2.6.2")
addSbtPlugin("com.github.sbt" % "sbt-github-actions" % "0.31.0")
