package snapcode.app;
import snap.util.ArrayUtils;
import snap.view.Clipboard;
import snap.view.ClipboardData;
import snap.view.View;
import snap.view.ViewEvent;
import snap.web.WebFile;
import snap.web.WebURL;

/**
 * Handle Drag and Drop for WorkspacePane.
 */
public class WorkspacePaneDnD {

    // The WorkspacePane
    private WorkspacePane _workspacePane;

    /**
     * Constructor.
     */
    public WorkspacePaneDnD(WorkspacePane workspacePane)
    {
        _workspacePane = workspacePane;

        // Add drag listener to content view
        View pagePane = _workspacePane.getPagePane().getUI();
        pagePane.addEventHandler(this::handleDragEvent, View.DragEvents);
    }

    /**
     * Called when content gets drag event.
     */
    private void handleDragEvent(ViewEvent anEvent)
    {
        // Handle drag over: Accept
        if (anEvent.isDragOver()) {
            if (isSupportedDragEvent(anEvent))
                anEvent.acceptDrag();
            return;
        }

        // Handle drop
        if (anEvent.isDragDrop()) {
            if (!isSupportedDragEvent(anEvent))
                return;
            anEvent.acceptDrag();
            Clipboard clipboard = anEvent.getClipboard();
            ClipboardData clipboardData = clipboard.getFiles().get(0);
            dropFile(clipboardData);
            anEvent.dropComplete();
        }
    }

    /**
     * Returns whether event is supported drag event.
     */
    private boolean isSupportedDragEvent(ViewEvent anEvent)
    {
        Clipboard clipboard = anEvent.getClipboard();
        if (!clipboard.hasFiles())
            return false;
        return true;
    }

    /**
     * Called to handle a file drop on top graphic.
     */
    private void dropFile(ClipboardData clipboardData)
    {
        // If clipboard data not loaded, come back when it is
        if (!clipboardData.isLoaded()) {
            clipboardData.addLoadListener(f -> dropFile(clipboardData));
            return;
        }

        // Get path and extension (set to empty string if null)
        String fileType = clipboardData.getFileType();
        if (fileType == null)
            return;

        // If unknown type, just return
        if (!ArrayUtils.contains(new String[] { "zip", "gfar", "java", "jepl" }, fileType))
            return;

        // Create temp file for clipboard data
        WebFile dropFile = getFileForClipboardData(clipboardData);
        WorkspacePaneUtils.openFile(_workspacePane, dropFile);
    }

    /**
     * Returns a file for clipboard data.
     */
    private static WebFile getFileForClipboardData(ClipboardData clipboardData)
    {
        // If file available (desktop), get file and return
        WebURL sourceUrl = clipboardData.getSourceURL();
        if (sourceUrl != null) {
            WebFile sourceFile = sourceUrl.getFile();
            if (sourceFile != null)
                return sourceFile;
        }

        // Get filename and bytes, create temp file and return
        String filename = clipboardData.getName();
        byte[] fileBytes = clipboardData.getBytes();
        return createTempFileForNameAndBytes(filename, fileBytes);
    }

    /**
     * Creates a temp file for name and bytes.
     */
    private static WebFile createTempFileForNameAndBytes(String filename, byte[] fileBytes)
    {
        // Create temp file for drop file, set bytes and save
        WebFile tempFile = WebFile.createTempFileForName(filename, false);
        tempFile.setBytes(fileBytes);
        tempFile.save();

        // Return
        return tempFile;
    }
}
