package snapcode.util;
import javakit.resolver.*;
import snapcode.project.ClassFileUtils;
import snapcode.project.Project;
import snapcode.webbrowser.TextPage;
import snap.web.WebFile;
import java.util.Arrays;

/**
 * A WebPage subclass to show class info for a .class file.
 */
public class ClassInfoPage extends TextPage {

    /**
     * Override to return class info text instead of class file contents.
     */
    protected String getDefaultText()
    {
        // Get JavaClass for class file
        WebFile classFile = getFile();
        Project project = Project.getProjectForFile(classFile);
        JavaClass javaClass = project.getJavaClassForFile(classFile);
        if (javaClass == null)
            return "Java class not found for class file: " + classFile.getPath();

        // Create StringBuffer and append Declarations
        StringBuilder sb = new StringBuilder();
        sb.append("\n    - - - - - - - - - - Declarations - - - - - - - - - -\n\n");
        appendClassDecl(sb, javaClass);

        // Get external references
        JavaDecl[] externalReferences = ClassFileUtils.getExternalReferencesForClassFile(classFile);
        Arrays.sort(externalReferences);

        // Append References
        sb.append("\n    - - - - - - - - - - " + externalReferences.length + " References - - - - - - - - - -\n\n");
        for (JavaDecl ref : externalReferences)
            sb.append(ref.getType()).append(' ').append(ref.getDeclarationString()).append('\n');

        // Set Text
        return sb.toString();
    }

    /**
     * Appends a class declaration.
     */
    private static void appendClassDecl(StringBuilder sb, JavaClass javaClass)
    {
        // Append class string
        sb.append("Class ").append(javaClass.getDeclarationString()).append('\n');

        // Append Fields, Constructors, Methods
        for (JavaField field : javaClass.getDeclaredFields())
            appendMemberDecl(sb, field);
        for (JavaConstructor constr : javaClass.getDeclaredConstructors())
            appendMemberDecl(sb, constr);
        for (JavaMethod method : javaClass.getDeclaredMethods())
            appendMemberDecl(sb, method);

        // Append inner classes
        for (JavaClass innerClass : javaClass.getDeclaredClasses())
            appendClassDecl(sb, innerClass);

        sb.append('\n');
    }

    /**
     * Appends a member declaration.
     */
    private static void appendMemberDecl(StringBuilder sb, JavaMember aMember)
    {
        sb.append("    ").append(aMember.getType()).append(' ').append(aMember.getDeclarationString()).append('\n');
    }
}