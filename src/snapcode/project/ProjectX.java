/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.FilePathUtils;
import snap.util.SnapUtils;
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
        if (!SnapUtils.isWebVM) {
            JavaFileBuilder javaFileBuilder = new JavaFileBuilderImpl(this);
            _projBuilder.setJavaFileBuilder(javaFileBuilder);
        }
    }

    /**
     * Returns a class loader to be used with compiler.
     */
    @Override
    public ClassLoader createCompilerClassLoader()
    {
        // Get CompilerClassPaths and ClassPathUrls
        String[] classPaths = getCompileClassPaths();
        URL[] classPathUrls = FilePathUtils.getUrlsForPaths(classPaths);

        // Get System ClassLoader
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader().getParent();

        // Create/Return URLClassLoader for classPath
        return new URLClassLoader(classPathUrls, systemClassLoader);
    }
}