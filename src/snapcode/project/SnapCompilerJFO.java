/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.project;
import javakit.parse.JFile;
import snap.web.WebFile;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;

/**
 * A JavaFileObject subclass for a WebFile.
 */
class SnapCompilerJFO extends SimpleJavaFileObject {

    // The Project
    private Project  _proj;

    // The Compiler
    protected SnapCompiler  _compiler;

    // The class file
    private WebFile  _file;

    // The Java source file
    protected WebFile _javaFile;

    // The binary name
    private String  _binaryName;

    /**
     * Constructor for project, file (source or class) and SnapCompiler.
     */
    protected SnapCompilerJFO(Project aProject, WebFile aFile, SnapCompiler aCompiler)
    {
        super(getFileURI(aFile), isJavaFile(aFile) ? Kind.SOURCE : Kind.CLASS);
        _file = aFile;
        _proj = aProject;
        _compiler = aCompiler;
    }

    /**
     * Returns the file.
     */
    public WebFile getFile()  { return _file; }

    /**
     * Returns ModifiedTime of WebFile.
     */
    @Override
    public long getLastModified()  { return _file.getLastModTime(); }

    /**
     * Returns the "binary name" for the CompilerFileManager inferBinaryName method.
     */
    public String getBinaryName()
    {
        // If already set, just return
        if (_binaryName != null) return _binaryName;

        // Get binary name from file
        String path = _file.getPath();
        int index = path.lastIndexOf('.');
        String binaryName = path.substring(1, index).replace('/', '.');

        // Set, return
        return _binaryName = binaryName;
    }

    /**
     * Returns the char content of file (for source file).
     */
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors)
    {
        // Get java text string
        String javaTextStr = getJavaTextString();

        // Since compiler just read new file contents, clear buildIssues for file
        JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(_file);
        javaAgent.clearBuildIssues();

        // Return
        return javaTextStr;
    }

    /**
     * Returns the Java string of file.
     */
    private String getJavaTextString()
    {
        // Handle Jepl and Java markdown
        if (_file.getFileType().equals("jepl") || _file.getFileType().equals("jmd")) {
            JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(_file);
            JFile jFile = javaAgent.getJFile();
            int javaVersion = _proj.getBuildFile().getCompileRelease();
            return new JeplToJava(jFile, javaVersion).getJava();
        }

        // Get string
        return _file.getText();
    }

    /**
     * Returns input stream for source/class bytes.
     */
    @Override
    public InputStream openInputStream()
    {
        return _file.getInputStream();
    }

    /**
     * Returns output stream for byte code.
     */
    @Override
    public OutputStream openOutputStream()
    {
        return new ByteArrayOutputStream() {
            public void close() throws IOException
            {
                super.close();
                byte[] classFileBytes = toByteArray();
                compileFinished(classFileBytes);
            }
        };
    }

    /**
     * Called after file is read/compiled/closed.
     */
    private void compileFinished(byte[] classFileBytes)
    {
        // If SourceFile not set, get it
        if (_javaFile == null)
            _javaFile = _proj.getProjectFiles().getJavaFileForClassFile(_file);

        // Add SourceFile to Compiler.CompiledJFs
        _compiler._compiledJavaFiles.add(_javaFile);

        // Get bytes and whether class file is modified
        boolean modified = !_file.getExists() || !Arrays.equals(classFileBytes, _file.getBytes());

        // If modified, set File.Bytes and add ClassFile to ModifiedFiles and SourceFile to ModifiedSources
        if (modified) {

            // Set enw bytes
            _file.setBytes(classFileBytes);
            _compiler._modifiedJavaFiles.add(_javaFile);

            // Clear external references
            JavaAgent javaAgent = JavaAgent.getAgentForJavaFile(_javaFile);
            javaAgent.clearExternalReferences();
        }

        // If file was modified or a real compile file, save
        if (modified || _file.getLastModTime() < _javaFile.getLastModTime()) {
            try { _file.save(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
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
        String type = aFile.getFileType();
        return type.equals("java") || type.equals("jepl") || type.equals("jmd");
    }

    /**
     * Returns the URI for a file.
     */
    private static URI getFileURI(WebFile aFile)
    {
        String filePath = aFile.getPath();
        if (aFile.getFileType().equals("jepl"))
            filePath = filePath.substring(0, filePath.length() - 3) + "ava";
        if (aFile.getFileType().equals("jmd"))
            filePath = filePath.substring(0, filePath.length() - 2) + "ava";
        return URI.create(filePath);
    }
}
