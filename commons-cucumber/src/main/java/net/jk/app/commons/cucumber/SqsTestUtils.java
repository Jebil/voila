package net.jk.app.commons.cucumber;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import com.google.common.net.MediaType;
import java.util.*;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;

@Slf4j
public final class SqsTestUtils {

  private SqsTestUtils() {} // static utilities only

  /** The URL that SQS API calls are sent to */
  private static final String AWS_ENDPOINT_URL =
      new EnvVariable("AWS_SQS_ENDPOINT_URL", "http://localhost:9324").getValue();

  /** The region the SQS instance is running */
  private static final String AWS_ENDPOINT_REGION =
      new EnvVariable("AWS_SQS_ENDPOINT_REGION", "elasticmq").getValue();

  /** The AWS environment the SQS instance is running in */
  private static final String AWS_ENVIRONMENT =
      new EnvVariable("AWS_ENVIRONMENT", "local").getValue();

  /**
   * A string that should be replaced by the value of {@code AWS_ENDPOINT_URL} in all queue urls
   * returned by the SQS service. This is to resolve the situation where the SQS service returns a
   * url that isn't resolvable in the context of where tests are being run.
   *
   * <p>E.g. the SQS endpoint returns the hostname of a docker container but the tests are being ran
   * from outside the container's network and can't resolve the name
   *
   * <p>http://voila-elasticmq:9324 -> http://localhost:9324
   */
  private static final String AWS_ENDPOINT_REPLACE_URL =
      new EnvVariable("AWS_SQS_ENDPOINT_REPLACE_URL", "http://voila-elasticmq:9324").getValue();

  /** The single instance of the SQS client for test purposes */
  public static final AmazonSQS SQS = buildClient(AWS_ENDPOINT_URL, AWS_ENDPOINT_REGION);

  /** Builds a simple SQS client */
  private static AmazonSQS buildClient(String endpointUrl, String endpointRegion) {

    AmazonSQSClientBuilder.EndpointConfiguration endpointConfiguration =
        new AmazonSQSClientBuilder.EndpointConfiguration(endpointUrl, endpointRegion);

    AWSCredentialsProvider credentialsProvider =
        new AWSStaticCredentialsProvider(new AnonymousAWSCredentials());

    return AmazonSQSClientBuilder.standard()
        .withEndpointConfiguration(endpointConfiguration)
        .withCredentials(credentialsProvider)
        .build();
  }

  /** Delete all queues present in the system */
  public static void deleteAllQueues() {
    ListQueuesResult listQueuesResult = SQS.listQueues();
    List<String> queueUrls = listQueuesResult.getQueueUrls();

    // sort the queue names so that dead letter dependencies are deleted first
    Comparator<String> deadLetterFirst =
        (lhs, rhs) -> {
          int lhsVal = lhs.contains("dead.") ? 0 : 1;
          int rhsVal = rhs.contains("dead.") ? 0 : 1;

          return lhsVal - rhsVal;
        };

    queueUrls.stream().sorted(deadLetterFirst).forEach(q -> SQS.deleteQueue(q));
  }

  /** Creates a fifo queue and returns the url for which the queue is accessible */
  public static String createFifoQueue(String queueName) {

    Assert.assertThat(queueName, CoreMatchers.endsWith(".fifo"));

    queueName = calculateQueueName(queueName);

    // always create a corresponding dead letter queue
    String deadLetterName = "dead." + queueName;
    CreateQueueRequest deadRequest = new CreateQueueRequest(deadLetterName);
    deadRequest.addAttributesEntry(QueueAttributeName.FifoQueue.toString(), "true");
    String deadLetterUrl = SQS.createQueue(deadRequest).getQueueUrl();

    // create the queue
    CreateQueueRequest request = new CreateQueueRequest(queueName);
    request.addAttributesEntry(QueueAttributeName.FifoQueue.toString(), "true");
    request.addAttributesEntry(
        QueueAttributeName.RedrivePolicy.toString(), buildRedrivePolicyJson(deadLetterName));

    CreateQueueResult result = SQS.createQueue(request);
    return result.getQueueUrl();
  }

  /** Creates a standard queue and returns the url for which the queue is accessible */
  public static String createStandardQueue(String queueName) {

    queueName = calculateQueueName(queueName);

    // always create a corresponding dead letter queue
    String deadLetterName = "dead." + queueName;
    String deadLetterUrl = SQS.createQueue(deadLetterName).getQueueUrl();

    CreateQueueRequest request = new CreateQueueRequest(queueName);
    request.addAttributesEntry(
        QueueAttributeName.RedrivePolicy.toString(), buildRedrivePolicyJson(deadLetterName));
    CreateQueueResult result = SQS.createQueue(request);
    return result.getQueueUrl();
  }

  /** Build the json for specifying the dead letter queue */
  public static String buildRedrivePolicyJson(String deadLetterQueueName) {
    return String.format(
        "{ \"maxReceiveCount\" : \"1\", \"deadLetterTargetArn\" : \"%s\" }", deadLetterQueueName);
  }

