/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.client.model;

import java.util.List;
import java.util.Map;

public class DeploymentApiResponse
{
  private String deploymentId;

  private String deploymentName;

  private DeploymentState deploymentState;

  private List<String> purls;

  private Map<String, List<String>> errors;

  private String cherryBomUrl;

  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(final String deploymentId) {
    this.deploymentId = deploymentId;
  }

  public String getDeploymentName() {
    return deploymentName;
  }

  public void setDeploymentName(final String deploymentName) {
    this.deploymentName = deploymentName;
  }

  public DeploymentState getDeploymentState() {
    return deploymentState;
  }

  public void setDeploymentState(final DeploymentState deploymentState) {
    this.deploymentState = deploymentState;
  }

  public List<String> getPurls() {
    return purls;
  }

  public void setPurls(final List<String> purls) {
    this.purls = purls;
  }

  public Map<String, List<String>> getErrors() {
    return errors;
  }

  public void setErrors(final Map<String, List<String>> errors) {
    this.errors = errors;
  }

  public String getCherryBomUrl() {
    return cherryBomUrl;
  }

  public void setCherryBomUrl(final String cherryBomUrl) {
    this.cherryBomUrl = cherryBomUrl;
  }
}
