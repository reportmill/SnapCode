/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.project.*;
import snap.util.FilePathUtils;
import snap.web.WebFile;
import snap.web.WebSite;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * A class to manage build attributes and behavior for a WebSite.
 */
public class ProjectX extends Project {

    /**
     * Constructor.
     */
    public ProjectX(Workspace aWorkspace, WebSite aSite)
    {
        super(aWorkspace, aSite);

        // Create/set ProjectBuilder.JavaFileBuilderImpl
        JavaFileBuilder javaFileBuilder = new JavaFileBuilderImpl(this);
        _projBuilder.setJavaFileBuilder(javaFileBuilder);
    }

    /**
     * Override to support legacy EclipseBuildFile.
     */
    @Override
    protected BuildFile getBuildFileImpl()
    {
        // Do normal version - just return if build file already exists
        BuildFile buildFile = super.getBuildFileImpl();
        WebFile buildFileFile = buildFile.getBuildFile();
        if (buildFileFile.getExists())
            return buildFile;

        // If build file doesn't exist, try EclipseBuildFile
        new EclipseBuildFile(this, buildFile);
        buildFile.writeFile();
        return buildFile;
    }

    /**
     * Returns a class loader to be used with compiler.
     */
    @Override
    public ClassLoader createCompilerClassLoader()
    {
        // Get CompilerClassPaths and ClassPathUrls
        String[] classPaths = getCompilerClassPaths();
        URL[] classPathUrls = FilePathUtils.getUrlsForPaths(classPaths);

        // Get System ClassLoader
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader().getParent();

        // Create/Return URLClassLoader for classPath
        return new URLClassLoader(classPathUrls, systemClassLoader);
    }
}