/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.client.model;

public enum DeploymentState
{
  PENDING,
  VALIDATING,
  VALIDATED,
  PUBLISHING,
  PUBLISHED,
  FAILED
}
