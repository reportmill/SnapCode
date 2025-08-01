package snapcode.views;
import javakit.parse.JClassDecl;
import javakit.parse.JNode;
import javakit.parse.JStmt;
import javakit.parse.JavaParser;
import javakit.resolver.JavaClass;
import snap.geom.Point;
import snap.geom.Rect;
import snap.gfx.Image;
import snap.props.PropChangeListener;
import snap.util.ArrayUtils;
import snap.util.ListUtils;
import snap.view.*;
import snapcode.app.*;
import snapcode.javatext.JavaTextArea;
import snapcode.webbrowser.WebPage;
import java.util.stream.Stream;

/**
 * Tool to show puzzle blocks for drag and drop coding.
 */
public class BlocksTool extends WorkspaceTool {

    // The current JavaTextArea
    protected JavaTextArea _javaTextArea;

    // The ColView to hold Method blocks
    private ColView _methodBlocksColView;

    // The ColView to hold conditional blocks
    private ColView _conditionalBlocksColView;

    // The ColView to hold all blocks
    private ColView _allBlocksColView;

    // The selected class
    private JavaClass _selClass;

    // Listener for JavaTextArea prop change
    private PropChangeListener _textAreaPropChangeLsnr = pc -> handleJavaTextAreaSelNodeChange();

    // Listener for JavaTextArea drag events
    private EventListener _textAreaDragEventLsnr = e -> handleJavaTextAreaDragEvent(e);

    /**
     * Constructor.
     */
    public BlocksTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the selected class.
     */
    public JavaClass getSelClass()  { return _selClass; }

    /**
     * Sets the selected class.
     */
    public void setSelClass(JavaClass javaClass)
    {
        // If already set, just return
        if (javaClass == getSelClass()) return;

        // Set value
        _selClass = javaClass;

        // Reset UI
        resetLater();

        // Remove existing method blocks
        _methodBlocksColView.removeChildren();

        // Get blockView for SelClass and add to blocksColView
        JBlockView<?>[] blockViews = getBlockViewsForClass(_selClass);
        Stream.of(blockViews).forEach(_methodBlocksColView::addChild);
    }

    /**
     * Create UI.
     */
    protected View createUI()
    {
        // Create method blocks ColView
        _methodBlocksColView = new ColView();
        _methodBlocksColView.setMargin(10, 20, 10, 20);
        _methodBlocksColView.setSpacing(8);

        // Create conditional blocks ColView
        _conditionalBlocksColView = new ColView();
        _conditionalBlocksColView.setMargin(10, 20, 10, 20);
        _conditionalBlocksColView.setPadding(0, 0, 150, 0);
        _conditionalBlocksColView.setSpacing(8);

        // Create colView to hold all blocks
        _allBlocksColView = new ColView();
        _allBlocksColView.setFillWidth(true);
        _allBlocksColView.setGrowHeight(true);
        _allBlocksColView.setChildren(_methodBlocksColView, _conditionalBlocksColView);

        // Wrap in ScrollView
        ScrollView allBlocksScrollView = new ScrollView(_allBlocksColView);
        allBlocksScrollView.setGrowHeight(true);

        // Do normal version and add allBlocksScrollView
        ColView superUI = (ColView) super.createUI();
        superUI.addChild(allBlocksScrollView);

        // Return
        return superUI;
    }

    /**
     * Configure UI.
     */
    protected void initUI()
    {
        // Get DirectoryListView and configure
        ListView<String> directoryListView = getView("DirectoryListView", ListView.class);
        directoryListView.setFocusWhenPressed(false);
        directoryListView.setItems(ListUtils.of("Methods", "Conditionals"));
        directoryListView.setSelIndex(0);

        // Create conditional statement block views and add to ConditionalBlocksColView
        String ifStmtStr = "if (true) {\n}";
        String forStmtStr = "for (int i = 0; i < 10; i++) {\n}";
        String whileStmtStr = "while (true) {\n}";
        String[] condStmtStrings = { ifStmtStr, forStmtStr, whileStmtStr };
        JBlockView<?>[] condBlockViews = ArrayUtils.map(condStmtStrings, stmtStr -> createBlockViewForStatementString(stmtStr), JBlockView.class);
        Stream.of(condBlockViews).forEach(_conditionalBlocksColView::addChild);

        // Register to handle MouseRelease, DragGesture
        View uiView = getUI();
        uiView.addEventHandler(this::handleMouseRelease, MouseRelease);
        uiView.addEventHandler(this::handleDragGesture, ViewEvent.Type.DragGesture);
    }

