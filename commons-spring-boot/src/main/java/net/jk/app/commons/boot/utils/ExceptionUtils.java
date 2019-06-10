package net.jk.app.commons.boot.utils;

import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import net.jk.app.commons.boot.ThreadLocals;

/** Helper methods for exceptions */
public class ExceptionUtils {

  /** simple string summary of multiple finder values */
  public static String getFinderSummary(@NonNull Map<String, Object> finderValues) {
    StringBuilder sb = ThreadLocals.STRINGBUILDER.get();
    // handle potentially multiple finder values
    finderValues
        .values()
        .stream()
        .filter(Objects::nonNull)
        .forEach(
            v -> {
              if (sb.length() > 0) {
                sb.append(" & ");
              }
              sb.append(v);
            });
    return sb.toString();
  }
}
