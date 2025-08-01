package snapcode.util;
import snap.text.TextModel;
import snap.web.WebSite;
import snapcode.project.JavaTextModel;
import snapcode.javatext.JavaTextArea;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.gfx.Painter;
import snap.gfx.Stroke;
import snapcode.project.VersionControl;
import snap.text.TextLine;
import snap.text.TextSel;
import snap.view.*;
import snapcode.webbrowser.WebPage;
import snap.web.WebFile;
import java.util.ArrayList;
import java.util.List;

/**
 * A WebPage subclass for viewing a DiffFile.
 */
public class DiffPage extends WebPage {

    // The LocalFile
    WebFile _localFile;

    // The diff pane
    SplitView _splitView;

    // The two texts
    TextArea _leftTextArea, _rightTextArea;

    /**
     * Creates a new DiffPage for given file.
     */
    public DiffPage(WebFile aFile)
    {
        _localFile = aFile; //WebURL url = WebURL.getURL(aFile.getURL().getString() + ".diff"); setURL(url);
        WebFile cmpFile = aFile.getSite().createFileForPath(aFile.getPath() + ".diff", false);
        setFile(cmpFile);
    }

    /**
     * Creates the UI.
     */
    protected View createUI()
    {
        // Create SplitView for left/right TextAreas, and ScrollView to hold it
        _splitView = new SplitView();
        ScrollView spane = new ScrollView(_splitView);
        spane.setFillWidth(true);
        spane.setGrowWidth(true);

        // Wrap ScrollView and OverviewPane in HBox and return
        RowView hbox = new RowView();
        hbox.setFillHeight(true);
        hbox.setChildren(spane, new OverviewPane());
        return hbox;
    }

    /**
     * Initialize UI.
     */
    protected void initUI()
    {
        // Get texts, initialize and install
        WebFile localFile = getLocalFile();
        WebFile remoteFile = getRemoteFile();
        _leftTextArea = getTextAreaForFile(localFile);
        _leftTextArea.setGrowWidth(true);
        _rightTextArea = getTextAreaForFile(remoteFile);
        _rightTextArea.setGrowWidth(true);

        // Get DiffPane and install texts
        _splitView.setItems(_leftTextArea, _rightTextArea);

        // Get ranges lists
        List<TextSel> lranges, rranges;
        lranges = _leftTextArea instanceof DiffTextArea ? ((DiffTextArea) _leftTextArea).ranges : ((DiffJavaTextArea) _leftTextArea).ranges;
        rranges = _rightTextArea instanceof DiffTextArea ? ((DiffTextArea) _rightTextArea).ranges : ((DiffJavaTextArea) _rightTextArea).ranges;

        // Print diffs
        DiffUtil diffUtil = new DiffUtil();
        List<DiffUtil.Diff> diffs = diffUtil.diff_main(_rightTextArea.getText(), _leftTextArea.getText());
        int localIndex = 0;
        int remoteIndex = 0;
        for (DiffUtil.Diff diff : diffs) {
            DiffUtil.Operation op = diff.operation;
            boolean insert = op == DiffUtil.Operation.INSERT;
            boolean delete = op == DiffUtil.Operation.DELETE;
            int length = diff.text.length();

            // Handle insert
            if (insert) {
                TextModel textModel = _leftTextArea.getTextModel();
                lranges.add(new TextSel(textModel, localIndex, localIndex + length));
                localIndex += length;
            }

            // Handle delete
            else if (delete) {
                TextModel textModel = _rightTextArea.getTextModel();
                rranges.add(new TextSel(textModel, remoteIndex, remoteIndex + length));
                remoteIndex += length;
            }

            // Handle other
            else {
                localIndex += length;
                remoteIndex += length;
            }
            //String pfx = insert? ">> " : delete? "<< " : ""; Sys.out.println(pfx + diff.text.replace("\n", "\n" + pfx));
        }
    }

    /**
     * Returns the file local to project.
     */
    public WebFile getLocalFile()  { return _localFile; }

    /**
     * Returns the file from Project.RemoteSite.
     */
    public WebFile getRemoteFile()
    {
        WebFile localFile = getLocalFile();
        VersionControl versionControl = VersionControl.getVersionControlForProjectSite(localFile.getSite());
        WebSite remoteSite = versionControl.getRemoteSite();
        return remoteSite.getFileForPath(localFile.getPath());
    }

    /**
     * Returns the text area for file.
     */
    private TextArea getTextAreaForFile(WebFile aFile)
    {
        // Refresh file to get latest version
        aFile.resetAndVerify();

        // Handle JavaFile
        if (aFile.getFileType().equals("java")) {

            // Create JavaTextModel
            JavaTextModel javaTextModel = JavaTextModel.getJavaTextModelForFile(aFile);

            // Create DiffJavaTextArea and set JavaTextModel
            DiffJavaTextArea diffJavaTextArea = new DiffJavaTextArea();
            diffJavaTextArea.setTextModel(javaTextModel);
            return diffJavaTextArea;
        }

        // Handle normal TextFile
        TextArea diffTextArea = new DiffTextArea();
        diffTextArea.setFont(Font.getCodeFontForSize(10));
        diffTextArea.setText(aFile.getText());
        return diffTextArea;
    }

    // Stroke and fill colors for diffs
    private static final Color DIFF_FILL_COLOR = new Color(230, 230, 230, 192);
    private static final Color DIFF_STROKE_COLOR = new Color(140, 140, 140);

    /**
     * A text area that shows diffs.
     */
    static class DiffTextArea extends TextArea {

        // The ranges
        List<TextSel> ranges = new ArrayList<>();

