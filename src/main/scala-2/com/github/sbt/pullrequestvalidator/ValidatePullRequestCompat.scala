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

import sbt.Credentials
import sbt.librarymanagement.ivy.DirectCredentials

private[pullrequestvalidator] object ValidatePullRequestCompat {
  def credentialsForHost(sc: Seq[Credentials], host: String): Option[DirectCredentials] =
    sbt.Credentials.forHost(sc, host)

  def testFull = sbt.Keys.test
}
