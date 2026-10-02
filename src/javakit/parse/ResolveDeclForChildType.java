package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;
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
        String typeName = childType.getSimpleName();

        for (JNode parentNode = childType.getParent(); parentNode != null; parentNode = parentNode.getParent()) {

            switch (parentNode) {

                // Handle JFile: See if type is found in imports
                case JFile jfile -> {
                    String className = jfile.getClassNameForSimpleName(typeName);
                    JavaClass javaClass = className != null ? jfile.getJavaClassForName(className) : null;
                    if (javaClass != null)
                        return javaClass;
                }

                // Handle class decl: Check type variables and this class or inner class
                case JClassDecl classDecl -> {

                    // If extends/implements type, just continue
                    if (ArrayUtils.hasMatch(classDecl.getExtendsTypes(), etype -> childType == etype || childType.isAncestor(etype)))
                        continue;
                    if (ArrayUtils.hasMatch(classDecl.getImplementsTypes(), itype -> childType == itype || childType.isAncestor(itype)))
                        continue;

                    // Look for JTypeVar for given type name
                    JTypeVar typeVar = classDecl.getTypeParamDeclForName(typeName);
                    if (typeVar != null)
                        return typeVar.getTypeVariable();

                    // See if this class matches name
                    if (classDecl.getSimpleName().equals(typeName))
                        return classDecl.getJavaClass();

                    // See if inner class matches name
                    JClassDecl innerClass = ArrayUtils.findMatch(classDecl.getDeclaredClassDecls(), cdecl -> cdecl.getSimpleName().equals(typeName));
                    if (innerClass != null)
                        return innerClass.getJavaClass();

                    // See extends class(es) matches name or have matching inner classes
                    for (JType extendsType : classDecl.getExtendsTypes()) {
                        if (extendsType.getSimpleName().equals(typeName))
                            return extendsType.getJavaClass();
                        JavaClass extendsClass = extendsType.getJavaClass();
                        if (extendsClass != null) {
                            JavaClass extendsClassInnerClass = extendsClass.getClassForName(typeName);
                            if (extendsClassInnerClass != null)
                                return extendsClassInnerClass;
                        }
                    }

                    // See implements class(es) matches name or have matching inner classes
                    for (JType implementsType : classDecl.getImplementsTypes()) {
                        if (implementsType.getSimpleName().equals(typeName))
                            return implementsType.getJavaClass();
                        JavaClass implementsClass = implementsType.getJavaClass();
                        if (implementsClass != null) {
                            JavaClass implementsClassInnerClass = implementsClass.getClassForName(typeName);
                            if (implementsClassInnerClass != null)
                                return implementsClassInnerClass;
                        }
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
