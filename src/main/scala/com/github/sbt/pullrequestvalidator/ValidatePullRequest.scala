/*
 * Sbt Pull Request Validator
 * Copyright Lightbend and Hewlett Packard Enterprise
 *
 * Licensed under Apache License 2.0
 * SPDX-License-Identifier: Apache-2.0
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package com.github.sbt.pullrequestvalidator

import java.util.regex.Pattern

import org.kohsuke.github._
import sbt.Keys._
import sbt._

import scala.collection.immutable
import scala.sys.process._
import scala.util.matching.Regex

object ValidatePullRequest extends AutoPlugin {

  final case class Changes(allBuildMatched: Boolean, projectRefs: immutable.Set[ProjectRef])

  /**
   * Like GlobFilter, but matches on the full path, and ** matches /, while * doesn't.
   */
  object PathGlobFilter {
    def apply(glob: String): FileFilter = {
      val pattern = Pattern.compile(
        // First, split by **
        glob
          .split("\\*\\*", -1)
          .map {
            case ""   => ""
            case part =>
              // Now, handle *
              part
                .split("\\*", -1)
                .map {
                  case ""      => ""
                  case literal => Pattern.quote(literal)
                }
                .mkString("[^/]*")
          }
          .mkString(".*")
      )
      (pathname: File) => pattern.matcher(pathname.getPath).matches()
    }
  }

  object autoImport {
    // git configuration
    lazy val prValidatorSourceBranch = settingKey[String]("Branch containing the changes of this PR")
    lazy val prValidatorTargetBranch = settingKey[String]("Target branch of this PR, defaults to `main`")

    // Configuration for what tasks to run in which scenarios
    lazy val prValidatorTasks =
      settingKey[Seq[TaskKey[?]]]("The tasks that should be run on a project when that project has changed")
    lazy val prValidatorBuildAllTasks = settingKey[Seq[TaskKey[?]]](
      "The tasks that should be run when one of the files that have changed matches the build all filters"
    )
    lazy val prValidatorEnforcedBuildAllTasks =
      settingKey[Seq[TaskKey[?]]]("The tasks that should be run when build all is explicitly requested")

    // enforced build all configuration
    lazy val prValidatorBuildAllKeyword = settingKey[Regex](
      "Magic phrase to be used to trigger building of the entire project instead of analysing dependencies"
    )
    lazy val prValidatorGithubEndpoint = settingKey[URI](
      "URI for the GitHub API, defaults to https://api.github.com. Override for GitHub enterprise, note that the URI for GitHub enterprise should end in /api/v3."
    )
    lazy val prValidatorGithubRepository = settingKey[Option[String]](
      "Optional name of the repository where Pull Requests are created. Necessary for explicit 'enforced build all'"
    )
    lazy val prValidatorGithubEnforcedBuildAll =
      taskKey[Boolean]("Checks via GitHub API if comments included the PLS BUILD ALL keyword.")
    lazy val prValidatorTravisNonPrEnforcedBuildAll =
      taskKey[Boolean]("Checks whether this is a non PR build on Travis.")
    lazy val prValidatorEnforcedBuildAll = taskKey[Boolean]("Whether an enforced build all is done.")

    // determining touched dirs and projects
    lazy val prValidatorChangedProjects = taskKey[Changes]("List of touched projects in this PR branch")
    lazy val prValidatorProjectBuildTasks =
      taskKey[Seq[TaskKey[?]]]("The tasks that should be run, according to what has changed")

    // running validation
    lazy val validatePullRequest = taskKey[Unit]("Validate pull request")
    lazy val validatePullRequestBuildAll = taskKey[Unit]("Validate pull request, building all projects")
  }

  import autoImport._

  override lazy val trigger = allRequirements

  override lazy val requires = plugins.JvmPlugin

  /*
    Assumptions:
      Env variables set "by Jenkins" are assumed to come from this plugin:
      https://wiki.jenkins-ci.org/display/JENKINS/GitHub+pull+request+builder+plugin
   */

  // settings
  private val JenkinsPullIdEnvVarName = "ghprbPullId" // Set by "GitHub pull request builder plugin"
  private val TravisPullIdEnvVarName = "TRAVIS_PULL_REQUEST"

  private val TravisRepoName = "TRAVIS_REPO_SLUG"

  private val TargetBranchEnvVarName = "PR_TARGET_BRANCH"
  private val TargetBranchTravisEnvVarName = "TRAVIS_BRANCH"
  private val TargetBranchJenkinsEnvVarName = "ghprbTargetBranch"

  private val SourceBranchEnvVarName = "PR_SOURCE_BRANCH"

  private lazy val localTargetBranch = sys.env.get(TargetBranchEnvVarName)
  private lazy val jenkinsTargetBranch = sys.env.get(TargetBranchJenkinsEnvVarName).map("origin/" + _)
  private lazy val travisTargetBranch = sys.env.get(TargetBranchTravisEnvVarName).map("origin/" + _)

  private lazy val localSourceBranch = sys.env.get(SourceBranchEnvVarName)
  private lazy val jenkinsSourceBranch = sys.env.get(JenkinsPullIdEnvVarName).map("pullreq/" + _)

  private lazy val jenkinsPullRequestId = sys.env.get(JenkinsPullIdEnvVarName).map(_.toInt)
  private lazy val travisPullRequestId = sys.env.get(TravisPullIdEnvVarName).filterNot(_ == "false").map(_.toInt)
  private lazy val pullRequestId = jenkinsPullRequestId orElse travisPullRequestId

  override lazy val globalSettings = Seq(
    validatePullRequest := (),
    validatePullRequestBuildAll := (),
    prValidatorSourceBranch := {
      localSourceBranch orElse jenkinsSourceBranch getOrElse "HEAD"
    },
    prValidatorTargetBranch := {
      localTargetBranch orElse jenkinsTargetBranch orElse travisTargetBranch getOrElse "origin/main"
    },
    prValidatorTasks := Nil,
    prValidatorBuildAllTasks := Nil,
    prValidatorEnforcedBuildAllTasks := Nil,
    prValidatorGithubEndpoint := uri("https://api.github.com"),
    prValidatorGithubRepository := sys.env.get(TravisRepoName),
    prValidatorBuildAllKeyword := """PLS BUILD ALL""".r,
    prValidatorGithubEnforcedBuildAll := false,
    prValidatorTravisNonPrEnforcedBuildAll := {
      sys.env.get(TravisPullIdEnvVarName).contains("false")
    },
    prValidatorEnforcedBuildAll := false,
    prValidatorChangedProjects := Changes(allBuildMatched = false, Set.empty),
    prValidatorProjectBuildTasks := Nil
  )

  override lazy val buildSettings = Seq(
    validatePullRequest / includeFilter := "*",
    validatePullRequest / excludeFilter := "README.*",
    validatePullRequestBuildAll / includeFilter := PathGlobFilter("project/**") || PathGlobFilter("*.sbt"),
    validatePullRequestBuildAll / excludeFilter := NothingFilter,
    prValidatorGithubEnforcedBuildAll := {
      val log = streams.value.log
      val buildAllMagicPhrase = prValidatorBuildAllKeyword.value
      val githubRepository = prValidatorGithubRepository.value
      val githubEndpoint = prValidatorGithubEndpoint.value
      val githubCredentials = Credentials.forHost(credentials.value, githubEndpoint.getHost)

      pullRequestId.exists { prId =>
        log.info("Checking GitHub comments for PR validation options...")

        try {
          import scala.collection.JavaConverters._
          val gh = {
            val builder = GitHubBuilder.fromEnvironment().withEndpoint(githubEndpoint.toString)
            githubCredentials match {
              case Some(creds) => builder.withOAuthToken(creds.passwd).build()
              case _           => builder.build()
            }
          }
          val commentsOpt =
            githubRepository.map(repository => gh.getRepository(repository).getIssue(prId).getComments.asScala)

          def triggersBuildAll(c: GHIssueComment): Boolean = buildAllMagicPhrase.findFirstIn(c.getBody).isDefined

          val shouldBuildAll = commentsOpt.exists(comments => comments.exists(c => triggersBuildAll(c)))

          if (shouldBuildAll)
            log.info(s"Building all projects because of 'build all' Github comment")

          shouldBuildAll
        } catch {
          case ex: Exception =>
            log.warn("Unable to reach GitHub! Exception was: " + ex.getMessage)
            false
        }
      }
    },
    prValidatorEnforcedBuildAll := prValidatorTravisNonPrEnforcedBuildAll.value || prValidatorGithubEnforcedBuildAll.value,
    prValidatorChangedProjects := {
      val log = streams.value.log

      val prId = prValidatorSourceBranch.value

      val target = prValidatorTargetBranch.value

      val state = Keys.state.value
      val extracted = Project.extract(state)
      val rootBaseDir = (ThisBuild / baseDirectory).value
      // All projects in reverse order of path, this ensures when we search through them, we get the most specific
      // first
      val projects = extracted.structure.allProjects
        .flatMap(project =>
          project.base
            .relativeTo(rootBaseDir)
            .map { relativePath =>
              val projRef = ProjectRef(extracted.structure.root, project.id)
              if (relativePath.getPath == "")
                "" -> projRef
              else
                relativePath.getPath + "/" -> projRef
            }
        )
        .sortBy(_._1)
        .reverse

      val filter = (validatePullRequest / includeFilter).value -- (validatePullRequest / excludeFilter).value
      val allBuildFilter =
        (validatePullRequestBuildAll / includeFilter).value -- (validatePullRequestBuildAll / excludeFilter).value

      log.info(s"Diffing [$prId] to determine changed modules in PR...")
      val diffOutput = s"git diff $target --name-only".!!.split("\n")
      val diffedFiles = diffOutput
        .map(l => file(l.trim))
        .toSeq

      val statusOutput = s"git status --short".!!.split("\n")
      val dirtyFiles = statusOutput
        .filterNot(_.isEmpty)
        // Need to drop the leading status characters, followed by a space
        .map(l => file(l.trim.dropWhile(_ != ' ').drop(1)))

      if (dirtyFiles.nonEmpty)
        log.info("Detected uncommitted changes: " + dirtyFiles.take(5).mkString("[", ",", "]"))

      val allChangedFiles = diffedFiles ++ dirtyFiles

      val allBuildMatched = allChangedFiles.exists(allBuildFilter.accept)

      val changedProjects = allChangedFiles
        .filter(filter.accept)
        .flatMap { file =>
          projects.collectFirst {
            case (path, project) if file.getPath.startsWith(path) => project
          }
        }

      if (allBuildMatched)
        log.info("Building all modules because the all build filter was matched")

      Changes(allBuildMatched, changedProjects.toSet)
    }
  )

  override lazy val projectSettings = Seq(
    prValidatorProjectBuildTasks := {
      val log = streams.value.log
      val proj = name.value
      log.debug(s"Analysing project (for inclusion in PR validation): [$proj]")
      val changedProjects = prValidatorChangedProjects.value
      val changedProjectTasks = prValidatorBuildAllTasks.value

      val enforcedBuildAll = prValidatorEnforcedBuildAll.value
      val enforcedBuildAllTasks = prValidatorEnforcedBuildAllTasks.value

      val projectDependencies = Keys.buildDependencies.value.classpathRefs(thisProjectRef.value)

      def isDependency: Boolean =
        projectDependencies.exists(changedProjects.projectRefs) || changedProjects.projectRefs(thisProjectRef.value)

      val dependencyChangedTasks = prValidatorTasks.value

      if (enforcedBuildAll) {
        log.debug(s"Building [$proj] because this is an enforced all build project")
        enforcedBuildAllTasks
      } else if (changedProjects.allBuildMatched) {
        log.debug(s"Building [$proj] because the all build filter was matched")
        changedProjectTasks
      } else if (isDependency) {
        log.info(s"Building [$proj] because it or a dependency has changed")
        dependencyChangedTasks
      } else {
        log.debug(s"Skipping build of [$proj] because it, and none of its dependencies are changed")
        Seq()
      }
    },
    prValidatorTasks := Seq(Test / test),
    prValidatorBuildAllTasks := prValidatorTasks.value,
    prValidatorEnforcedBuildAllTasks := prValidatorBuildAllTasks.value,
    validatePullRequest := Def.taskDyn {
      val validationTasks = prValidatorProjectBuildTasks.value

      // Create a task for every validation task key and
      // then zip all of the tasks together discarding outputs.
      // Task failures are propagated as normal.
      val zero: Def.Initialize[Seq[Task[Any]]] = Def.setting {
        Seq(task(()))
      }
      validationTasks
        .map(taskKey =>
          Def.task {
            taskKey.value
          }
        )
        .foldLeft(zero) { (acc, current) =>
          acc.zipWith(current) { case (taskSeq, task) =>
            taskSeq :+ task.asInstanceOf[Task[Any]]
          }
        } apply { (tasks: Seq[Task[Any]]) =>
        tasks.join map { _ => () /* Ignore the sequence of unit returned */ }
      }
    }.value,
    validatePullRequestBuildAll := Def.taskDyn {
      val validationTasks = prValidatorBuildAllTasks.value

      // Create a task for every validation task key and
      // then zip all of the tasks together discarding outputs.
      // Task failures are propagated as normal.
      val zero: Def.Initialize[Seq[Task[Any]]] = Def.setting {
        Seq(task(()))
      }
      validationTasks
        .map(taskKey =>
          Def.task {
            taskKey.value
          }
        )
        .foldLeft(zero) { (acc, current) =>
          acc.zipWith(current) { case (taskSeq, task) =>
            taskSeq :+ task.asInstanceOf[Task[Any]]
          }
        } apply { (tasks: Seq[Task[Any]]) =>
        tasks.join map { _ => () /* Ignore the sequence of unit returned */ }
      }
    }
  )
}
