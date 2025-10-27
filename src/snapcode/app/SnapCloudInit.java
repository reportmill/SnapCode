package snapcode.app;

/**
 * This class is used to initialize SnapCloud.
 */
public class SnapCloudInit {

    /**
     * Initialize SnapCloud.
     */
    public static void initSnapCloud()
    {
        String endPoint = "";
        String accessKey = "";
        String secretKey = "";

        // Set the system properties for the AWS SDK client that the NIO provider uses
        System.setProperty("aws.accessKeyId", accessKey);
        System.setProperty("aws.secretAccessKey", secretKey);
        System.setProperty("aws.endpointUrl", endPoint);
        System.setProperty("aws.region", "auto");
    }
}