        /**
         * Constructor.
         */
        public DiffTextArea()
        {
            super();
            setPadding(5, 5, 5, 5);
            setFill(Color.WHITE);
            setEditable(true);
        }

        /**
         * Override to add ranges.
         */
        protected void paintBack(Painter aPntr)
        {
            super.paintBack(aPntr);
            for (TextSel range : ranges) {
                Shape rpath = range.getPath();
                aPntr.setPaint(DIFF_FILL_COLOR);
                aPntr.fill(rpath);
                aPntr.setPaint(DIFF_STROKE_COLOR);
                aPntr.draw(rpath);
            }
        }
    }

    /**
     * A text area that shows diffs.
     */
    static class DiffJavaTextArea extends JavaTextArea {

        // The ranges
        List<TextSel> ranges = new ArrayList<>();

        /**
         * Constructor.
         */
        public DiffJavaTextArea()
        {
            super();
        }

        /**
         * Override to add ranges.
         */
        protected void paintBack(Painter aPntr)
        {
            super.paintBack(aPntr);
            for (TextSel range : ranges) {
                Shape rangePath = range.getPath();
                aPntr.setPaint(DIFF_FILL_COLOR);
                aPntr.fill(rangePath);
                aPntr.setPaint(DIFF_STROKE_COLOR);
                aPntr.draw(rangePath);
            }
        }
    }

    // Colors
    private static final Color MARKER_COLOR = new Color(181, 214, 254, 255);
    private static final Color MARKER_BORDER_COLOR = MARKER_COLOR.darker();

    /**
     * A component to show locations of Errors, warnings, selected symbols, etc.
     */
    public class OverviewPane extends View {

        // The list of markers
        List<Marker> _markers;

        // The last mouse point
        double _mx, _my;

        /**
         * Creates a new OverviewPane.
         */
        public OverviewPane()
        {
            enableEvents(MouseMove, MouseRelease);
            setToolTipEnabled(true);
            setPrefWidth(14);
        }

        /**
         * Sets the JavaTextArea selection.
         */
        public void setTextSel(int aStart, int anEnd)
        {
            _leftTextArea.setSel(aStart, anEnd);
        }

        /**
         * Returns the list of markers.
         */
        public List<Marker> getMarkers()
        {
            // If already set, just return
            if (_markers != null) return _markers;

            // Create list
            List<Marker> markers = new ArrayList<>();

            // Add markers for TextArea.JavaSource.BuildIssues
            List<TextSel> ranges = _leftTextArea instanceof DiffTextArea ? ((DiffTextArea) _leftTextArea).ranges :
                    ((DiffJavaTextArea) _leftTextArea).ranges;
            for (TextSel ts : ranges)
                markers.add(new Marker(ts));

            // Return markers
            return _markers = markers;
        }

        /**
         * Called on mouse click to select marker line.
         */
        protected void processEvent(ViewEvent anEvent)
        {
            // Handle MosueClicked
            if (anEvent.isMouseClick()) {
                for (Marker marker : getMarkers()) {
                    if (marker.contains(anEvent.getX(), anEvent.getY())) {
                        setTextSel(marker.getSelStart(), marker.getSelEnd());
                        return;
                    }
                }

                TextModel textModel = _leftTextArea.getTextModel();
                TextLine line = textModel.getLineForY(anEvent.getY() / getHeight() * _leftTextArea.getHeight());
                setTextSel(line.getStartCharIndex(), line.getEndCharIndex());
            }

            // Handle MouseMoved
            if (anEvent.isMouseMove()) {
                _mx = anEvent.getX();
                _my = anEvent.getY();
                for (Marker marker : getMarkers()) {
                    if (marker.contains(_mx, _my)) {
                        setCursor(Cursor.HAND);
                        return;
                    }
                }
                setCursor(Cursor.DEFAULT);
            }
        }

        /**
         * Paint markers.
         */
        protected void paintFront(Painter aPntr)
        {
            double th = _leftTextArea.getHeight();
            double h = Math.min(getHeight(), th);
            aPntr.setStroke(Stroke.Stroke1);
            for (Marker marker : getMarkers()) {
                marker.setY(marker._y / th * h);
                aPntr.setPaint(MARKER_COLOR);
                aPntr.fill(marker);
                aPntr.setPaint(MARKER_BORDER_COLOR);
                aPntr.draw(marker);
            }
        }

        /**
         * Override to return tool tip text.
         */
        public String getToolTip(ViewEvent anEvent)
        {
            // If marker, return special tooltip
            List<Marker> markers = getMarkers();
            for (int i = 0, iMax = markers.size(); i < iMax; i++) {
                Marker marker = markers.get(i);
                if (marker.contains(_mx, _my))
                    return String.format("Diff %d of %d, Line %d", i + 1, iMax, marker._sel.getStartLine().getLineIndex());
            }

            // Otherwise, just return line
            TextModel textModel = _leftTextArea.getTextModel();
            TextLine line = textModel.getLineForY(_my / getHeight() * _leftTextArea.getHeight());
            return "Line: " + (line.getLineIndex() + 1);
        }
    }

    /**
     * The class that describes a overview marker.
     */
    public static class Marker extends Rect {

        // The diff range
        TextSel _sel;

        // Y location of range start line in text.
        double _y;

        /**
         * Creates a new marker for target.
         */
        public Marker(TextSel aSel)
        {
            _sel = aSel;
            setRect(3, 0, 10, 5);
            TextLine line = aSel.getStartLine();
            _y = line.getY() + line.getHeight() / 2;
        }

        /**
         * Returns the selection start.
         */
        public int getSelStart()  { return _sel.getStart(); }

        /**
         * Returns the selection start.
         */
        public int getSelEnd()  { return _sel.getEnd(); }
    }
}