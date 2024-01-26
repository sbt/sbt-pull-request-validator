libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

addSbtPlugin("org.foundweekends" % "sbt-bintray"        % "0.5.3")
addSbtPlugin("com.github.gseitz" % "sbt-release"        % "1.0.8")
addSbtPlugin("de.heikoseeberger" % "sbt-header"         % "5.0.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"       % "2.5.2")
addSbtPlugin("com.github.sbt"    % "sbt-github-actions" % "0.22.0")
