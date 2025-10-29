package snapcode.project;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import snap.util.ActivityMonitor;
import snapcode.apptools.AccountTool;

/**
 * Utilities for git.
 */
public class GitUtils {

    /**
     * Returns the git username.
     */
    public static String getGitUserName()  { return null; }

    /**
     * Returns the git password.
     */
    public static String getGitPassword()  { return null; }

    /**
     * Returns credentials provider.
     */
    public static CredentialsProvider getCredentialsProvider()
    {
        String userName = AccountTool.getGithubUser();
        String password = AccountTool.getGithubPac();
        if (userName == null || userName.isEmpty() || password == null || password.isEmpty())
            return null;

        // Return
        return new UsernamePasswordCredentialsProvider(userName, password);
    }

    /**
     * Returns a ProgressMonitor for given ActivityMonitor.
     */
    public static ProgressMonitor getProgressMonitor(ActivityMonitor activityMonitor)
    {
        return new ProgressMonitor() {
            public void start(int arg0)  { activityMonitor.startForTaskCount(arg0); }
            public void beginTask(String arg0, int arg1)  { activityMonitor.beginTask(arg0, arg1); }
            public void update(int arg0)  { activityMonitor.updateTask(arg0); }
            public void endTask()  { activityMonitor.endTask(); }
            public void showDuration(boolean b)  { }
            public boolean isCancelled()  { return activityMonitor.isCancelled(); }
        };
    }
}
