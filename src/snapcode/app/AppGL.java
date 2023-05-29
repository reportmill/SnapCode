package snapcode.app;
import snapgl.JGLRenderer;

/**
 * Main App class for SnapCode.
 */
public class AppGL {

    /**
     * Standard main implementation.
     */
    public static void main(String[] args)
    {
        JGLRenderer.registerFactory();

        App.main(args);
    }
}
