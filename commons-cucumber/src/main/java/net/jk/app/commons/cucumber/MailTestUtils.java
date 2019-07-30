package net.jk.app.commons.cucumber;

import static java.util.stream.Collectors.toList;
import static javax.mail.Message.RecipientType.BCC;
import static javax.mail.Message.RecipientType.CC;
import static javax.mail.Message.RecipientType.TO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.ServerSetupTest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.*;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Common methods for retrieving and processing email */
@Slf4j
public class MailTestUtils {

  private static final String HEADER_CONTENT_ID = "Content-ID";
  private static final ContentType TEXT_HTML = new ContentType("text", "html", null);
  private static final ContentType TEXT_PLAIN = new ContentType("text", "plain", null);

  /** Clear Inbox of the provided emailId. */
  @SuppressFBWarnings
  public static void clearMailBox(String emailId) {
    try {
      Folder folder = getInboxFolder(emailId);
      if (folder == null) {
        return;
      }
      Message[] messages = folder.getMessages();
      for (Message message : messages) {
        message.setFlag(Flags.Flag.DELETED, true);
      }
      folder.close(true);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static String extractEmailWithHtml(String jsonBody) throws IOException, JSONException {
    List<Object> list = JsonTestUtils.parseList(jsonBody);
    JSONArray jsonArray = new JSONArray();
    for (Object jsonMap : list) {
      jsonArray.put(readHtmlContentFromFile((Map<String, Object>) jsonMap));
    }
    return jsonArray.toString();
  }

  /** Get the inbox messages of specified emailId in JSON format */
  public static String getMessagesAsJSON(String emailId, ObjectMapper json) throws IOException {
    Map<Integer, EmailMessage> map = new HashMap<>();
    Message[] messages = getInboxMessages(emailId);
    log.info("Found {} messages in the inbox for email {}", messages.length, emailId);

    for (Message message : messages) {
      EmailMessage emailMessage = parseEmailMessage(message);
      map.put(message.getMessageNumber(), emailMessage);
    }
    return json.writeValueAsString(map.values());
  }

  /**
   * Retrieve the html content of the first email message in the inbox of the user with the
   * specified email address
   */
  public static Optional<String> getHtmlContentOfFirstInboxMessage(String emailAddress) {
    return getFirstInboxMessage(emailAddress)
        .map(MailTestUtils::parseEmailMessage)
        .map(EmailMessage::getHtmlContent)
        .filter(StringUtils::isNotEmpty);
  }

  @SuppressFBWarnings("EXS")
  private static EmailMessage parseEmailMessage(Message message) {
    try {
      EmailMessage.EmailMessageBuilder builder =
          EmailMessage.builder()
              .subject(message.getSubject())
              .from(getAddressList(message.getFrom()).stream().findFirst().get())
              .bcc(getAddressList(message.getRecipients(BCC)))
              .cc(getAddressList(message.getRecipients(CC)))
              .to(getAddressList(message.getRecipients(TO)));
      ContentType contentType = new ContentType(message.getContentType());
      if (contentType.match(TEXT_HTML) || contentType.match(TEXT_PLAIN)) {
        if (contentType.match(TEXT_HTML)) {
          builder.htmlContent(message.getContent().toString());
        } else {
          builder.textContent(message.getContent().toString());
        }
      } else if (message.getContent() instanceof MimeMultipart) {
        handleMultiPartMessage(builder, (MimeMultipart) message.getContent());
      }
      return builder.build();
    } catch (MessagingException | IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Replace file name with file content */
  @SuppressWarnings("unchecked")
  private static JSONObject readHtmlContentFromFile(Map<String, Object> jsonMap)
      throws IOException, JSONException {
    if (jsonMap.get("htmlContent") != null) {
      String htmlContent =
          CucumberTestUtils.readResourceFile(jsonMap.get("htmlContent").toString());
      String html = htmlContent.replaceAll("(\r\n|\n)", "\r\n");
      jsonMap.put(
          "htmlContent", resolvePlaceHolders((Map<String, String>) jsonMap.get("params"), html));
      jsonMap.remove("params");
    }
    return new JSONObject(jsonMap);
  }

  private static List<String> getAddressList(Address[] addresses) {
    if (addresses != null) {
      return Arrays.stream(addresses).map(Object::toString).collect(toList());
    }
    return Collections.emptyList();
  }

  /** Set EmailMessage content according to the provided mail's content type. */
  private static void handleMultiPartMessage(
      EmailMessage.EmailMessageBuilder messageBuilder, MimeMultipart multipartMail)
      throws MessagingException, IOException {
    ContentType contentType;
    BodyPart bodyPart;
    for (int i = 0; i < multipartMail.getCount(); i++) {
      bodyPart = multipartMail.getBodyPart(i);
      if (bodyPart.getDisposition() == null) {
        contentType = new ContentType(bodyPart.getContentType());
        if (contentType.match(TEXT_PLAIN)) {
          messageBuilder.textContent(bodyPart.getContent().toString());
        } else if (contentType.match(TEXT_HTML)) {
          messageBuilder.htmlContent(bodyPart.getContent().toString());
        } else if (bodyPart.getContent() instanceof MimeMultipart) {
          MimeMultipart root = (MimeMultipart) bodyPart.getContent();
          handleMultiPartMessage(messageBuilder, root);
        }
      } else if (MimeBodyPart.INLINE.equals(bodyPart.getDisposition())) {
        String[] contentIdArr = bodyPart.getHeader(HEADER_CONTENT_ID);
        if (contentIdArr != null) {
          messageBuilder.inlineContent(
              new EmailMessage.InlineContent(contentIdArr[0], bodyPart.getContentType()));
        }
      } else if (MimeBodyPart.ATTACHMENT.equals(bodyPart.getDisposition())) {
        messageBuilder.attachment(
            new EmailMessage.Attachment(
                MimeUtility.decodeText(bodyPart.getFileName()),
                new ContentType(bodyPart.getContentType()).toString()));
      }
    }
  }

  /** Get the Inbox folder of the specified emailId with READ and WRITE permission. */
  private static Folder getInboxFolder(String emailId) throws MessagingException {
    Properties props = new Properties();
    props.setProperty("mail.pop3.connectiontimeout", "5000");
    Session session = Session.getInstance(props);
    URLName urlName =
        new URLName("pop3", "0.0.0.0", ServerSetupTest.POP3.getPort(), null, emailId, emailId);
    Store store = session.getStore(urlName);
    try {
      store.connect();
    } catch (AuthenticationFailedException e) {
      return null;
    }
    Folder folder = store.getFolder("INBOX");
    folder.open(Folder.READ_WRITE);
    return folder;
  }

  @SuppressFBWarnings
  private static Message[] getInboxMessages(String email) {
    try {
      Folder folder = MailTestUtils.getInboxFolder(email);
      if (folder != null) {
        return folder.getMessages();
      } else {
        return new Message[0];
      }
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  private static Optional<Message> getFirstInboxMessage(String email) {
    return Arrays.stream(getInboxMessages(email)).findFirst();
  }

  /** Replace the placeholders in html with actual value */
  private static String resolvePlaceHolders(Map<String, String> placeholderMap, String html)
      throws JSONException {
    for (String key : placeholderMap.keySet()) {
      html = html.replace(key, placeholderMap.get(key));
    }
    return html;
  }
}