  /** Creates a queue and returns the url for which the queue is accessible */
  public static void sendMessage(
      String queueName, @Nullable String groupId, String jsonBody, Map<String, String> headers) {

    queueName = calculateQueueName(queueName);

    String queueUrl = getTransformedQueueUrl(queueName);
    SendMessageRequest request = new SendMessageRequest(queueUrl, jsonBody);
    if (groupId != null) {
      request.setMessageGroupId(groupId);
      request.setMessageDeduplicationId(UUID.randomUUID().toString());
    }
    MessageAttributeValue typeValue = new MessageAttributeValue();
    typeValue.setDataType("String");
    typeValue.withStringValue(MediaType.JSON_UTF_8.toString());
    request.addMessageAttributesEntry("contentType", typeValue);

    // add any custom headers
    headers
        .entrySet()
        .forEach(
            es -> {
              MessageAttributeValue attr = new MessageAttributeValue();
              attr.setDataType("String");
              attr.withStringValue(es.getValue());
              request.addMessageAttributesEntry(es.getKey(), attr);
            });

    SQS.sendMessage(request);
    log.info("Placed message into queue {} with group id {}", queueName, groupId);
  }

  /** Purge a single queue of all messages */
  public static void purgeQueue(String queueName) {
    String queueUrl = getTransformedQueueUrl(queueName);
    PurgeQueueRequest request = new PurgeQueueRequest(queueUrl);
    SQS.purgeQueue(request);
  }

  /** Purge a every queue of all messages */
  public static void purgeAllQueues() {
    List<String> queueUrls = getAllTransformedQueueUrls();

    queueUrls
        .stream()
        .map(qn -> qn.substring(qn.lastIndexOf('/') + 1))
        .forEach(SqsTestUtils::purgeQueue);
  }

  /** Return the number of messages remaining in all the queues */
  public static Map<String, Integer> getMessageCountsOfActiveQueues() {
    return getAllTransformedQueueUrls()
        .parallelStream()
        // ignore dead letter queues
        .filter(qurl -> !qurl.contains("dead"))
        .map(SqsTestUtils::getPendingMessageCount)
        .collect(
            Collectors.toMap(
                Pair::getLeft, Pair::getRight, (p1, p2) -> p2, ConcurrentHashMap::new));
  }

  /** Get the addressable queue url for the specified queue in the test context */
  public static String getTransformedQueueUrl(String queueName) {
    return transformQueueUrl(SQS.getQueueUrl(queueName).getQueueUrl());
  }

  /** Get the list of urls for all the queues in the SQS instance */
  public static List<String> getAllTransformedQueueUrls() {
    ListQueuesResult listQueuesResult = SQS.listQueues();
    return listQueuesResult
        .getQueueUrls()
        .stream()
        .map(SqsTestUtils::transformQueueUrl)
        .collect(Collectors.toList());
  }

  public static List<SqsTestMessage> getPendingMessages(String queueName) {
    queueName = calculateQueueName(queueName);
    String queueUrl = getTransformedQueueUrl(queueName);

    ReceiveMessageRequest req = new ReceiveMessageRequest(queueUrl);
    req.setMaxNumberOfMessages(10);

    return SQS.receiveMessage(req)
        .getMessages()
        .stream()
        .map(m -> new SqsTestMessage(JsonTestUtils.parseMap(m.getBody()), m.getAttributes()))
        .collect(Collectors.toList());
  }

  public static int getPendingMessageCountForQueueName(String queueName) {
    queueName = calculateQueueName(queueName);
    String queueUrl = getTransformedQueueUrl(queueName);

    return getPendingMessageCount(queueUrl).getValue();
  }

  private static String calculateQueueName(String queueName) {
    if (queueName.endsWith(".fifo")) {
      // remove .fifo suffix
      return queueName.substring(0, queueName.length() - 5) + "-" + AWS_ENVIRONMENT + ".fifo";
    } else {
      return queueName + "-" + AWS_ENVIRONMENT;
    }
  }

  private static Pair<String, Integer> getPendingMessageCount(String queueUrl) {
    GetQueueAttributesResult result =
        SQS.getQueueAttributes(
            queueUrl,
            Arrays.asList(
                QueueAttributeName.ApproximateNumberOfMessages.toString(),
                QueueAttributeName.ApproximateNumberOfMessagesDelayed.toString(),
                QueueAttributeName.ApproximateNumberOfMessagesNotVisible.toString()));

    int numMessages = result.getAttributes().values().stream().mapToInt(Integer::parseInt).sum();

    return Pair.of(queueUrl, numMessages);
  }

  private static String transformQueueUrl(String url) {
    if (AWS_ENDPOINT_REPLACE_URL != null) {
      url = url.replace(AWS_ENDPOINT_REPLACE_URL, AWS_ENDPOINT_URL);
    }

    return url;
  }

  @Value
  public static class SqsTestMessage {
    Map<String, Object> payload;
    Map<String, String> headers;
  }
}
