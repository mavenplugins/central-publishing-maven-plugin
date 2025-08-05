/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.utils;

import java.util.Optional;

public interface PurlUtils
{

  Optional<String> toRepo1Url(String purl);
}