    /**
     * Init showing.
     */
    @Override
    protected void initShowing()
    {
        // Start listening to PagePane.SelFile prop change
        _workspacePane.getSelFileTool().addPropChangeListener(pc -> handleSelFileChange(), SelFileTool.SelFile_Prop);
        handleSelFileChange();
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        // Update ClassText
        JavaClass selClass = getSelClass();
        setViewText("ClassText", selClass != null ? selClass.getSimpleName() : null);
    }

    /**
     * Handle UI changes.
     */
    @Override
    protected void respondUI(ViewEvent anEvent)
    {
        switch (anEvent.getName()) {

            // Handle DirectoryListView
            case "DirectoryListView" -> showBlocksforDirectoryIndex(anEvent.getSelIndex());
        }
    }

    /**
     * Shows the blocks for given DirectoryListView entry index.
     */
    private void showBlocksforDirectoryIndex(int directoryIndex)
    {
        View selView = _allBlocksColView.getChild(directoryIndex);
        Rect selRect = selView.getBoundsLocal();
        selRect.height = selView.getParent(Scroller.class).getHeight();
        selView.scrollToVisible(selRect);
    }

    /**
     * Respond to UI.
     */
    private void handleMouseRelease(ViewEvent anEvent)
    {
        // Handle MouseClick (double click)
//        if (anEvent.isMouseClick() && anEvent.getClickCount() == 2) {
//            JBlockView<?> blockView = getNodeViewForViewAndXY(getUI(ParentView.class), anEvent.getX(), anEvent.getY());
//            if (blockView != null) _javaTextArea.getNodeHpr().replaceNodeWithNode(blockView.getJNode());
//        }
    }

    /**
     * Handle drag gesture event: Get Dragboard and drag shape with image (with DragSourceListener to clear DragShape)
     */
    private void handleDragGesture(ViewEvent anEvent)
    {
        // Get drag node
        JNodeView<?> dragNodeView = JNodeView._dragNodeView = getNodeViewForViewAndXY(getUI(ParentView.class), anEvent.getX(), anEvent.getY());
        if (dragNodeView == null)
            return;

        // Get drag string and image
        JNode dragNode = dragNodeView.getJNode();
        String dragString = dragNode.getString(); // Was: "SupportPane:" + dragSnapPart.getClass().getSimpleName()
        Image dragImage = ViewUtils.getImageForScale(dragNodeView, 1);

        // Create Dragboard, set drag string and image and start drag
        Clipboard clipboard = anEvent.getClipboard();
        clipboard.addData(dragString);
        clipboard.setDragImage(dragImage);
        clipboard.startDrag();
    }

    /**
     * Called when WorkspacePane.SelFileTool.SelFile property changes
     */
    private void handleSelFileChange()
    {
        // Get PagePane JavaPage
        WebPage selPage = _workspacePane.getBrowser().getSelPage();
        JavaPage javaPage = selPage instanceof JavaPage ? (JavaPage) selPage : null;

        // Get JavaPage JavaTextArea and set
        JavaTextArea javaTextArea = javaPage != null ? javaPage.getTextArea() : null;
        setJavaTextArea(javaTextArea);
    }

    /**
     * Called when JavaTextArea.SelNode changes to reset SelClass.
     */
    private void handleJavaTextAreaSelNodeChange()
    {
        // Get selected node
        JNode selNode = _javaTextArea != null ? _javaTextArea.getSelNode() : null;
        JClassDecl classDecl = selNode != null ? selNode.getParent(JClassDecl.class) : null;
        if (classDecl == null && _javaTextArea != null)
            classDecl = _javaTextArea.getJFile().getClassDecl();

        // Get eval class for class decl
        JavaClass selNodeEvalClass = classDecl != null ? classDecl.getEvalClass() : null;
        setSelClass(selNodeEvalClass);
    }

    /**
     * Called when JavaTextArea gets drag events.
     */
    private void handleJavaTextAreaDragEvent(ViewEvent anEvent)  { }

    /**
     * Sets the JavaTextArea associated with text pane.
     */
    private void setJavaTextArea(JavaTextArea javaTextArea)
    {
        if (javaTextArea == _javaTextArea) return;

        // Remove listener from old JavaTextArea
        if (_javaTextArea != null) {
            _javaTextArea.removePropChangeListener(_textAreaPropChangeLsnr);
            _javaTextArea.removeEventHandler(_textAreaDragEventLsnr);
        }

        // Set
        _javaTextArea = javaTextArea;

        // Start listening to JavaTextArea SelNode prop
        if (_javaTextArea != null) {
            _javaTextArea.addPropChangeListener(_textAreaPropChangeLsnr, JavaTextArea.SelNode_Prop);
            _javaTextArea.addEventHandler(_textAreaDragEventLsnr, View.DragEvents);
            handleJavaTextAreaSelNodeChange();
        }
    }

