package net.jk.app.commons.cucumber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/** Common methods for comparing JSON documents in BDDs */
@Slf4j
public class JsonTestUtils {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();

  // we use TreeMap for all the docs, so the keys are always predictably sorted for comparison
  private static final TypeReference<TreeMap<String, Object>> mapTypeReference =
      new TypeReference<TreeMap<String, Object>>() {};
  private static final TypeReference<ArrayList<Object>> listTypeReference =
      new TypeReference<ArrayList<Object>>() {};

  static {
    DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
    DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter();
    printer.indentObjectsWith(indenter); // Indent JSON objects
    printer.indentArraysWith(indenter); // Indent JSON arrays

    // customize JSON mapper
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  }

  /** Parses a JSON dict into a Java map */
  @SuppressFBWarnings("EXS")
  public static Map<String, Object> parseMap(String json) {
    try {
      return mapper.readValue(json, mapTypeReference);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Parses a JSON list into a Java list */
  @SuppressFBWarnings("EXS")
  public static List<Object> parseList(String json) {
    try {
      return mapper.readValue(json, listTypeReference);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Convers a list of objects to a JSON */
  @SuppressFBWarnings("EXS")
  public static String toJson(Object value) {
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /** Get the json at the specified path and log a more useful error if the path isn't present */
  public static <T> T getJsonAtPath(String json, String path) {
    try {
      return JsonPath.parse(json).read(path, new TypeRef<T>() {});
    } catch (PathNotFoundException ex) {
      log.info("PATH AT \"{}\" NOT FOUND IN JSON:\n{}", path, prettyPrintJson(json));
      throw ex;
    }
  }

  /** Get the json at the specified path and log a more useful error if the path isn't present */
  public static <T> T getJsonAtPath(String json, String path, Class<T> type) {
    try {
      return JsonPath.parse(json).read(path, type);
    } catch (PathNotFoundException ex) {
      log.info("PATH AT \"{}\" NOT FOUND IN JSON:\n{}", path, prettyPrintJson(json));
      throw ex;
    }
  }

  /**
   * Merge <code>jsonToBeMerged</code> to <code>fullJson</code>. Basically, add/override the keys
   * found in <code>jsonToBeMerged</code> to <code>fullJson</code>.
   */
  @SuppressFBWarnings("EXS")
  public static String mergeJson(String fullJson, String jsonToBeMerged) {
    try {
      JSONObject bodyJsonObject = new JSONObject(fullJson);
      JSONObject jsonObject = new JSONObject(jsonToBeMerged);
      Iterator<String> keys = jsonObject.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        bodyJsonObject.put(key, jsonObject.get(key));
      }
      return bodyJsonObject.toString();
    } catch (JSONException e) {
      log.info("Exception while replacing json", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the json at the specified path as a list and log a more useful error if the path isn't
   * present
   */
  @SuppressFBWarnings("EXS")
  public static <T> List<T> getJsonListAtPath(String json, String path) {
    try {
      return JsonPath.parse(json).read(path, new TypeRef<List<T>>() {});
    } catch (PathNotFoundException ex) {
      log.info("PATH AT \"{}\" NOT FOUND IN JSON:\n{}", path, prettyPrintJson(json));
      throw ex;
    } catch (MappingException ex) {
      log.info("PATH AT \"{}\" IS NOT A LIST. ACTUAL JSON:\n{}", path, prettyPrintJson(json));
      throw ex;
    }
  }

  /**
   * Sort the json at the specified path by the field at the sortPath
   *
   * @param json The raw json that should be ordered at a given path
   * @param path The path to be sorted
   * @param sortPath The path within each object at {@code path} that should be sorted
   * @param ascending Whether or not to sort in ascending order
   */
  public static String sortJsonAtPath(
      String json, String path, String sortPath, boolean ascending) {

    try {
      DocumentContext documentContext = JsonPath.parse(json);

      JsonNode rootNode = documentContext.read(path, JsonNode.class);

      if (!rootNode.isArray()) {
        throw new IllegalArgumentException("Json at " + path + " must be an array.");
      }

      // a comparator that compares each json node by the value at sortPath
      Comparator<JsonNode> nodeComparator =
          Comparator.comparing(
              (jn) -> {
                try {
                  String nodeJson = mapper.writeValueAsString(jn);
                  Object valueAtPath = getJsonAtPath(nodeJson, sortPath);

                  if (!(valueAtPath instanceof Comparable)) {
                    throw new IllegalArgumentException(
                        "Property at " + path + " must be comparable.");
                  }

                  return (Comparable<Object>) valueAtPath;
                } catch (IOException ex) {
                  throw new RuntimeException(ex);
                }
              });

      if (!ascending) {
        nodeComparator = nodeComparator.reversed();
      }

      List<JsonNode> orderedNodes =
          StreamSupport.stream(rootNode.spliterator(), false)
              .sorted(nodeComparator)
              .collect(Collectors.toList());

      // delete the first item and append to the end for each node
      for (JsonNode node : orderedNodes) {
        documentContext.delete(path + "[0]");
        documentContext.add(path, node);
      }

      return documentContext.jsonString();

    } catch (PathNotFoundException ex) {
      log.info("PATH AT \"{}\" NOT FOUND IN JSON:\n{}", path, prettyPrintJson(json));
      throw ex;
    }
  }

  /**
   * Compares JSON documents but only for the fields specified in the expected JSON document All the
   * newly added fields in actual JSON are ignored
   *
   * <p>That allows to add new fields to JSON for new functionality without breaking all existing
   * BDDs
   *
   * @param expected Expected JSON document
   * @param actual Actual JSON document
   */
  @SuppressFBWarnings("EXS")
  public static void assertEquivalentJson(String expected, String actual) {
    assertJson(expected, actual, JSONCompareMode.LENIENT);
  }

  /** Verifies two JSON docs are identica, including ALL fields */
  public static void assertEqualJson(String expected, String actual) {
    assertJson(expected, actual, JSONCompareMode.STRICT_ORDER);
  }

  /**
   * Asserts the size of a JSON array at a specified path within that json
   *
   * @param actualJson The full json to be tested
   * @param path The path within the json that contains the array
   * @param expectedSize The number of elements expected in the JSON array at the path
   */
  public static void assertArraySizeAtPath(String actualJson, String path, int expectedSize) {
    List<Object> actualAtPath = JsonTestUtils.getJsonListAtPath(actualJson, path);
    try {
      Assert.assertThat(actualAtPath.size(), CoreMatchers.is(expectedSize));
    } catch (AssertionError e) {
      log.info("ACTUAL FULL JSON AT PATH \"{}\":\n{}", path, prettyPrintValue(actualAtPath));
      throw e;
    }
  }

  /**
   * Asserts the size of a JSON array at a specified path within that JSON is at least some number
   * of elements
   *
   * @param actualJson The full json to be tested
   * @param path The path within the json that contains the array
   * @param minSize The minimum number of elements expected in the JSON array at the path
   */
  public static void assertArraySizeAtPathIsAtLeast(String actualJson, String path, int minSize) {
    List<Object> actualAtPath = JsonTestUtils.getJsonListAtPath(actualJson, path);
    int actualSize = actualAtPath.size();
    try {
      Assert.assertTrue(
          String.format(
              "Expected JSON at path %s should have at least %d elements. But received %d",
              path, minSize, actualSize),
          actualSize >= minSize);
    } catch (AssertionError e) {
      log.info("ACTUAL FULL JSON AT PATH \"{}\":\n{}", path, prettyPrintValue(actualAtPath));
      throw e;
    }
  }

  // parses list or map using different Jackson type reference
  @SuppressFBWarnings("EXS")
  private static Object readValue(@NonNull String json) {
    try {
      if (json.trim().startsWith("[")) {
        return mapper.readValue(json, listTypeReference);
      } else {
        return mapper.readValue(json, mapTypeReference);
      }
    } catch (Exception e) {
      log.error("Unable to parse:\n{}", json);
      throw new RuntimeException(e);
    }
  }

  /**
   * Compares JSONS and copies original JSON doc to clipboard so easy to paste into BDD while
   * developing
   */
  @SuppressFBWarnings("EXS")
  private static void assertJson(String expected, String actual, JSONCompareMode compareMode) {
    try {
      JSONAssert.assertEquals(expected, actual, compareMode);
    } catch (AssertionError e) {
      String actualPrettyJson = prettyPrintJson(actual);
      log.info("ACTUAL FULL JSON:\n{}", actualPrettyJson);
      throw e;
    } catch (JSONException e) {
      log.info("ACTUAL FULL JSON:\n{}", actual);
      throw new RuntimeException(e);
    }
  }

  @SuppressFBWarnings("EXS")
  private static String prettyPrintJson(String jsonBody) {
    try {
      return mapper.writer(prettyPrinter).writeValueAsString(readValue(jsonBody));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressFBWarnings("EXS")
  private static String prettyPrintValue(Object value) {
    try {
      return mapper.writer(prettyPrinter).writeValueAsString(value);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
