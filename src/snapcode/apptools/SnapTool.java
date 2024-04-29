package snapcode.apptools;
import javakit.parse.JNode;
import javakit.parse.JavaParser;
import javakit.resolver.JavaClass;
import snap.geom.Point;
import snap.gfx.Image;
import snap.view.*;
import snapcode.app.WorkspacePane;
import snapcode.app.WorkspaceTool;
import snapcode.views.JNodeView;
import snapcode.views.SnapEditor;
import snapcode.views.SnapEditorPane;

/**
 * UI to show puzzle pieces.
 */
public class SnapTool extends WorkspaceTool {

    // The SnapEditorPane
    protected SnapEditorPane _editorPane;

    // The drag image
    //private static Image  _dragImage;

    /**
     * Constructor.
     */
    public SnapTool(WorkspacePane workspacePane)
    {
        super(workspacePane);
    }

    /**
     * Returns the editor.
     */
    public SnapEditor getEditor()  { return _editorPane.getEditor(); }

    /**
     * Create UI.
     */
    protected View createUI()
    {
        TabView tabView = new TabView();
        tabView.setPrefWidth(300);
        tabView.addTab("Methods", createMethodsPane());
        tabView.addTab("Blocks", createBlocksPane());
        tabView.setSelIndex(1);
        return tabView;
    }

    /**
     * Configure UI.
     */
    protected void initUI()
    {
        // Add DragDetected action to start statement drag
        /*getDragUI().setOnDragDetected(e -> {
            JNodeView<?> dragSnapPart = JNodeView._dragSnapPart = getSnapPart(getDragUI(), e.getX(), e.getY()); if(dragSnapPart==null) return;
            javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters(); sp.setFill(Color.TRANSPARENT);
            _dragImage = dragSnapPart.getNative().snapshot(sp, null);
            //JNode copy = javaParser.getJavaStatement(dragSnapPart.getJNode().getString(), 0, 0);
            //dragSnapPart = SnapPart.createSnapPart(copy);
            //Dragboard db = getDragUI().startDragAndDrop(TransferMode.ANY);
            //ClipboardContent cc = new ClipboardContent(); cc.putString("Hello World"); db.setContent(cc);
            //e.consume(); db.setDragView(_dragImage);
        });*/

        enableEvents(getUI(), MouseRelease);

        // Register to handle DragGesture
        getUI().addEventHandler(this::handleDragGesture, ViewEvent.Type.DragGesture);
    }

    /**
     * Respond to UI.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle MouseClick (double click)
        if (anEvent.isMouseClick() && anEvent.getClickCount() == 2) {
            JNodeView<?> part = getNodeViewForViewAndXY(getUI(ParentView.class), anEvent.getX(), anEvent.getY());
            //if (part != null)
            //    getEditorPane().getSelectedPart().dropNode(part.getJNode());
        }
    }

    /**
     * Update tab.
     */
    public void rebuildUI()
    {
        // Get eval class for selected node
        JavaClass selNodeEvalClass = getEditor().getSelNodeEvalClass();

        TabView tabView = getUI(TabView.class);
        Tab methodTab = tabView.getTab(0);
        ScrollView scrollView = (ScrollView) methodTab.getContent();
        ColView colView = (ColView) scrollView.getContent();
        colView.removeChildren();

        // Add pieces for classes
        for (JavaClass cls = selNodeEvalClass; cls != null; cls = cls.getSuperClass())
            addNodeViewsForClass(cls, colView);
    }

    /**
     * Update node views for given class.
     */
    private void addNodeViewsForClass(JavaClass aClass, ChildView aPane)
    {
        // Get statement strings for class - just return if none
        String[] statementStrings = getStatementStringsForClass(aClass);
        if (statementStrings == null)
            return;

        // Iterate over statement string and create/add node view
        for (String statementStr : statementStrings) {
            JNodeView<?> statementView = createNodeViewForStatementString(statementStr);
            aPane.addChild(statementView);
        }
    }

    /**
     * Returns the motion pane.
     */
    private View createMethodsPane()
    {
        // Create vertical box
        ColView colView = new ColView();
        colView.setPadding(20, 20, 20, 20);
        colView.setSpacing(16);
        colView.setGrowWidth(true);
        colView.setGrowHeight(true);
        //colView.setFill(JFileView.BACK_FILL); //pane.setBorder(bevel);

        // Wrap in ScrollView and return
        ScrollView scrollView = new ScrollView(colView);
        scrollView.setPrefWidth(200);
        return scrollView;
    }

    /**
     * Returns the control pane.
     */
    private View createBlocksPane()
    {
        ColView colView = new ColView();
        colView.setPadding(20, 20, 20, 20);
        colView.setSpacing(16);
        colView.setGrowWidth(true);
        colView.setGrowHeight(true);
        //colView.setFill(JFileView.BACK_FILL); //pane.setBorder(bevel);

        // Add node for if(expr)
        JNodeView<?> ifStmtView = createNodeViewForStatementString("if(true) {\n}");
        colView.addChild(ifStmtView);

        // Add node for repeat(x)
        JNodeView<?> forStmtView = createNodeViewForStatementString("for(int i=0; i<10; i++) {\n}");
        colView.addChild(forStmtView);

        // Add node for while(true)
        JNodeView<?> whileStmtView = createNodeViewForStatementString("while(true) {\n}");
        colView.addChild(whileStmtView);

        // Wrap in ScrollView and return
        ScrollView scrollView = new ScrollView(colView);
        scrollView.setPrefWidth(200);
        return scrollView;
    }

    /**
     * Returns a NodeView for given string of code.
     */
    protected JNodeView<?> createNodeViewForStatementString(String aString)
    {
        JavaParser javaParser = JavaParser.getShared();
        JNode node = javaParser.parseStatement(aString, 0);
        node.setString(aString);
        JNodeView<?> nodeView = JNodeView.createNodeViewForNode(node);
        nodeView.getEventAdapter().disableEvents(DragEvents);
        return nodeView;
    }

    /**
     * Returns the child of given class hit by coords.
     */
    protected JNodeView<?> getNodeViewForViewAndXY(ParentView aParentView, double anX, double aY)
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
                 JNodeView nodeView = JNodeView.getJNodeView(child);
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
            default: return null;
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