/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import java.util.List;
import javakit.parse.*;
import javakit.resolver.*;
import snap.geom.Insets;
import snap.gfx.*;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.text.*;
import snap.view.*;

/**
 * A PopupList for an JavaTextPane.
 */
public class JavaPopupList extends PopupList<JavaDecl> {

    // The JavaTextArea
    private JavaTextArea  _textArea;

    // The current selection start
    private int  _selStart;

    // PropChangeListner for TextArea prop changes
    private PropChangeListener  _textAreaLsnr = pce -> textAreaPropChange(pce);

    // Constants
    private static Color BACKGROUND_COLOR = Color.get("#FC");
    private static Color CELL_TEXT_FILL = Color.get("#28");

    /**
     * Creates a new java popup for given JavaTextArea.
     */
    public JavaPopupList(JavaTextArea aJavaTextArea)
    {
        super();

        // Create ListArea and configure style
        ListArea<JavaDecl> listArea = getListArea();
        listArea.setFill(BACKGROUND_COLOR);
        listArea.setAltPaint(BACKGROUND_COLOR);
        listArea.setCellConfigure(listCell -> configureCell(listCell));

        // Configure ListArea sizing
        listArea.setRowHeight(18);
        listArea.setCellPadding(new Insets(0, 2, 2, 2));

        _textArea = aJavaTextArea;
        setPrefWidth(500);
        setPrefRowCount(12);

        // Set font
        TextBlock textBlock = aJavaTextArea.getTextBlock();
        Font font = textBlock.getDefaultStyle().getFont();
        listArea.setFont(font);
    }

    /**
     * Returns the JavaTextArea.
     */
    public JavaTextArea getTextArea()
    {
        return _textArea;
    }

    /**
     * Activates the popup list (shows popup if multiple suggestions, does replace for one, does nothing for none).
     */
    public void activatePopupList()
    {
        // Get suggestions (just return if none)
        JavaDecl[] completions = _textArea.getCompletionsAtCursor();
        if (completions == null || completions.length == 0)
            return;

        // Set completions
        setItems(completions);
        setSelIndex(0);

        // Get location for text start
        TextSel textSel = _textArea.getSel();
        TextLine selLine = textSel.getStartLine();
        int selLineStart = selLine.getStartCharIndex();
        JNode selNode = _textArea.getSelNode();
        int selNodeStart = selNode.getStartCharIndex() - selLineStart;
        if (selNodeStart < 0) // This seems a little bogus
            selNodeStart = selLine.length();

        // Get location for popup and show
        double textX = selLine.getTextXForCharIndex(selNodeStart);
        double textY = selLine.getTextMaxY() + 4;
        show(_textArea, textX, textY);
    }

    /**
     * Handle TextEditor PropertyChange to update Popup Suggestions when SelectedNode changes.
     */
    public void updatePopupList()
    {
        // If not showing, just return
        if (!isShowing())
            return;

        // Get completions (just return if empty)
        JavaDecl[] completions = _textArea.getCompletionsAtCursor();
        if (completions == null || completions.length == 0) {
            hide();
            return;
        }

        // Set completions
        setItems(completions);
        setSelIndex(0);
    }

    /**
     * Applies the current suggestion.
     */
    public void applySuggestion()
    {
        // Get completion and completionString
        JavaDecl completionDecl = getSelItem();
        String completionStr = completionDecl.getReplaceString();

        // Get start/stop char index for completion
        JavaTextArea textArea = getTextArea();
        JNode selNode = textArea.getIdExprAtCursor(); //textArea.getSelNode();
        int nodeStart = selNode.getStartCharIndex();
        int nodeEnd = textArea.getSelEnd();
        if (textArea.charAt(nodeEnd - 1) == '.')
            nodeStart = nodeEnd;

        // Replace selection with completeString
        textArea.replaceChars(completionStr, null, nodeStart, nodeEnd, true);

        // If completion has parens needing content, select inside parens
        int argStart = indexOfParenContent(completionDecl, completionStr);
        if (argStart > 0) {
            int argEnd = completionStr.indexOf(')', argStart);
            if (argEnd > 0)
                textArea.setSel(nodeStart + argStart + 1, nodeStart + argEnd);
        }

        // Add import for suggestion Class, if not present
        addImportForNodeAndDecl(selNode, completionDecl);

        // Hide PopupList
        hide();
    }

    /**
     * Returns whether suggestion has parens that want content.
     */
    private int indexOfParenContent(JavaDecl javaDecl, String completionStr)
    {
        // If no open paren, just return
        int argStart = completionStr.indexOf('(');
        if (argStart < 0)
            return -1;

        // If not executable, just return paren index (probably is "if (...)" or "for (...)" )
        if (!(javaDecl instanceof JavaExecutable))
            return argStart;

        // If method/constructor with non-zero params, return paren index
        JavaExecutable exec = (JavaExecutable) javaDecl;
        if (exec.getParameterCount() > 0)
            return argStart;

        // If constructor
        JavaClass declClass = exec.getDeclaringClass();
        if (exec instanceof JavaConstructor) {
            boolean multipleConstructors = declClass.getDeclaredConstructors().size() > 1;
            return multipleConstructors ? argStart : -1;
        }

        // If more methods with this name
        List<JavaMethod> methods = declClass.getDeclaredMethods();
        JavaMethod javaMethod = (JavaMethod) exec;
        String methodName = javaMethod.getName();
        int count = 0;
        for (JavaMethod method : methods) {
            if (method.getName().equals(methodName)) {
                count++;
                if (count > 1)
                    return argStart;
            }
        }

        // Return no paren content
        return -1;
    }

