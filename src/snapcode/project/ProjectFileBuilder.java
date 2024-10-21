/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import snap.util.ListUtils;
import snap.util.TaskMonitor;
import snap.web.WebFile;

import java.util.ArrayList;
import java.util.List;

/**
 * An interface for classes that can "build" changed files.
 */
public interface ProjectFileBuilder {

    /**
     * Returns whether this builder has files to build.
     */
    boolean isNeedsBuild();

    /**
     * Returns whether file is build file.
     */
    boolean isBuildFile(WebFile aFile);

    /**
     * Returns whether given file needs a build.
     */
    boolean isFileNeedsBuild(WebFile aFile);

    /**
     * Add a build file.
     */
    void addBuildFile(WebFile aFile);

    /**
     * Remove a build file.
     */
    void removeBuildFile(WebFile aFile);

    /**
     * Build files.
     */
    boolean buildFiles(TaskMonitor aTM);
}