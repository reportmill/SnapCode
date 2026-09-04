package javakit.parse;
import javakit.resolver.*;
import java.util.List;
import java.util.Objects;

/**
 * Utility methods to resolve a type to declaration.
 */
class ResolveDeclForChildType {

    /**
     * Returns the JavaDecl most closely associated with given child JType node.
     */
    public static JavaType getJavaTypeForChildType(JType childType)
    {
        String typeName = childType.getName();

        for (JNode parentNode = childType.getParent(); parentNode != null; parentNode = parentNode.getParent()) {

            switch (parentNode) {

                // Handle JFile: See if type is found in imports
                case JFile jfile -> {
                    String className = jfile.getImportClassName(typeName);
                    JavaClass javaClass = className != null ? jfile.getJavaClassForName(className) : null;
                    if (javaClass != null)
                        return javaClass;
                }

                // Handle class decl: Check type variables and this class or inner class
                case JClassDecl classDecl -> {

                    // Look for JTypeVar for given type name
                    JTypeVar typeVar = classDecl.getTypeParamDeclForName(typeName);
                    if (typeVar != null)
                        return typeVar.getTypeVariable();

                    // See if this class matches name or has inner class of name
                    JavaClass thisClass = classDecl.getEvalClass();
                    if (thisClass != null) {
                        if (thisClass.getSimpleName().equals(typeName))
                            return thisClass;
                        JavaClass innerClass = thisClass.getDeclaredClassForName(typeName);
                        if (innerClass != null)
                            return innerClass;
                    }
                }

                // Handle method/constructor decl: If matches TypeVar name, return typevar decl
                case JExecutableDecl execDecl -> {
                    JTypeVar typeVarDecl = execDecl.getTypeParamDeclForName(typeName);
                    if (typeVarDecl != null)
                        return typeVarDecl.getTypeVariable();
                }

                // Handle block statement: If any previous statements are class decl statements that declare type, return class
                case JStmtBlock blockStmt -> {
                    JavaClass javaClass = getJavaClassForStatementsAndChildType(blockStmt, childType);
                    if (javaClass != null)
                        return javaClass;
                }

                // Handle switch entry: If any previous statements are class decl statements that declare type, return class
                case JSwitchEntry switchEntry -> {
                    JavaClass javaClass = getJavaClassForStatementsAndChildType(switchEntry, childType);
                    if (javaClass != null)
                        return javaClass;
                }

                // Handle type variable: Handle nested case, e.g.: T extends Class <? super T>
                case JTypeVar typeVar -> {
                    if (typeName.equals(typeVar.getName()))
                        return typeVar.getJavaClassForClass(Object.class);
                }

                case JNode jnode -> { }
            }
        }

        return null;
    }

    /**
     * Looks for given child type in given statements.
     */
    private static JavaClass getJavaClassForStatementsAndChildType(WithStmts withStmts, JType childType)
    {
        List<JStmt> statements = withStmts.getStatements();
        String typeName = childType.getName();

        for (JStmt stmt : statements) {

            // If statement decl beyond type decl, just return
            if (stmt.getStartCharIndex() >= childType.getStartCharIndex())
                break;

            // If statement is class decl, return class if match
            if (stmt instanceof JStmtClassDecl classDeclStmt) {
                JClassDecl classDecl = classDeclStmt.getClassDecl();
                if (Objects.equals(typeName, classDecl.getName()))
                    return classDecl.getJavaClass();
            }
        }

        // Return not found
        return null;
    }
}
