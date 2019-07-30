package net.jk.app.commons.cucumber;

import com.amazonaws.util.IOUtils;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Scanner;
import javax.ws.rs.core.MediaType;
import junit.framework.AssertionFailedError;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/** Utilities to support Cucumber BDD integration tests */
@Slf4j
public class CucumberTestUtils {

  /**
   * Waits up to 30 seconds for app to start by probing the admin health check API until it starts
   * up
   */
  public static void waitForAppToStart(@NonNull ApplicationInfo app) {

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime timeout = now.plusSeconds(180);

    boolean waitingForAppToStart = true;

    while (waitingForAppToStart && LocalDateTime.now().isBefore(timeout)) {

      try {

        // test admin health check & main HTTP port together
        // both have to be up and responding to HTTP requests
        HttpResponse adminResponse =
            HttpTestUtils.get(
                app, true, app.getAdminHealthCheckUrl(), Maps.newHashMap(), Maps.newHashMap());

        if (adminResponse.getStatus() == 200 || adminResponse.getStatus() == 403) {
          log.debug("Server application {} has started (admin port)", app.getUrlPrefix());

          // verify main HTTP port is up as well and at least responds with a 200 or 404
          HttpResponse response =
              HttpTestUtils.get(app, false, "/", Maps.newHashMap(), Maps.newHashMap());
          if (response.getStatus() == 200 || response.getStatus() == 404) {
            log.info("Server application {} has started (all ports)", app.getUrlPrefix());
            waitingForAppToStart = false;
          }

        } else {
          throw new RuntimeException(
              "Healthcheck returned unexpected status: "
                  + adminResponse.getStatus()
                  + "\n"
                  + adminResponse.toString());
        }

      } catch (Exception e) {

        try {
          // print to console on build server
          log.info("Waiting for server application {} to start...", app.getUrlPrefix());
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
      }
    }

    // either we were succesful or app failed to start
    if (waitingForAppToStart) {
      log.error("Server application {} failed to start!", app.getUrlPrefix());
      throw new RuntimeException("Server application failed to start!");
    }
  }

  /**
   * Runs the actuator endpoint to re-create BASE data in the system after it has been nuked to
   * restore it to a clean install state
   */
  public static void recreateBaseData(ApplicationInfo app) {
    recreateData(app, "/actuator/base-data");
  }

  /**
   * Runs the actuator endpoint to re-create DEMO data in the system after it has been nuked to
   * restore it to a clean install state
   */
  public static void recreateDemoData(ApplicationInfo app) {
    recreateData(app, "/actuator/demo-data");
  }

  /** Read the contents of a file in the resources directory */
  @SuppressFBWarnings("EXS")
  public static String readResourceFile(String resourcePath) {
    return readResourceFile(resourcePath, StandardCharsets.UTF_8);
  }

  /** Read the contents of a file in the resources directory */
  @SuppressFBWarnings("EXS")
  public static String readResourceFile(String resourcePath, Charset charset) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try (InputStream resourceStream = classLoader.getResourceAsStream(resourcePath);
        Scanner scanner = new Scanner(resourceStream, charset.name())) {

      return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void recreateData(ApplicationInfo app, String url) {

    HttpResponse response =
        HttpTestUtils.post(
            app,
            true,
            url,
            "",
            Maps.newHashMap(),
            Maps.newHashMap(),
            MediaType.APPLICATION_JSON_TYPE);

    int statusCode = response.getStatus();
    if (statusCode != 204) {
      log.error("Failed to re-create base data: {}", response);
      throw new AssertionFailedError("Failed to re-create data via URL " + url);
    }
  }

  @SuppressFBWarnings("EXS")
  public static byte[] readFileAsBytes(String fileName) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try (InputStream resourceStream = classLoader.getResourceAsStream(fileName)) {
      return IOUtils.toByteArray(resourceStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
