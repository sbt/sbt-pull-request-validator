import sbt._

import de.heikoseeberger.sbtheader.HeaderPlugin
import de.heikoseeberger.sbtheader.HeaderPlugin.{ autoImport => SbtHeaderKeys }

object Header extends AutoPlugin {
  override lazy val requires = plugins.JvmPlugin && HeaderPlugin
  override lazy val trigger = allRequirements
  import SbtHeaderKeys.{ HeaderFileType, HeaderCommentStyle, HeaderLicense }

  override lazy val projectSettings = Seq(
    SbtHeaderKeys.headerMappings ++= Map(
      HeaderFileType.scala -> HeaderCommentStyle.cStyleBlockComment,
      HeaderFileType.java -> HeaderCommentStyle.cStyleBlockComment
    ),
    SbtHeaderKeys.headerLicense := Some(
      HeaderLicense.Custom(
        """|Sbt Pull Request Validator
           |Copyright Lightbend and Hewlett Packard Enterprise
           |
           |Licensed under Apache License 2.0
           |SPDX-License-Identifier: Apache-2.0
           |
           |See the NOTICE file distributed with this work for
           |additional information regarding copyright ownership.
           |""".stripMargin
      )
    )
  )
}
