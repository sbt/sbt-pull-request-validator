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

package com.hpe

package object sbt {
  @deprecated("com.github.sbt.pullrequestvalidator.ValidatePullRequest", "2.0.0")
  val ValidatePullRequest = com.github.sbt.pullrequestvalidator.ValidatePullRequest
}
