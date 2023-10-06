/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.*;
import snap.util.ArrayUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This JavaFileBuilder implementation simply resolves the node tree to find errors.
 */
public class JavaFileBuilderSimple extends JavaFileBuilder {

    /**
     * Constructor for given Project.
     */
    public JavaFileBuilderSimple(Project aProject)
    {
        super(aProject);
    }

    /**
     * Returns whether file is build file.
     */
    @Override
    public boolean isBuildFile(WebFile aFile)
    {
        String type = aFile.getType();
        return type.equals("java") || type.equals("jepl");
    }

    /**
     * Compiles files.
     */
    @Override
    public boolean buildFiles(TaskMonitor aTaskMonitor)
    {
        // Empty case
        if (_buildFiles.size() == 0) return true;

        // Get files
        WebFile[] javaFiles = _buildFiles.toArray(new WebFile[0]);
        _buildFiles.clear();

        // Iterate over files and build
        boolean success = true;
        for (WebFile javaFile : javaFiles)
            success &= buildFile(javaFile);

        // Return
        return success;
    }

    /**
     * Builds a file.
     */
    public boolean buildFile(WebFile javaFile)
    {
        // Get JavaAgent and JFile
        JavaAgent javaAgent = JavaAgent.getAgentForFile(javaFile);
        JFile jFile = javaAgent.getJFile();
        List<NodeError> errorsList = new ArrayList<>();

        // Find errors in JFile
        findNodeErrors(jFile, errorsList);
        NodeError[] errors = errorsList.toArray(NodeError.NO_ERRORS);

        // Convert to BuildIssues and set in agent
        BuildIssue[] buildIssues = ArrayUtils.map(errors, error -> BuildIssue.createIssueForNodeError(error, javaFile), BuildIssue.class);
        javaAgent.setBuildIssues(buildIssues);

        // Return
        boolean success = errorsList.size() == 0;
        return success;
    }

    /**
     * Recurse into nodes
     */
    private static void findNodeErrors(JNode aNode, List<NodeError> theErrors)
    {
        NodeError[] errors = aNode.getErrors();
        if (errors.length > 0)
            Collections.addAll(theErrors, errors);

        if (aNode instanceof JStmtExpr)
            return;

        List<JNode> children = aNode.getChildren();
        for (JNode child : children)
            findNodeErrors(child, theErrors);
    }
}
