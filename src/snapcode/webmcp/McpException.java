package snapcode.webmcp;

/**
 * An exception thrown by tool handlers to report a user-level error to the calling agent.
 */
public class McpException extends RuntimeException {

    public McpException(String aMessage)
    {
        super(aMessage);
    }
}
