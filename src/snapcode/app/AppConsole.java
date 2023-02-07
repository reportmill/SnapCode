package snapcode.app;

import snap.util.StringUtils;
import snap.view.ScrollView;
import snap.view.View;
import snap.view.ViewOwner;
import snap.viewx.ConsoleView;
import snap.web.WebFile;
import snap.web.WebSite;

/**
 * A pane to show a console for app.
 */
public class AppConsole extends ViewOwner {

    // The AppPane
    AppPane _appPane;

    // The ConsoleView
    ConsoleView _consoleText;

    /**
     * Returns the AppPane.
     */
    public AppPane getAppPane()
    {
        return _appPane;
    }

    /**
     * Returns the site.
     */
    public WebSite getSite()
    {
        return getAppPane().getRootSite();
    }

    /**
     * Creates the UI.
     */
    protected View createUI()
    {
        _consoleText = new ConsoleText();
        _consoleText.setPrompt("prompt> ");
        ScrollView spane = new ScrollView();
        spane.setContent(_consoleText);
        return spane;
    }

    /**
     * A text area for console processing.
     */
    public class ConsoleText extends ConsoleView {

        /**
         * Executes command.
         */
        protected String executeCommandImpl(String aCommand)
        {
            // Remove semi-colon
            String cmd = StringUtils.delete(aCommand, ";");

            // Handle show tables
            //if(cmd.equalsIgnoreCase("show tables")) DataSiteUtils.showTables(DataSite.get(getSite()));

            // Handle get command
            if (cmd.startsWith("get ")) return executeGet(aCommand.substring(4));

            // Handle select command
            //if(cmd.startsWith("select ")) return DataSiteUtils.executeSelect(DataSite.get(getSite()), cmd.substring(7));

            // Otherwise, do default version
            return super.executeCommandImpl(cmd);
        }

        /**
         * Execute a help command.
         */
        public String executeHelp(String aCommand)
        {
            StringBuffer sb = new StringBuffer();
            sb.append("show tables\n");
            sb.append("get [ file name ]\n");
            sb.append("select [ property, ... ] from [ table_name ]\n");
            sb.append("print [ expression ]\n");
            sb.append("clear");
            return sb.toString();
        }

        /**
         * Execute get command.
         */
        public String executeGet(String aCommand)
        {
            WebFile file = getSite().getFileForPath("/" + aCommand);
            if (file != null)
                return StringUtils.getString(file.getBytes());
            return "File not found";
        }
    }

}