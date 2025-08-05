/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin;

public class Constants
{
  public static final String DEFAULT_BUILD_DIR_NAME = "target";

  public static final String DEFAULT_STAGING_DIR_NAME = "central-staging";

  public static final String LOCAL_STAGING_REPOSITORY_NAME = "central-staging";

  public static final String CLI_EXECUTION_ID = "default-cli";

  public static final String POM_FILE_EXTENSION = "pom";

  public static final String DEFAULT_BUNDLE_OUTPUT_DIR_NAME = "central-publishing";

  public static final String DEFAULT_BUNDLE_OUTPUT_FILENAME = "central-bundle.zip";

  public static final String DEFAULT_DEPLOYMENT_NAME = "Deployment";

  public static final String PUBLISH_GOAL = "publish";

  public static final String DEPLOY_PHASE = "deploy";

  public static final String PUBLISH_GOAL_ID = "injected-central-publishing";

  public static final String CENTRAL_PUBLISHING_PLUGIN_GROUP_ID = "org.sonatype.central";

  public static final String CENTRAL_PUBLISHING_PLUGIN_ARTIFACT_ID = "central-publishing-maven-plugin";

  public static final String NEXUS_STAGING_PLUGIN_GROUP_ID = "org.sonatype.plugins";

  public static final String NEXUS_STAGING_PLUGIN_ARTIFACT_ID = "nexus-staging-maven-plugin";

  public static final String MAVEN_DEPLOY_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

  public static final String MAVEN_DEPLOY_PLUGIN_ARTIFACT_ID = "maven-deploy-plugin";
}
