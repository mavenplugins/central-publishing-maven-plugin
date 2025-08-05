/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.utils;

import java.util.Optional;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;

@Component(role = PurlUtils.class)
public class PurlUtilsImpl
    extends AbstractLogEnabled
    implements PurlUtils
{
  private static final String REPO1_URL = "https://repo1.maven.org/maven2/";

  @Override
  public Optional<String> toRepo1Url(final String purl) {
    try {
      PackageURL packageURL = new PackageURL(purl);
      if (null != packageURL.getNamespace() && null != packageURL.getName() && null != packageURL.getVersion()) {
        String url = REPO1_URL +
            packageURL.getNamespace().replaceAll("\\.", "/") + "/" +
            packageURL.getName() + "/" +
            packageURL.getVersion() + "/";
        return Optional.of(url);
      }
    }
    catch (MalformedPackageURLException e) {
      if (null != getLogger()) {
        getLogger().debug("Invalid package URL returned: " + purl, e);
      }
    }
    return Optional.empty();
  }
}
