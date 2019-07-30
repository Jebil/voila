package net.jk.app.commons.cucumber;

import lombok.Value;

/**
 * A basic set of properties that differentiates one services from another and allows to write BDDs
 * that integrate multiple micro-services as one app
 */
@Value
public class ApplicationInfo {

  /** The app URL prefix, e.g. "test" */
  String urlPrefix;

  int httpPort;
  int httpAdminPort;
  String host;

  // required for pinging the app at start up time
  String adminHealthCheckUrl;

  /** Returns full URL for HTTP operations */
  public String getHttpUrl() {
    return "http://" + host + ":" + httpPort;
  }

  public String getHttpAdminUrl() {
    return "http://" + host + ":" + httpAdminPort;
  }

  /** Convenicen method for test support */
  public String getHttpUrl(boolean admin) {
    return (admin) ? getHttpAdminUrl() : getHttpUrl();
  }
}