    /**
     * Returns the BlockViews for method invocations for given class.
     */
    private JBlockView<?>[] getBlockViewsForClass(JavaClass aClass)
    {
        String[] stmtStrings = getStatementStringsForClass(aClass);
        return ArrayUtils.map(stmtStrings, stmtStr -> createBlockViewForStatementString(stmtStr), JBlockView.class);
    }

    /**
     * Returns a BlockView for given statement string.
     */
    protected JBlockView<?> createBlockViewForStatementString(String stmtStr)
    {
        // Create statement node for statement string
        JavaParser javaParser = JavaParser.getShared();
        JStmt stmtNode = javaParser.parseStatement(stmtStr, 0);
        stmtNode.setString(stmtStr);

        // Create block view for statement node
        JBlockView<?> blockView = JBlockView.createBlockViewForNode(stmtNode); assert (blockView != null);
        blockView.getEventAdapter().disableEvents(DragEvents);

        // Return
        return blockView;
    }

    /**
     * Returns the child of given class hit by coords.
     */
    protected static JNodeView<?> getNodeViewForViewAndXY(ParentView aParentView, double anX, double aY)
    {
        ViewList childView = aParentView.getChildren();

        // Iterate over children
        for (View child : childView) {

            // If child not visible, skip
            if (!child.isVisible()) continue;

            // Get point in child coords
            Point pointInChildCoords = child.parentToLocal(anX, aY);

            // If view contains point and has NodeView, return NodeView
            if (child.contains(pointInChildCoords.x, pointInChildCoords.y)) {
                 JNodeView<?> nodeView = JNodeView.getNodeView(child);
                 if (nodeView != null)
                     return nodeView;
            }

            // If view is parent, recurse
            if (child instanceof ParentView parView) {
                JNodeView<?> nodeView = getNodeViewForViewAndXY(parView, pointInChildCoords.x, pointInChildCoords.y);
                if (nodeView != null)
                    return nodeView;
            }
        }

        // Return not found
        return null;
    }

    /**
     * Override for title.
     */
    @Override
    public String getTitle()  { return "Blocks"; }

    /**
     * Returns the statement strings for given class.
     */
    private static String[] getStatementStringsForClass(JavaClass javaClass)
    {
        String[] statementStrings = new String[0];

        // Iterate up class hierarchy adding statements strings
        for (JavaClass cls = javaClass; cls != null; cls = cls.getSuperClass()) {
            String[] stmtStrs = getStatementStringsForClassImpl(cls);
            statementStrings = ArrayUtils.addAll(statementStrings, stmtStrs);
        }

        // Return
        return statementStrings;
    }

    /**
     * Returns the statement strings for given class.
     */
    private static String[] getStatementStringsForClassImpl(JavaClass javaClass)
    {
        String simpleName = javaClass.getSimpleName();
        return switch (simpleName) {
            case "Actor" -> ActorPieces;
            case "PenActor" -> PenActorPieces;
            case "GameView" -> GameViewPieces;
            default -> new String[0];
        };
        //else try { strings = (String[])ClassUtils.getMethod(aClass, "getSnapPieces").invoke(null); } catch(Exception e){ }
    }

    /**
     * Returns Actor pieces.
     */
    private static String[] ActorPieces = {
            "moveBy(10);", "turnBy(10);", "scaleBy(.1);",
            "getX();", "getY();", "getWidth();", "getHeight();", "setXY(10,10);", "setSize(50,50);",
            "getRotate();", "setRotate(10);", "getScale();", "setScale(1);",
            "getAngleToMouse();", "getDistance(\"Mouse\");", "isMouseDown();", "isMouseClick();",
            "isKeyDown(\"right\");", "isKeyClicked(\"right\");", "playSound(\"Beep.wav\");", "getGameView();"
    };

    /**
     * Returns PenActor pieces.
     */
    private static String[] PenActorPieces = {
            "isPenDown();", "setPenDown(true);", "penDown();", "clearPen();", "getPenColor();", "setPenColor(\"Random\");",
            "getPenWidth();", "setPenWidth(10);", "getPenPoint();"
    };

    /**
     * Returns GameView pieces.
     */
    private static String[] GameViewPieces = {
            "getWidth();", "getHeight();",
            "isMouseDown();", "isMouseClick();", "getMouseX();", "getMouseY();", "isKeyDown(\"right\");",
            "isKeyClicked(\"right\");", "getActor(\"Cat1\");", "playSound(\"Beep.wav\");",
            "setColor(\"Random\");", "setShowCoords(true);"
    };
}