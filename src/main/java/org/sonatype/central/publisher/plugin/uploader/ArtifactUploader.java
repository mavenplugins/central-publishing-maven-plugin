/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.uploader;

import org.sonatype.central.publisher.plugin.model.UploadArtifactRequest;

public interface ArtifactUploader
{
  String upload(final UploadArtifactRequest uploadArtifactRequest);
}
