package snapcode.apptools;
import javakit.parse.JNode;
import javakit.parse.JavaParser;
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
            JNodeView<?> part = getSnapPart(getUI(ParentView.class), anEvent.getX(), anEvent.getY());
            //if (part != null)
            //    getEditorPane().getSelectedPart().dropNode(part.getJNode());
        }
    }

    /**
     * Update tab.
     */
    public void rebuildUI()
    {
        // Get class for SnapPart.JNode
        Class<?> cls = getEditor().getSelectedPartEnclClass();

        TabView tabView = getUI(TabView.class);
        Tab methodTab = tabView.getTab(0);
        ScrollView scrollView = (ScrollView) methodTab.getContent();
        ColView colView = (ColView) scrollView.getContent();
        colView.removeChildren();

        // Add pieces for classes
        for (Class<?> c = cls; c != null; c = c.getSuperclass())
            updateTabView(c, colView);
    }

    public void updateTabView(Class<?> aClass, ChildView aPane)
    {
        String[] strings = null;
        String simpleName = aClass.getSimpleName();
        if (simpleName.equals("SnapActor")) strings = SnapActorPieces;
        else if (simpleName.equals("SnapPen")) strings = SnapPenPieces;
        else if (simpleName.equals("SnapScene")) strings = SnapScenePieces;
        //else try { strings = (String[])ClassUtils.getMethod(aClass, "getSnapPieces").invoke(null); } catch(Exception e){ }
        if (strings == null)
            return;

        for (String str : strings) {
            JNodeView<?> move = createSnapPartStmt(str);
            aPane.addChild(move);
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
        JNodeView<?> ifStmtView = createSnapPartStmt("if(true) {\n}");
        colView.addChild(ifStmtView);

        // Add node for repeat(x)
        JNodeView<?> forStmtView = createSnapPartStmt("for(int i=0; i<10; i++) {\n}");
        colView.addChild(forStmtView);

        // Add node for while(true)
        JNodeView<?> whileStmtView = createSnapPartStmt("while(true) {\n}");
        colView.addChild(whileStmtView);

        // Wrap in ScrollView and return
        ScrollView scrollView = new ScrollView(colView);
        scrollView.setPrefWidth(200);
        return scrollView;
    }

    /**
     * Returns a SnapPart for given string of code.
     */
    protected JNodeView<?> createSnapPartStmt(String aString)
    {
        JavaParser javaParser = JavaParser.getShared();
        JNode node = javaParser.parseStatement(aString, 0);
        node.setString(aString);
        JNodeView<?> nodeView = JNodeView.createView(node);
        nodeView.getEventAdapter().disableEvents(DragEvents);
        return nodeView;
    }

    /**
     * Returns the child of given class hit by coords.
     */
    protected JNodeView<?> getSnapPart(ParentView aPar, double anX, double aY)
    {
        for (View child : aPar.getChildren()) {
            if (!child.isVisible()) continue;
            Point p = child.parentToLocal(anX, aY);
            if (child.contains(p.getX(), p.getY()) && JNodeView.getJNodeView(child) != null)
                return JNodeView.getJNodeView(child);
            if (child instanceof ParentView) {
                ParentView par = (ParentView) child;
                JNodeView<?> no = getSnapPart(par, p.getX(), p.getY());
                if (no != null)
                    return no;
            }
        }
        return null;
    }

    /**
     * Handle drag gesture event: Get Dragboard and drag shape with image (with DragSourceListener to clear DragShape)
     */
    private void handleDragGesture(ViewEvent anEvent)
    {
        // Get drag node
        JNodeView<?> dragSnapPart = JNodeView._dragSnapPart = getSnapPart(getUI(ParentView.class), anEvent.getX(), anEvent.getY());
        if (dragSnapPart == null)
            return;

        // Get drag string and image
        JNode dragNode = dragSnapPart.getJNode();
        String dragString = dragNode.getString(); // Was: "SupportPane:" + dragSnapPart.getClass().getSimpleName()
        Image dragImage = ViewUtils.getImage(dragSnapPart);

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
     * Returns SnapActor pieces.
     */
    private static String[] SnapActorPieces = {
            "moveBy(10);", "turnBy(10);", "scaleBy(.1);",
            "getX();", "getY();", "getWidth();", "getHeight();", "setXY(10,10);", "setSize(50,50);",
            "getRotate();", "setRotate(10);", "getScale();", "setScale(1);",
            "getAngle(\"Mouse\");", "getDistance(\"Mouse\");", "isMouseDown();", "isMouseClick();",
            "isKeyDown(\"right\");", "isKeyClicked(\"right\");", "playSound(\"Beep.wav\");", "getScene();",
            "getPen();", "setPenColor(\"Random\");", "penDown();", "getAnimator();"
    };

    /**
     * Returns SnapPen pieces.
     */
    private static String[] SnapPenPieces = { "down();", "up();", "clear();", "setColor(\"Random\");", "setWidth(10);" };

    /**
     * Returns SnapScene pieces.
     */
    private static String[] SnapScenePieces = {
            "getWidth();", "getHeight();",
            "isMouseDown();", "isMouseClick();", "getMouseX();", "getMouseY();", "isKeyDown(\"right\");",
            "isKeyClicked(\"right\");", "getActor(\"Cat1\");", "playSound(\"Beep.wav\");",
            "setColor(\"Random\");", "setShowCoords(true);"
    };
}