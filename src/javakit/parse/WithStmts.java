package javakit.parse;
import javakit.resolver.JavaClass;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * This interface identifies nodes with child statements (JStmtBlock, JStmtSwitchEntry).
 */
public interface WithStmts {

    List<JStmt> getStatements();

    /**
     * Returns VarDecls encapsulated by WithStmts (JStmtVarDecl.VarDecls).
     */
    static JavaClass getJavaClassForChildTypeOrId(WithStmts withStmts, JNode typeOrIdNode)
    {
        List<JStmt> statements = withStmts.getStatements();
        String typeName = typeOrIdNode.getName();

        for (JStmt stmt : statements) {

            // If statement decl beyond type decl, just return
            if (stmt.getStartCharIndex() >= typeOrIdNode.getStartCharIndex())
                break;

            // If statement is class decl, return class if match
            if (stmt instanceof JStmtClassDecl) {
                JStmtClassDecl classDeclStmt = (JStmtClassDecl) stmt;
                JClassDecl classDecl = classDeclStmt.getClassDecl();
                if (Objects.equals(typeName, classDecl.getName()))
                    return classDecl.getJavaClass();
            }
        }

        // Return not found
        return null;
    }

    /**
     * Returns VarDecls encapsulated by WithStmts (JStmtVarDecl.VarDecls).
     */
    static JVarDecl[] getVarDecls(WithStmts withStmts)
    {
        // Get Statement.VarDecls
        List<JStmt> statements = withStmts.getStatements();
        Stream<JStmtVarDecl> varDeclStmtsStream = (Stream<JStmtVarDecl>) (Stream<?>) statements.stream().filter(stmt -> stmt instanceof JStmtVarDecl);
        Stream<JVarDecl> varDeclsStream = varDeclStmtsStream.flatMap(stmt -> Stream.of(stmt.getVarDecls()));
        return varDeclsStream.toArray(size -> new JVarDecl[size]);
    }
}
