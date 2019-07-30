package net.jk.app.commons.cucumber;

import static net.jk.app.commons.cucumber.CucumberTestUtils.waitForAppToStart;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/** Similar to env.rb in the Ruby version of Cucumber */
@Slf4j
public class CucumberEnvironment {

  private static final ConcurrentMap<String, ApplicationInfo> applications =
      new ConcurrentHashMap<>();

  private static final ConcurrentMap<String, Connection> connections = new ConcurrentHashMap<>();

  /** Returns a read only view of the application configurations */
  public static Map<String, ApplicationInfo> getApplications() {
    return Collections.unmodifiableMap(applications);
  }

  /** Returns the app info associated with an app's url prefix */
  public static ApplicationInfo getApp(@NonNull String urlPrefix) {
    ApplicationInfo app = applications.get(urlPrefix);
    if (app == null) {
      throw new IllegalArgumentException("No application found for url prefix: " + urlPrefix);
    } else {
      return app;
    }
  }

  /** Registers app info by URL prefix */
  public static void registerApp(ApplicationInfo app) {
    applications.put(app.getUrlPrefix(), app);
  }

  public static Set<ApplicationInfo> getAllApps() {
    return new HashSet<>(applications.values());
  }

  /** Figures out the path from the full URL, by extracting the prefix */
  public static ApplicationInfo getAppForPath(@NonNull String path) {
    String parts[] = StringUtils.split(path, "/");
    if (parts.length > 0) {
      return getApp(parts[0]);
    } else {
      throw new IllegalArgumentException("No url prefix found in : " + path);
    }
  }

  /** Called before BDD test run starts */
  public static void setupTestRun() {
    // wait for every registered app to start up
    getAllApps().parallelStream().forEach(app -> waitForAppToStart(app));
  }

  /** Registers a DB connection for SQL test statements */
  public static void registerDatabaseConnection(String dbId, Connection connection) {
    connections.put(dbId, connection);
  }

  public static Connection getDatabaseConnection(String dbId) {
    Connection connection = connections.get(dbId);
    if (connection == null) {
      throw new RuntimeException("No DB connection found for id " + dbId);
    } else {
      return connection;
    }
  }
}
