package javakit.parse;
import javakit.resolver.*;
import snap.util.ArrayUtils;
import java.util.List;
import java.util.Objects;

/**
 * Utility methods to resolve an id to declaration.
 */
class ResolveDeclForChildId {

    /**
     * Returns the JavaDecl most closely associated with given child JExprId node.
     */
    public static JavaDecl getDeclForChildId(JExprId childId)
    {
        String childIdName = childId.getName();

        for (JNode parentNode = childId.getParent(); parentNode != null; parentNode = parentNode.getParent()) {

            switch (parentNode) {

                // Handle import decl
                case JImportDecl importDecl -> {
                    if (importDecl.isModule()) {
                        Resolver resolver = importDecl.getResolver();
                        return resolver != null ? resolver.getJavaModuleForName(childIdName) : null;
                    }
                    return importDecl.getJavaPackageForName(childIdName);
                }

                // Handle file
                case JFile jfile -> {

                    // See if it's a known class name using imports
                    String className = jfile.getImportClassName(childIdName);
                    JavaClass javaClass = className != null ? jfile.getJavaClassForName(className) : null;
                    if (javaClass != null)
                        return javaClass;

                    // See if it's a known static import class member
                    JavaMember field = jfile.getStaticImportMemberForNameAndParamTypes(childIdName, null);
                    if (field != null)
                        return field;

                    // If name is known package name, return package
                    if (jfile.isKnownPackageName(childIdName))
                        return jfile.getJavaPackageForName(childIdName);
                }

                // Handle class decl
                case JClassDecl classDecl -> {

                    // If it's "this", set class and return ClassField
                    if (childIdName.equals("this"))
                        return classDecl.getDecl();

                    // If it's "super", set class and return ClassField
                    if (childIdName.equals("super"))
                        return classDecl.getSuperClass();

                    // Iterate over enum constants
                    if (classDecl.isEnum()) {
                        JEnumConst[] enumConstants = classDecl.getEnumConstants();
                        for (JEnumConst enumConst : enumConstants) {
                            if (childIdName.equals(enumConst.getName()))
                                return enumConst.getDecl();
                        }
                    }

                    // See if it is record parameter
                    if (classDecl.isRecord()) {
                        JVarDecl[] params = classDecl.getParameters();
                        for (JVarDecl param : params) {
                            if (childIdName.equals(param.getName()))
                                return param.getDecl();
                        }
                    }

                    // See if it's a field reference from superclass
                    JavaClass superClass = classDecl.getSuperClass();
                    if (superClass != null) {
                        JavaField field = superClass.getFieldForName(childIdName);
                        if (field != null)
                            return field;
                    }

                    // Check interfaces (id could be interface static field)
                    JType[] implementsTypes = classDecl.getImplementsTypes();
                    for (JType implementsType : implementsTypes) {
                        JavaClass interf = implementsType.getEvalClass();
                        JavaField field2 = interf != null ? interf.getDeclaredFieldForName(childIdName) : null;
                        if (field2 != null)
                            return field2;
                    }

                    // Look for InnerClass of given name
                    JavaClass thisClass = classDecl.getEvalClass();
                    if (thisClass != null) {
                        JavaClass innerClass = thisClass.getDeclaredClassForName(childIdName);
                        if (innerClass != null)
                            return innerClass;
                    }
                }

                // Handle type variable: nested case, e.g.: T extends Class <? super T>
                case JTypeVar typeVar -> {
                    if (childId.getName().equals(typeVar.getName()))
                        return typeVar.getJavaClassForClass(Object.class);
                }

                // Handle block statement: If any previous statements are class decl statements that declare type, return class
                case JStmtBlock blockStmt -> {
                    JavaClass javaClass = getJavaClassForStatementsAndChildId(blockStmt, childId);
                    if (javaClass != null)
                        return javaClass;
                }

                // Handle switch entry
                case JSwitchEntry switchEntry -> {

                    // If node is case label id, try to evaluate against Switch expression enum type
                    if (switchEntry.getLabels().contains(childId))
                        return getJavaDeclForCaseLabelId(childId);

                    // If any previous statements are class decl statements that declare type, return class
                    JavaClass javaClass = getJavaClassForStatementsAndChildId(switchEntry, childId);
                    if (javaClass != null)
                        return javaClass;
                }

                // Handle constructor call statement: Check ids
                case JStmtConstrCall constrCallStmt -> {
                    if (childId.getParent() == constrCallStmt && ArrayUtils.containsId(constrCallStmt.getIds(), childId))
                        return constrCallStmt.getDecl();
                }

                // Handle method ref
                case JExprMethodRef methodRefExpr when childId == methodRefExpr.getMethodId() -> {

                    // If array creation, return prefix expr class
                    if (methodRefExpr.getType() == JExprMethodRef.Type.ArrayInit)
                        return methodRefExpr.getPrefixExprClass();

                    // If method ref is for method or constructor, return it
                    JavaExecutable methodOrConstr = methodRefExpr.getExecutable();
                    if (methodOrConstr != null)
                        return methodOrConstr;
                }

                // Handle label statement id
                case JStmtLabeled labeledStmt -> {
                    if (Objects.equals(childIdName, labeledStmt.getName()))
                        return labeledStmt.getDecl();
                }

                // Handle type
                case JType type -> {
                    if (childId == type.getBaseExpr())
                        return type.getBaseType();
                }

                case JNode jnode -> { }
            }
        }

        return null;
    }

    /**
     * Looks for given child id in given statements.
     */
    private static JavaClass getJavaClassForStatementsAndChildId(WithStmts withStmts, JExprId childId)
    {
        List<JStmt> statements = withStmts.getStatements();
        String childIdName = childId.getName();

        for (JStmt stmt : statements) {

            // If statement decl beyond type decl, just return
            if (stmt.getStartCharIndex() >= childId.getStartCharIndex())
                break;

            // If statement is class decl, return class if match
            if (stmt instanceof JStmtClassDecl classDeclStmt) {
                JClassDecl classDecl = classDeclStmt.getClassDecl();
                if (Objects.equals(childIdName, classDecl.getName()))
                    return classDecl.getJavaClass();
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns the decl for the case expression.
     */
    private static JavaDecl getJavaDeclForCaseLabelId(JExprId caseLabelId)
    {
        JExprSwitch switchStmt = caseLabelId.getParent(JExprSwitch.class);
        JExpr switchExpr = switchStmt.getSelector();

        // Get Switch expression type
        JavaType switchExprType = switchExpr != null ? switchExpr.getEvalType() : null;
        if (switchExprType == null)
            return null;

        // Handle enum switch
        if (switchExprType.isEnum()) {
            JavaClass enumClass = (JavaClass) switchExprType;
            String enumName = caseLabelId.getName();
            JavaField enumConst = enumClass.getDeclaredFieldForName(enumName);
            if (enumConst != null)
                return enumConst;
        }

        return switchExprType;
    }
}
