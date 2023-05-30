/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.project;
import snap.util.ListUtils;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides project methods that work over an array of projects.
 */
public class ProjectSet {

    // The array of projects
    private Project[]  _projects;

    // The array of class paths
    private String[]  _classPaths;

    /**
     * Creates a new ProjectSet for given Projects.
     */
    public ProjectSet(Project[] theProjects)
    {
        _projects = theProjects;
    }

    /**
     * Returns the source file for given path.
     */
    public WebFile getSourceFile(String aPath)
    {
        // Check projects
        for (Project proj : _projects) {
            WebFile file = proj.getSourceFile(aPath, false, false);
            if (file != null)
                return file;
        }

        // Return not found
        return null;
    }

    /**
     * Returns the paths needed to compile/run project.
     */
    public String[] getClassPaths()
    {
        // If already set, just return
        if (_classPaths != null) return _classPaths;

        // Get projects
        List<String> classPathsList = new ArrayList<>();

        // Iterate over projects and add Project.ClassPaths for each
        for (Project proj : _projects) {
            String[] projClassPaths = proj.getClassPaths();
            ListUtils.addAllUnique(classPathsList, projClassPaths);
        }

        // Set/return
        return _classPaths = classPathsList.toArray(new String[0]);
    }

    /**
     * Returns a Java file for class name.
     */
    public WebFile getJavaFileForClassName(String aClassName)
    {
        // Check projects
        for (Project proj : _projects) {
            ProjectFiles projFiles = proj.getProjectFiles();
            WebFile file = projFiles.getJavaFileForClassName(aClassName);
            if (file != null)
                return file;
        }

        // Return not found
        return null;
    }
}