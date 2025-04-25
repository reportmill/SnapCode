package snapcode.debug;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.text.TextBlock;
import snap.text.TextLineStyle;
import snap.text.TextLink;
import snap.text.TextStyle;
import snap.util.Convert;
import snapcode.apptools.RunTool;
import snapcode.project.JavaTextDocUtils;
import snapcode.project.Project;
import snapcode.project.ProjectUtils;

/**
 * A TextBlock to hold system console output.
 */
public class ConsoleText extends TextBlock {

    // The RunTool
    private RunTool _runTool;

    // Constants
    private static Color ERROR_COLOR = new Color("CC0000");
    private static Color BANNER_COLOR = new Color("#C08080");

    /**
     * Constructor.
     */
    public ConsoleText()
    {
        super(true);

        // Set font
        Font codeFont = JavaTextDocUtils.getDefaultJavaFont();
        setDefaultFont(codeFont);
        setDefaultLineStyle(getDefaultLineStyle().copyForPropKeyValue(TextLineStyle.Spacing_Prop, 2));
    }

    /**
     * Sets the RunTool.
     */
    public void setRunTool(RunTool runTool)  { _runTool = runTool; }

    /**
     * Appends given string to text with option for whether text is error.
     */
    public void appendString(String aStr, boolean isError)
    {
        // Handle overflow
        if (length() > 100000) return;

        // Get color
        Color color = isError ? ERROR_COLOR : Color.BLACK;
        if (length() == 0 && aStr.contains("java "))
            color = BANNER_COLOR;

        // Get default style modified for color
        TextStyle style = getTextStyleForCharIndex(length());
        if (!style.getColor().equals(color))
            style = style.copyForStyleValue(color);

        // Look for a StackFrame reference: " at java.pkg.Class(Class.java:55)" and add as link if found
        int start = 0;
        for (int i = aStr.indexOf(".java:"); i > 0; i = aStr.indexOf(".java:", start)) {

            // Get start/end of Java file name inside parens (if parens not found, just add chars and continue)
            int parenStartIndex = aStr.lastIndexOf("(", i);
            int parenEndIndex = aStr.indexOf(")", i);
            if (parenStartIndex < start || parenEndIndex < 0) {
                addCharsWithStyle(aStr.substring(start, start = i + 6), style);
                continue;
            }

            // Get chars before parens and add
            String prefix = aStr.substring(start, parenStartIndex + 1);
            addCharsWithStyle(prefix, style);

            // Get link text, link address, TextLink
            String linkText = aStr.substring(parenStartIndex + 1, parenEndIndex);
            String linkAddr = getLink(prefix, linkText);
            TextLink textLink = new TextLink(linkAddr);

            // Get TextStyle for link and add link chars
            TextStyle linkStyle = style.copyForStyleValue(textLink);
            addCharsWithStyle(linkText, linkStyle);

            // Update start to end of link text and continue
            start = parenEndIndex;
        }

        // Add remainder normally
        addCharsWithStyle(aStr.substring(start), style);
    }

    /**
     * Returns a link for a StackString.
     */
    protected String getLink(String aPrefix, String linkedText)
    {
        // Get start/end of full class path for .java
        int start = aPrefix.indexOf("at ");
        if (start < 0)
            return "/Unknown";
        start += 3;
        int end = aPrefix.indexOf('$');
        if (end < start)
            end = aPrefix.lastIndexOf('.');
        if (end < start)
            end = aPrefix.length() - 1;

        // Get class file path
        String classFilePath = aPrefix.substring(start, end);
        classFilePath = '/' + classFilePath.replace('.', '/') + ".java";

        // Get project (if null, return - shouldn't be possible anymore)
        Project rootProject = _runTool.getProject();
        if (rootProject == null) { System.err.println("ConsoleText.getLink: No Project"); return "/Unknown"; }

        // Get full path to source file in project (or full URL string if external source)
        String sourceCodeFilePath = ProjectUtils.getSourceCodeUrlForClassPath(rootProject, classFilePath);
        String lineStr = linkedText.substring(linkedText.indexOf(":") + 1);

        // Add line number
        int line = Convert.intValue(lineStr);
        if (line > 0)
            sourceCodeFilePath += "#LineNumber=" + line;

        // Return
        return sourceCodeFilePath;
    }
}
