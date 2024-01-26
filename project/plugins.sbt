libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("com.github.sbt"    % "sbt-ci-release"     % "1.5.12")
addSbtPlugin("de.heikoseeberger" % "sbt-header"         % "5.0.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"       % "2.5.2")
addSbtPlugin("com.github.sbt"    % "sbt-github-actions" % "0.22.0")
