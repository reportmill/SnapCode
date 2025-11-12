package snapcode.apptools;
import snapcode.javatext.NodeMatcher;
import javakit.parse.JFile;
import javakit.parse.JNode;
import snapcode.project.JavaAgent;
import snapcode.project.Project;
import snap.view.TextArea;
import snap.view.ViewUtils;
import snap.web.WebFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides analysis information about a project.
 */
public class ProjectAnalysisTool {

    private JFile _symFile;
    private int _undefCount;

    /**
     * Constructor.
     */
    public ProjectAnalysisTool()
    {
        super();
    }

    /**
     * Returns the line of code text.
     */
    public static String getLinesOfCodeText(Project aProject)
    {
        // Declare loop variables
        StringBuilder sb = new StringBuilder("Lines of Code:\n\n");
        DecimalFormat fmt = new DecimalFormat("#,##0");
        int total = 0;

        // Get projects
        List<Project> projects = new ArrayList<>();
        projects.add(aProject);
        projects.addAll(aProject.getProjects());

        // Iterate over projects and add: ProjName: xxx
        for (Project prj : projects) {
            int loc = getLinesOfCode(prj.getSourceDir());
            total += loc;
            sb.append(prj.getName()).append(": ").append(fmt.format(loc)).append('\n');
        }

        // Add total and return string (trimmed)
        sb.append("\nTotal: ").append(fmt.format(total)).append('\n');
        return sb.toString().trim();
    }

    /**
     * Returns lines of code in a file (recursive).
     */
    private static int getLinesOfCode(WebFile aFile)
    {
        int linesOfCode = 0;

        // Handle Java or snp file
        if (aFile.isFile() && (aFile.getFileType().equals("java") || aFile.getFileType().equals("snp"))) {
            String text = aFile.getText();
            for (int i = text.indexOf('\n'); i >= 0; i = text.indexOf('\n', i + 1))
                linesOfCode++;
        }

        // Handle dir: recurse
        else if (aFile.isDir()) {
            for (WebFile child : aFile.getFiles())
                linesOfCode += getLinesOfCode(child);
        }

        // Return
        return linesOfCode;
    }

    /**
     * Loads the undefined symbols in file.
     */
    public void findUndefines(WebFile aFile, TextArea aTextArea)
    {
        // Handle Java file: Find undefines
        if (aFile.isFile() && aFile.getFileType().equals("java")) {
            JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(aFile);
            JNode jfile = javaAgent.getJFile();
            findUndefines(jfile, aTextArea);
        }

        // Handle dir: Recurse
        else if (aFile.isDir())
            for (WebFile child : aFile.getFiles())
                findUndefines(child, aTextArea);
    }

    /**
     * Loads the undefined symbols in file.
     */
    private void findUndefines(JNode aNode, TextArea aTextArea)
    {
        if (_undefCount > 49) return;

        if (aNode.getDecl() == null && NodeMatcher.isDeclExpected(aNode)) {
            aNode.getDecl();
            _undefCount++;

            // If first undefined for file, print header
            if (aNode.getFile() != _symFile) {
                _symFile = aNode.getFile();
                showSymText("\n" + aNode.getFile().getSourceFile().getName() + ":\n\n", aTextArea);
            }

            // Print header
            try { showSymText("    " + _undefCount + ". " + aNode + '\n', aTextArea); }
            catch (Exception e) { showSymText(e.toString(), aTextArea); }
        }

        // Recurse into node
        else if (aNode.getChildCount() > 0) {
            for (JNode child : aNode.getChildren())
                findUndefines(child, aTextArea);
        }
    }

    private void showSymText(String aStr, TextArea aTextArea)
    {
        ViewUtils.runLater(() -> aTextArea.getTextModel().addChars(aStr));

        // Sleep
        try { Thread.sleep(80); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
