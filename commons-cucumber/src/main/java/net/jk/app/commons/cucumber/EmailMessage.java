package net.jk.app.commons.cucumber;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

@Data
@Builder
public class EmailMessage {

  private String textContent;

  private String htmlContent;

  private String subject;

  private String from;

  private List<String> to;

  private List<String> cc;

  private List<String> bcc;

  @Singular private List<InlineContent> inlineContents;

  @Singular private List<Attachment> attachments;

  @Value
  public static class InlineContent {
    private String contentId;
    private String contentType;
  }

  @Value
  public static class Attachment {
    private String fileName;
    private String contentType;
  }
}
