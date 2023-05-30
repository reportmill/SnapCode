/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.runner;
import java.io.PrintStream;

/**
 * Utility methods and support for JavaShell.
 */
public class JavaShellUtils {

    /**
     * A PrintStream to stand in for System.out and System.err.
     */
    protected static class ProxyPrintStream extends PrintStream {

        // The JavaShell
        private JavaShell  _javaShell;

        // Whether is standard err
        private boolean  _stdErr;

        /**
         * Constructor.
         */
        public ProxyPrintStream(JavaShell javaShell, PrintStream printStream)
        {
            super(printStream);
            _javaShell = javaShell;
            _stdErr = printStream == System.err;
        }

        /**
         * Override to send to ScanView.
         */
        public void write(int b)
        {
            // Do normal version
            super.write(b);

            // Write char to console
            String str = String.valueOf(Character.valueOf((char) b));
            if (_stdErr)
                _javaShell.appendErr(str);
            else _javaShell.appendOut(str);
        }

        /**
         * Override to send to ScanView.
         */
        public void write(byte[] buf, int off, int len)
        {
            // Do normal version
            super.write(buf, off, len);

            // Write buff to console
            String str = new String(buf, off, len);
            if (_stdErr)
                _javaShell.appendErr(str);
            else _javaShell.appendOut(str);
        }
    }
}
