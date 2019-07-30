package net.jk.app.commons.cucumber;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class S3TestUtils {

  private S3TestUtils() {} // static utilities only

  /** The URL that S3 API calls are sent to */
  private static final String AWS_ENDPOINT_URL =
      new EnvVariable("AWS_S3_ENDPOINT_URL", "http://localhost:8001").getValue();

  /** The region the S3 instance is running */
  private static final String AWS_ENDPOINT_REGION =
      new EnvVariable("AWS_S3_ENDPOINT_REGION", "s3mock").getValue();

  /** The AWS environment the S3 instance is running in */
  private static final String AWS_ENVIRONMENT =
      new EnvVariable("AWS_ENVIRONMENT", "local").getValue();

  /**
   * A string that should be replaced by the value of {@code AWS_ENDPOINT_URL} in all S3 urls
   * returned by the S3 service. This is to resolve the situation where the S3 service returns a url
   * that isn't resolvable in the context of where tests are being run.
   *
   * <p>E.g. the S3 endpoint returns the hostname of a docker container but the tests are being ran
   * from outside the container's network and can't resolve the name
   *
   * <p>http://voila-s3mock:8001 -> http://localhost:8001
   */
  private static final String AWS_ENDPOINT_REPLACE_URL =
      new EnvVariable("AWS_S3_ENDPOINT_REPLACE_URL", "http://voila-s3mock:8001").getValue();

  /** The single instance of the S3 client for test purposes */
  public static final AmazonS3 S3 = buildClient(AWS_ENDPOINT_URL, AWS_ENDPOINT_REGION);

  /** Create an S3 bucket */
  public static void createBucket(String bucketName) {
    S3.createBucket(calculateBucketName(bucketName));
  }

  /** Put some text content into an S3 bucket with the specified key */
  public static void putTextInBucket(String bucketName, String key, String text) {
    S3.putObject(calculateBucketName(bucketName), key, text);
  }

  private static String calculateBucketName(String bucketName) {
    return bucketName + "-" + AWS_ENVIRONMENT;
  }

  /** Purge a single bucket */
  public static void deleteObject(S3ObjectSummary objectSummary) {
    try {
      S3.deleteObject(objectSummary.getBucketName(), objectSummary.getKey());
    } catch (AmazonS3Exception exception) {
      log.debug("Error while clearing S3", exception);
    }
  }

  /** Purge a single bucket */
  public static void purgeBucket(Bucket bucket) {
    S3.listObjects(bucket.getName()).getObjectSummaries().forEach(S3TestUtils::deleteObject);
  }

  /** Purge all S3 buckets */
  public static void purgeBuckets() {
    S3.listBuckets().parallelStream().forEach(S3TestUtils::purgeBucket);
  }

  /** Check whether or not an object exists in the s3 bucket */
  public static boolean doesObjectExist(String bucketName, String key) {
    String bucketWithEnv = calculateBucketName(bucketName);
    try {
      S3.getObjectMetadata(bucketWithEnv, key);
      return true;
    } catch (AmazonS3Exception ex) {
      if (ex.getStatusCode() == 404) {

        List<String> keysInBucket =
            S3.listObjects(bucketWithEnv)
                .getObjectSummaries()
                .parallelStream()
                .map(s -> s.getKey())
                .collect(Collectors.toList());

        log.debug(
            "Key {} does not exists in bucket {}. The bucket contains the following keys: {}",
            key,
            bucketWithEnv,
            keysInBucket.toString());

        return false;
      } else {
        throw ex;
      }
    }
  }

  /**
   * Replace the host in a signed url in the case the tests can't resolve the hostname i.e. the
   * tests are running locally and the mock s3 is running in a docker container
   */
  public static String getTransformedSignedUrl(String signedUrl) {
    if (AWS_ENDPOINT_REPLACE_URL != null) {
      signedUrl = signedUrl.replace(AWS_ENDPOINT_REPLACE_URL, AWS_ENDPOINT_URL);
    }
    return signedUrl;
  }

  /** Builds a simple S3 client */
  private static AmazonS3 buildClient(String endpointUrl, String endpointRegion) {

    return AmazonS3ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(endpointUrl, endpointRegion))
        .withPathStyleAccessEnabled(true)
        .build();
  }
}
