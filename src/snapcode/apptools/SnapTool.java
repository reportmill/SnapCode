package snapcode.apptools;
import javakit.parse.JNode;
import javakit.parse.JStmt;
import javakit.parse.JavaParser;
import javakit.resolver.JavaClass;
import snap.geom.Point;
import snap.gfx.Image;
import snap.util.ArrayUtils;
import snap.view.*;
import snapcode.app.WorkspacePane;
import snapcode.views.JBlockView;
import snapcode.views.JNodeView;
import java.util.stream.Stream;

/**
 * UI to show puzzle pieces.
 */
public class SnapTool extends SnippetTool.ChildTool {

    // The ColView to hold Method blocks
    private ColView _methodBlocksColView;

    // The ColView to hold conditional blocks
    private ColView _conditionalBlocksColView;

    // The selected class
    private JavaClass _selClass;

    /**
     * Constructor.
     */
    public SnapTool(WorkspacePane workspacePane)
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

        // Remove existing method blocks
        _methodBlocksColView.removeChildren();

        // Get blockView for SelClass and add to blocksColView
        JBlockView<?>[] blockViews = getBlockViewsForClass(_selClass);
        Stream.of(blockViews).forEach(_methodBlocksColView::addChild);
    }

    /**
     * Sets CodeBlocks for current TextArea.SelectedNode.
     */
    public void resetSelClassFromJavaTextArea()
    {
        // Get selected node
        JNode selNode = _javaTextArea != null ? _javaTextArea.getSelNode() : null;
        while (selNode != null && selNode.getEvalClass() == null)
            selNode = selNode.getParent();

        // Get eval class for selected node
        JavaClass selNodeEvalClass = selNode != null ? selNode.getEvalClass() : null;
        setSelClass(selNodeEvalClass);
    }

    /**
     * Create UI.
     */
    protected View createUI()
    {
        // Create method blocks ColView
        _methodBlocksColView = new ColView();
        _methodBlocksColView.setPadding(20, 20, 20, 20);
        _methodBlocksColView.setSpacing(16);
        _methodBlocksColView.setGrowWidth(true);
        _methodBlocksColView.setGrowHeight(true);

        // Create method blocks ScrollView
        ScrollView methodBlocksScrollView = new ScrollView(_methodBlocksColView);
        methodBlocksScrollView.setPrefWidth(200);

        _conditionalBlocksColView = new ColView();
        _conditionalBlocksColView.setPadding(20, 20, 20, 20);
        _conditionalBlocksColView.setSpacing(16);
        _conditionalBlocksColView.setGrowWidth(true);
        _conditionalBlocksColView.setGrowHeight(true);

        // Wrap in ScrollView and return
        ScrollView conditionalBlocksScrollView = new ScrollView(_conditionalBlocksColView);
        conditionalBlocksScrollView.setPrefWidth(200);

        // Create TabView to hold method blocks and conditional blocks
        TabView tabView = new TabView();
        tabView.setPrefWidth(300);
        tabView.addTab("Methods", methodBlocksScrollView);
        tabView.addTab("Blocks", conditionalBlocksScrollView);
        tabView.setSelIndex(1);

        // Return
        return tabView;
    }

    /**
     * Configure UI.
     */
    protected void initUI()
    {
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
        Image dragImage = ViewUtils.getImage(dragNodeView);

        // Create Dragboard, set drag string and image and start drag
        Clipboard clipboard = anEvent.getClipboard();
        clipboard.addData(dragString);
        clipboard.setDragImage(dragImage);
        clipboard.startDrag();
    }

    /**
     * Called when JavaTextArea.SelNode changes.
     */
    @Override
    protected void javaTextAreaSelNodeChanged()
    {
        resetSelClassFromJavaTextArea();
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
        View[] childView = aParentView.getChildren();

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
            if (child instanceof ParentView) {
                ParentView parView = (ParentView) child;
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
    public String getTitle()  { return "Snippets"; }

    /**
     * Returns the statement strings for given class.
     */
    private static String[] getStatementStringsForClass(JavaClass javaClass)
    {
        String simpleName = javaClass.getSimpleName();
        switch (simpleName) {
            case "GameActor": return GameActorPieces;
            case "GamePen": return GamePenPieces;
            case "GameView": return GameViewPieces;
            default: return new String[0];
        }
        //else try { strings = (String[])ClassUtils.getMethod(aClass, "getSnapPieces").invoke(null); } catch(Exception e){ }
    }

    /**
     * Returns GameActor pieces.
     */
    private static String[] GameActorPieces = {
            "moveBy(10);", "turnBy(10);", "scaleBy(.1);",
            "getX();", "getY();", "getWidth();", "getHeight();", "setXY(10,10);", "setSize(50,50);",
            "getRotate();", "setRotate(10);", "getScale();", "setScale(1);",
            "getAngle(\"Mouse\");", "getDistance(\"Mouse\");", "isMouseDown();", "isMouseClick();",
            "isKeyDown(\"right\");", "isKeyClicked(\"right\");", "playSound(\"Beep.wav\");", "getScene();",
            "getPen();", "setPenColor(\"Random\");", "penDown();", "getAnimator();"
    };

    /**
     * Returns GamePen pieces.
     */
    private static String[] GamePenPieces = { "down();", "up();", "clear();", "setColor(\"Random\");", "setWidth(10);" };

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