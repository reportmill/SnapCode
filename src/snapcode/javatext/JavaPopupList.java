/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.javatext;
import java.util.List;
import javakit.parse.*;
import javakit.resolver.*;
import snap.geom.Insets;
import snap.gfx.*;
import snap.parse.ParseToken;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.text.*;
import snap.util.StringUtils;
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
        listArea.setCellPadding(new Insets(0, 2, 2, 2)); //listArea.setRowHeight(18);

        _textArea = aJavaTextArea;
        setPrefWidth(500);
        setPrefRowCount(12);

        // Set font
        TextBlock textBlock = aJavaTextArea.getTextBlock();
        Font font = textBlock.getDefaultFont();
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
     * Update JavaPopupList (show, hide or update completions) for TextArea KeyReleased event.
     */
    public void updateForTextAreaKeyReleasedEvent(ViewEvent anEvent)
    {
        // If shift key or up/down arrows, just ignore
        int keyCode = anEvent.getKeyCode();
        if (keyCode == KeyCode.SHIFT || keyCode == KeyCode.UP || keyCode == KeyCode.DOWN)
            return;

        // If Java identifier char or backspace, show/update popup list
        if (isUpdateCompletionsEvent(anEvent))
            ViewUtils.runLater(this::updatePopupList);

        // Otherwise if Showing, hide
        else if (isShowing())
            hide();
    }

    /**
     * Returns whether key released event is update event.
     */
    private boolean isUpdateCompletionsEvent(ViewEvent anEvent)
    {
        // If backspace event, return true if already showing
        if (anEvent.isBackSpaceKey())
            return isShowing();

        // If shortcut or control, return false
        if (anEvent.isShortcutDown() || anEvent.isControlDown() || anEvent.isEscapeKey())
            return false;

        // If java identifier char, return true
        char keyChar = anEvent.getKeyChar();
        if (Character.isJavaIdentifierPart(keyChar) || keyChar == '.')
            return true;

        // Return false
        return false;
    }

    /**
     * Updates popup list if completions found at text area cursor.
     */
    private void updatePopupList()
    {
        // Get completions (just return if empty)
        JavaDecl[] completions = getCompletionsAtCursor();
        if (completions == null || completions.length == 0) {
            hide();
            return;
        }

        // Set completions
        setItems(completions);
        setSelIndex(0);

        // If not showing, show popup list
        if (!isShowing())
            showPopupList();
    }

    /**
     * Shows the popup list if completions found at text area cursor.
     */
    private void showPopupList()
    {
        // Get start char index for completion node
        JExprId selNode = getIdExprAtCursor();
        String selNodeStr = selNode.getName();
        int selStart = _textArea.getSelStart();
        int nodeStart = selStart - selNodeStr.length();

        // Get selected line and start char index of completion
        TextLine selLine = _textArea.getSel().getStartLine();
        int selLineStart = selLine.getStartCharIndex();
        int nodeStartInLine = nodeStart - selLineStart;
        if (nodeStartInLine < 0)
            return;

        // Get location for popup and show
        double textX = selLine.getTextXForCharIndex(nodeStartInLine);
        double textY = selLine.getTextMaxY() + 4;
        show(_textArea, textX, textY);
    }

    /**
     * Returns completions for current text selection.
     */
    public JavaDecl[] getCompletionsAtCursor()
    {
        // Get id expression at cursor (just return if none)
        JExprId idExpr = getIdExprAtCursor();
        if (idExpr == null)
            return null;

        // Get completions and return
        NodeCompleter javaCompleter = new NodeCompleter();
        JavaDecl[] completions = javaCompleter.getCompletionsForId(idExpr);
        return completions;
    }

    /**
     * Returns the id expression at cursor, if available.
     */
    protected JExprId getIdExprAtCursor()
    {
        // If selection not empty, just return
        if (!_textArea.isSelEmpty())
            return null;

        // Get SelNode - just return if bogus
        int selStart = _textArea.getSelStart();
        JNode selNode = _textArea.getSelNode();
        if (selNode == null)
            return null;

        // If no class decl, just bail
        JFile jfile = selNode.getFile();
        if (jfile.getClassDecl() == null)
            return null;

        // If dot at cursor, get virtual id expression for empty string after dot
        JExprId virtualIdExprForDot = getVirtualIdExprForDot();
        if (virtualIdExprForDot != null)
            return virtualIdExprForDot;

        // Get selected id expression (just return if SelNode not id)
        JExprId selId = selNode instanceof JExprId ? (JExprId) selNode : null;
        if (selId == null)
            return getVirtualIdExprForTextTokenAtCursor();

        // If cursor not at end of expression, get virtual id expression for prefix
        int idEnd = selId.getEndCharIndex();
        if (selStart != idEnd)
            selId = getVirtualIdExprForPrefix(selId);

        // Return
        return selId;
    }

    /**
     * If previous character is a dot proceeded by id, creates and returns an empty id expression (with parent dot).
     */
    private JExprId getVirtualIdExprForDot()
    {
        // If previous char not dot or ::, just return
        TextBlock textBlock = _textArea.getTextBlock();
        int prevCharIndex = _textArea.getSelStart() - 1;
        char prevChar = prevCharIndex > 1 ? textBlock.charAt(prevCharIndex) : 0;
        if (prevChar == ':')
            prevChar = textBlock.charAt(--prevCharIndex);
        if (prevChar != '.' && prevChar != ':')
            return null;

        // Get previous expression - if null, just return
        JNode prevNode = _textArea.getNodeForCharIndex(prevCharIndex);
        JExpr prefixExpr = prevNode instanceof JExpr ? (JExpr) prevNode : null;
        if (prefixExpr == null)
            return null;

        // Create new id expression with empty string
        ParseToken prevToken = prefixExpr.getEndToken();
        JExprId virtualIdExpr = new JExprId("");
        virtualIdExpr.setStartToken(prevToken);
        virtualIdExpr.setEndToken(prevToken);

        // Create new dot expression for prefix expression and new id, set parent to prefixExpr and reset prefixExpr parent
        JNode prefixExprParent = prefixExpr.getParent();
        JExpr newDotExpr = prevChar == '.' ? new JExprDot(prefixExpr, virtualIdExpr) : new JExprMethodRef(prefixExpr, virtualIdExpr);
        newDotExpr.setParent(prefixExpr);
        prefixExpr.setParent(prefixExprParent);

        // Return
        return virtualIdExpr;
    }

    /**
     * Returns a new id expression from text token at selection, if string is valid java identifier.
     */
    private JExprId getVirtualIdExprForTextTokenAtCursor()
    {
        // Get token for SelStart - just return if none
        TextBlock textBlock = _textArea.getTextBlock();
        int selStart = _textArea.getSelStart();
        TextToken textToken = selStart > 0 ? textBlock.getTokenForCharIndex(selStart - 1) : null;
        if (textToken == null)
            return null;

        // Get token string - just return if not valid java identifier
        String tokenStr = textToken.getString();
        if (!isJavaIdentifier(tokenStr))
            return null;

        // Create virtual id expression
        JExprId virtualIdExpr = new JExprId(tokenStr);
        virtualIdExpr.setStartToken(textToken);
        virtualIdExpr.setEndToken(textToken);
        JNode selNode = _textArea.getSelNode();
        virtualIdExpr.setParent(selNode);

        // Return
        return virtualIdExpr;
    }

    /**
     * Returns a new id expression that is a prefix of given id to given char count.
     */
    private JExprId getVirtualIdExprForPrefix(JExprId idExpr)
    {
        // Get prefix string for new id expression
        ParseToken startToken = idExpr.getStartToken();
        String fullIdString = idExpr.getName();
        int selStart = _textArea.getSelStart();
        int idStart = idExpr.getStartCharIndex();
        int charCount = selStart - idStart;
        String prefixString = fullIdString.substring(0, charCount);

        // Create new id expression with empty string
        JExprId virtualIdExpr = new JExprId(prefixString);
        virtualIdExpr.setStartToken(startToken);
        virtualIdExpr.setEndToken(startToken);
        virtualIdExpr.setParent(idExpr.getParent());

        // If id node is the tail of a dot expression, create new dot expression parent for new id
        JNode idNode = idExpr;
        if (idNode.getParent() instanceof JExprMethodCall)
            idNode = idNode.getParent();
        if (idNode.getParent() instanceof JExprDot) {
            JExprDot dotExpr = (JExprDot) idNode.getParent();
            if (idNode == dotExpr.getExpr()) {
                JExpr prefixExpr = dotExpr.getPrefixExpr();
                JExpr newDotExpr = new JExprDot(prefixExpr, virtualIdExpr);
                newDotExpr.setParent(dotExpr.getParent());
                prefixExpr.setParent(dotExpr);
            }
        }

        // Return
        return virtualIdExpr;
    }

    /**
     * Applies the current suggestion.
     */
    private void applySuggestion()
    {
        // Get jnode and completion decl
        JExprId selNode = getIdExprAtCursor();
        JavaDecl completionDecl = getSelItem();

        // Handle body decl
        if (NodeCompleter.isBodyDeclId(selNode) && completionDecl instanceof JavaExecutable)
            applySuggestionForBodyDecl(selNode, (JavaExecutable) completionDecl);

        // Otherwise, general
        else applySuggestionGeneral(selNode, completionDecl);

        // Hide PopupList
        hide();
    }

    /**
     * Applies a general suggestion.
     */
    private void applySuggestionGeneral(JExprId selNode, JavaDecl completionDecl)
    {
        // Get replace start and end
        String selNodeStr = selNode.getName();
        int replaceEnd = _textArea.getSelStart();
        int replaceStart = replaceEnd - selNodeStr.length();

        // Get completion and completionString
        String completionStr = completionDecl.getReplaceString();

        // If method ref, just use name
        if (selNode.getParent() instanceof JExprMethodRef)
            completionStr = completionDecl.getSimpleName();

        // Replace selection with completeString
        _textArea.replaceChars(completionStr, null, replaceStart, replaceEnd, true);

        // If completion has parens needing content, select inside parens
        int argStart = indexOfParenContent(completionDecl, completionStr);
        if (argStart > 0) {
            int argEnd = completionStr.indexOf(')', argStart);
            if (argEnd > 0)
                _textArea.setSel(replaceStart + argStart + 1, replaceStart + argEnd);
        }

        // Add import for suggestion Class, if not present
        addImportForNodeAndDecl(selNode, completionDecl);
    }

    /**
     * Applies a body decl suggestion.
     */
    private void applySuggestionForBodyDecl(JExprId selNode, JavaExecutable completionDecl)
    {
        // Get method decl and the replace start/end
        JMethodDecl methodDecl = selNode.getParent(JMethodDecl.class);
        int replaceStart = methodDecl.getStartCharIndex();
        int replaceEnd = methodDecl.getEndCharIndex();

        // Get completionString
        String methodDeclStr = completionDecl.getDeclarationString();

        // Add method body: { return super.methodCall(params); }
        String indentStr = _textArea.getSel().getStartLine().getIndentString();
        String completionStr = "@Override\n" + indentStr + methodDeclStr;
        String superStr = getSuperCallStringForMethod(completionDecl);
        completionStr += '\n' + indentStr + "{\n" + indentStr + "    ";
        int superStrIndex = replaceStart + completionStr.length();
        completionStr += superStr + '\n' + indentStr + "}\n";

        // Replace selection with completeString
        _textArea.replaceChars(completionStr, null, replaceStart, replaceEnd, false);
        _textArea.setSel(superStrIndex, superStrIndex + superStr.length());

        // Add imports for method return type and param types
        if (completionDecl instanceof JavaMethod) {
            JavaClass returnType = ((JavaMethod) completionDecl).getReturnType();
            if (!returnType.isPrimitive())
                addImportForNodeAndClass(selNode, returnType);
            JavaClass[] paramClasses = completionDecl.getParameterClasses();
            for (JavaClass paramClass : paramClasses)
                addImportForNodeAndClass(selNode, paramClass);
        }
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
            boolean multipleConstructors = declClass.getDeclaredConstructors().length > 1;
            return multipleConstructors ? argStart : -1;
        }

        // If more methods with this name
        JavaMethod[] methods = declClass.getDeclaredMethods();
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

            // If in source, just return
            JavaClass javaClass = (JavaClass) aDecl;
            if (javaClass.isFromSource())
                return;

            // If in import statement, just return
            if (aNode.getParent(JImportDecl.class) != null)
                return;

            // Add import
            addImportForNodeAndClass(aNode, javaClass);
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
        textBlock.addChars(importStr, textLine.getStartCharIndex());

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
     * Override to remove listener and trigger syntax check
     */
    @Override
    protected void setShowing(boolean aValue)
    {
        // Do normal version
        if (aValue == isShowing()) return;
        super.setShowing(aValue);

        // On hide, remove TextArea prop change listener and do syntax check
        if (!aValue) {
            _textArea.removePropChangeListener(_textAreaLsnr);
            JavaTextPane javaTextPane = _textArea.getOwner(JavaTextPane.class);
            if (javaTextPane != null)
                javaTextPane.checkFileForErrorsAfterDelay();
        }
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

    /**
     * Returns whether string is valid java identifier.
     */
    private static boolean isJavaIdentifier(String aString)
    {
        if (aString == null || aString.length() == 0)
            return false;
        if (!Character.isJavaIdentifierStart(aString.charAt(0)))
            return false;
        for (int i = 1; i < aString.length(); i++)
            if (!Character.isJavaIdentifierPart(aString.charAt(i)))
                return false;
        return true;
    }

    /**
     * Returns a string for a super method call to given method call (e.g.: "return super.methodCall(arg0, arg1, ...);"
     */
    private static String getSuperCallStringForMethod(JavaExecutable completionDecl)
    {
        String paramNamesStr = '(' + StringUtils.join(completionDecl.getParameterNames(), ", ") + ')';
        String superStr = "super." + completionDecl.getSimpleName() + paramNamesStr + ';';
        if (!completionDecl.getEvalClass().getName().equals("void"))
            superStr = "return " + superStr;
        return superStr;
    }
}