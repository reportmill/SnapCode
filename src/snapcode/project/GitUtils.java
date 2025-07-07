package snapcode.project;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import snap.util.TaskMonitor;
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
     * Returns a ProgressMonitor for given TaskMonitor.
     */
    public static ProgressMonitor getProgressMonitor(final TaskMonitor aTM)
    {
        return new ProgressMonitor() {
            public void start(int arg0)  { aTM.startForTaskCount(arg0); }
            public void beginTask(String arg0, int arg1)  { aTM.beginTask(arg0, arg1); }
            public void update(int arg0)  { aTM.updateTask(arg0); }
            public void endTask()  { aTM.endTask(); }
            public void showDuration(boolean b)  { }
            public boolean isCancelled()  { return aTM.isCancelled(); }
        };
    }
}
