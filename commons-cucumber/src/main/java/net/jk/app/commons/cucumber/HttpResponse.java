package net.jk.app.commons.cucumber;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Toolkit-independent representation of an HTTP response */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HttpResponse {

  /** Creates instance from a Jersey response */
  public static HttpResponse from(Response jersey) {
    Multimap<String, String> headers = HashMultimap.create(jersey.getStringHeaders().size(), 1);
    jersey.getStringHeaders().entrySet().forEach(es -> headers.putAll(es.getKey(), es.getValue()));

    return new HttpResponse(jersey.getStatus(), jersey.readEntity(String.class), headers);
  }

  private int status;
  private String body;
  private Multimap<String, String> headers;
}
