/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.project.Project;
import javakit.project.ProjectFiles;
import snap.web.WebFile;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;

/**
 * A Java File Object for a WebFile.
 */
class SnapCompilerJFO extends SimpleJavaFileObject {

    // The Project
    private Project _proj;

    protected SnapCompiler _compiler;

    // The SnapCompiler and JavaFile (if available)
    WebFile _file;
    WebFile _sourceFile;
    String _bname, _str;

    /**
     * Creates a new SnapFileJFO with WebFile, SnapCompiler and (optional) source file.
     */
    protected SnapCompilerJFO(Project aProject, WebFile aFile, SnapCompiler aCompiler)
    {
        super(URI.create(aFile.getPath()), isJavaFile(aFile) ? Kind.SOURCE : Kind.CLASS);
        _file = aFile;
        _proj = aProject;
        _compiler = aCompiler;

        // If Class file, Get SourceFile
        if (_file.getType().equals("class")) {
            ProjectFiles projectFiles = _proj.getProjectFiles();
            _sourceFile = projectFiles.getJavaFileForClassFile(_file);
        }
    }

    /**
     * Returns the file.
     */
    public WebFile getFile()  { return _file; }

    /**
     * Returns ModifiedTime of WebFile.
     */
    public long getLastModified()
    {
        return _file.getLastModTime();
    }

    /**
     * Returns the "binary name" for the CompilerFileManager inferBinaryName method.
     */
    public String getBinaryName()
    {
        if (_bname != null) return _bname;
        String path = _file.getPath();
        int index = path.lastIndexOf('.');
        String name = path.substring(1, index).replace('/', '.');
        return _bname = name;
    }

    /**
     * Returns the char content of file (for source file).
     */
    public CharSequence getCharContent(boolean ignoreEncodingErrors)
    {
        if (_str != null) return _str;
        _str = _file.getText();
        _proj.getRootProject().getBuildIssues().removeIssuesForFile(_file);
        return _str;
    }

    /**
     * Returns input stream for source/class bytes.
     */
    public InputStream openInputStream()
    {
        return _file.getInputStream();
    }

    /**
     * Returns output stream for byte code.
     */
    public OutputStream openOutputStream()
    {
        return new ByteArrayOutputStream() {
            public void close() throws IOException
            {

                // Do normal close and add SourceFile to Compiler.CompiledFiles
                super.close();
                _compiler._compJFs.add(_sourceFile);

                // Get bytes and whether class file is modified
                byte[] bytes = toByteArray();
                boolean modified = !Arrays.equals(bytes, _file.getBytes());

                // If modified, set File.Bytes and add ClassFile to ModifiedFiles and SourceFile to ModifiedSources
                if (modified) {
                    _file.setBytes(bytes);
                    _compiler._modJFs.add(_sourceFile);
                }

                // If file was modified or a real compile file, save
                if (modified || _file.getLastModTime() < _sourceFile.getLastModTime()) {
                    try {
                        _file.save();
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    /**
     * This is weird, but if I don't override this, backtraces show: "MyClass from JavaCompilerFO:22"
     */
    public String toString()
    {
        return uri.toString();
    }

    /**
     * Returns whether file is java file.
     */
    private static boolean isJavaFile(WebFile aFile)
    {
        return aFile.getType().equals("java");
    }
}
