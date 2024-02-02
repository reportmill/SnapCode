package snapcode.util;
import javakit.resolver.JavaClass;
import javakit.resolver.JavaDecl;
import javakit.resolver.JavaMember;
import snapcode.project.JavaData;
import snapcode.webbrowser.TextPage;
import snap.web.WebFile;
import java.util.Arrays;
import java.util.Set;

/**
 * A WebPage subclass to show class info for a .class file.
 */
public class ClassInfoPage extends TextPage {

    /**
     * Override to return class info text instead of class file contents.
     */
    protected String getDefaultText()
    {
        WebFile classFile = getFile();
        String javaFilePath = classFile.getPath().replace(".class", ".java").replace("/bin/", "/src/");
        WebFile javaFile = classFile.getSite().getFileForPath(javaFilePath);
        JavaData javaData = javaFile != null ? JavaData.getJavaDataForFile(javaFile) : null;
        if (javaData == null)
            return "Class File not found";

        // Get decls and refs
        Set<JavaDecl> decls = javaData.getDecls();
        Set<JavaDecl> refs = javaData.getRefs();

        // Create StringBuffer and append Declarations
        StringBuilder sb = new StringBuilder();
        sb.append("\n    - - - - - - - - - - Declarations - - - - - - - - - -\n\n");
        JavaDecl[] declArray = decls.toArray(new JavaDecl[0]);
        Arrays.sort(declArray);

        // Iterate over decls
        for (JavaDecl decl : declArray) {

            // Print class
            if (decl instanceof JavaClass) {
                sb.append("Class ").append(decl.getDeclarationString()).append('\n');

                // Iterate over decls
                for (JavaDecl d2 : declArray) {

                    // Print Members
                    if (d2 instanceof JavaMember) {
                        JavaMember member = (JavaMember) d2;
                        if (member.getDeclaringClass() == decl)
                            sb.append("    ").append(d2.getType()).append(' ').append(d2.getDeclarationString()).append('\n');
                    }
                }
                sb.append('\n');
            }
        }

        // Append References
        sb.append("\n    - - - - - - - - - - References - - - - - - - - - -\n\n");
        JavaDecl[] refArray = refs.toArray(new JavaDecl[0]);
        Arrays.sort(refArray);

        // Iterate over refs
        for (JavaDecl ref : refArray)
            sb.append(ref.getType()).append(' ').append(ref.getDeclarationString()).append('\n');

        // Set Text
        return sb.toString();
    }

}