    /**
     * Adds the import statement for completed decl into text, if missing.
     */
    protected void addImportForNodeAndDecl(JNode aNode, JavaDecl aDecl)
    {
        // Handle JavaClass: Add import for class (unless in import decl)
        if (aDecl instanceof JavaClass) {
            if (aNode.getParent(JImportDecl.class) != null)
                return;
            addImportForNodeAndClass(aNode, (JavaClass) aDecl);
        }

        // Handle JavaConstructor
        if (aDecl instanceof JavaConstructor) {
            JavaClass javaClass = ((JavaConstructor) aDecl).getDeclaringClass();
            if (javaClass != null)
                addImportForNodeAndClass(aNode, javaClass);
        }
    }

    /**
     * Adds the import statement for completed decl into text, if missing.
     */
    protected void addImportForNodeAndClass(JNode aNode, JavaClass aJavaClass)
    {
        // Get JFile
        JFile aFile = aNode.getFile();

        // Get ClassName, SimpleName
        String className = aJavaClass.getClassName();
        String simpleName = aJavaClass.getSimpleName();

        // Get importClassName for simple class name (If already present, just return)
        String importClassName = aFile.getImportClassName(simpleName);
        if (importClassName != null && importClassName.equals(className))
            return;

        // Construct import statement string
        String classPath = className.replace('$', '.');
        String importStr = "import " + classPath + ";\n";

        // Get import line index
        JPackageDecl pkgDecl = aFile.getPackageDecl();
        int importLineIndex = pkgDecl != null ? pkgDecl.getLineIndex() + 1 : 0;

        // Iterate over existing imports and increase line index to insert in alphabetical order
        List<JImportDecl> importDecls = aFile.getImportDecls();
        for (JImportDecl importDecl : importDecls) {
            if (importDecl.getEndCharIndex() == 0) // Skip Jepl default imports
                continue;
            if (classPath.compareTo(importDecl.getName()) < 0)
                break;
            importLineIndex = importDecl.getLineIndex() + 1;
        }

        // Get Java text
        TextArea textArea = getTextArea();
        TextBlock textBlock = textArea.getTextBlock();

        // Add import to Java text
        TextLine textLine = textBlock.getLine(importLineIndex);
        textBlock.addChars(importStr, null, textLine.getStartCharIndex());

        // Update selection
        int selStart = textArea.getSelStart();
        int selEnd = textArea.getSelEnd();
        textArea.setSel(selStart + importStr.length(), selEnd + importStr.length());
    }

    /**
     * Override to register for textArea property change.
     */
    @Override
    public void show(View aView, double aX, double aY)
    {
        // Shift X by image width
        aX -= 24;

        // Do normal version
        super.show(aView, aX, aY);

        // Start listening to TextArea
        _textArea.addPropChangeListener(_textAreaLsnr);
        _selStart = _textArea.getSelStart();
    }

    /**
     * Override to unregister property change.
     */
    @Override
    public void hide()
    {
        super.hide();
        _textArea.removePropChangeListener(_textAreaLsnr);
    }

    /**
     * Override to fire action on tab key.
     */
    @Override
    protected void processPopupListKeyPressEvent(ViewEvent anEvent)
    {
        if (anEvent.isTabKey()) {
            fireActionEvent(anEvent);
            return;
        }

        // Do normal version
        super.processPopupListKeyPressEvent(anEvent);
    }

    /**
     * Catch TextArea Selection changes that should cause Popup to close.
     */
    private void textAreaPropChange(PropChange aPC)
    {
        // If not showing, unregister (in case we were PopupList was dismissed without hide)
        if (!isShowing()) {
            _textArea.removePropChangeListener(_textAreaLsnr);
            return;
        }

        // If Selection change, update or hide
        String propName = aPC.getPropName();
        if (propName == TextArea.Selection_Prop) {
            int start = _textArea.getSelStart();
            int end = _textArea.getSelEnd();
            if (start != end || !(start == _selStart + 1 || start == _selStart - 1))
                hide();
            _selStart = start;
        }
    }

    /**
     * Configure cells for this PopupList.
     */
    @Override
    protected void configureCell(ListCell<JavaDecl> aCell)
    {
        // Get cell item
        JavaDecl item = aCell.getItem();
        if (item == null) return;

        // Get/set cell text
        String cellText = item.getSuggestionString();
        aCell.setText(cellText);
        aCell.setTextFill(CELL_TEXT_FILL);

        // Get/set cell image
        Image cellImage = JavaTextUtils.getImageForJavaDecl(item);
        aCell.setImage(cellImage);
    }

    /**
     * Override to apply suggestion.
     */
    @Override
    protected void fireActionEvent(ViewEvent anEvent)
    {
        applySuggestion();
        anEvent.consume();
    }
}