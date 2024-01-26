# GitHub Pull Request Validator

This plugin provides pull request optimization support for GitHub validators. It works with both [Travis](https://travis-ci.org) and the [Jenkins Pull Request Builder plugin](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+pull+request+builder+plugin). Optimization is done by only building projects that have changed files, and the projects that depend on them.

This was originally written by [@ktoso](https://github.com/ktoso) for Akka, and extracted into a standalone sbt plugin and generalized by HPE.

## Adding to your project

You can add the plugin to your project by adding the following to `project/plugins.sbt`:

```scala
addSbtPlugin("com.github.sbt" % "sbt-pull-request-validator" % "1.0.0")
```

Then, configure your project to run `sbt validatePullRequest` when pull requests get built.

## Configuration

### Controlling what gets run

By default, the pull request validator will run `Test / test`. This can be defined, or additional tasks can be added, using the `prValidatorTasks` setting, for example, to add running integration tests:

```scala
prValidatorTasks += IntegrationTest / test
```

This setting needs to be defined per project.

### Controlling which files trigger validation

By default, all files will trigger validation except any file called `README.*`. This can be controlled using `includeFilter` and `excludeFilter` in the `validatePullRequest` scope. The way these filters are applied to the changed files is a little different to the way sbt usually applies include and exclude filters, the filter is applied to each changed path, relative to the build root directory. It is not applied to directories, so recursive inclusion by directory name is not supported. To assist with defining filters, the plugin provides a `ValidatePullRequest.PathGlobFilter`, which runs globs against the entire path, supporting both `*` for matching a single file/directory level, and `**` for matching multiple levels at once.

For example, if you want to exclude documentation changes from triggering the root project to build, you can do this:

```scala
ThisBuild / validatePullRequest / excludeFilter :=
  (ThisBuild / validatePullRequest / excludeFilter).value || ValidatePullRequest.PathGlobFilter("docs/**")
```

As you can see, this must be configured at the build level, not at the project level.

### Triggering all projects to build

There are some files that, when changed, should trigger all projects to build. For example, if any `*.sbt` files in the root directory, or any files in the `project` directory have changed, then this generally means that all projects should be built. This can be configured using the `includeFilter` and `excludeFilter` in `validatePullRequestBuildAll` task. By default, these match all project build files as mentioned above.

If you want to make a file called `foo.txt` trigger all projects to build as well, this can be done like so:

```scala
ThisBuild / validatePullRequestBuildAll / includeFilter :=
  (ThisBuild / validatePullRequestBuildAll / includeFilter).value || "foo.txt"
```

As with the other include/exclude filters, this must be configured at the build level.

You can also control the tasks that run when all projects are built using `prValidatorBuildAllTasks`, by default it will be the same as gets run when a project is changed:

```scala
prValidatorBuildAllTasks += IntegrationTest / test
```

This setting is defined per project.

### Forcing all projects to build

You can force all projects to build by adding a phrase to a comment on the PR. By default, this phrase is `PLS BUILD ALL`. This can be configured using the `prValidatorBuildAllKeyword` setting, which takes a regular expression. If you want to change it from the default:

```scala
ThisBuild / prValidatorBuildAllKeyword := "BUILD ALL PLEASE".r
```

By default the same tasks will be run as the build all tasks. To change the tasks that get run when this is specified, you can configure the `prValidatorEnforcedBuildAllTasks`:

```scala
prValidatorEnforcedBuildAllTasks += IntegrationTest / test
```

If using Travis, this should work out of the box with open source GitHub projects. If using Jenkins, you will need to configure the GitHub repository that gets used:

```scala
ThisBuild / prValidatorGithubRepository := Some("myorg/myrepo")
```

For non open source builds, or if you want to ensure you don't exceed GitHub rate limiting, you need to configure credentials for accessing GitHub. This can be done by creating a [GitHub OAuth token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/), and then adding that to the sbt `credentials`. Typically this should be done globally, by adding the following to `~/.sbt/1.1/global.sbt`:

```scala
credentials += Credentials(Path.userHome / ".sbt" / "github.credentials")
```

Then, in `~/.sbt/github.credentials`, you can put your actual credentials:

```
realm=GitHub API
host=api.github.com
user=
password=25f94a2a5c7fbaf499c665bc73d67c1c87e496da
```

Note that the username is ignored, since OAuth authentication does not require a username.

If you are using GitHub enterprise, you will need to configure the endpoint that gets used to talk to GitHub. It's important to remember that the GitHub API is found under `/api/v3`. For example:

```scala
ThisBuild / prValidatorGithubEndpoint := uri("https://github.example.com/api/v3")
```

Ensure that you also update the `host` in your credentials file to match the hostname of your GitHub enterprise server.

### Travis push builds

Travis uses the same configuration for PR builds as push builds (ie, builds run on merge commits, tags, or commits pushed directly to a branch). The plugin will detect when a build is a non PR build, and run an enforced build all instead. As described above, the tasks run in this case can be controlled using `prValidatorEnforcedBuildAll`.
