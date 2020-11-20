/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package javakit.text;
import java.util.*;
import javakit.parse.*;
import snap.geom.*;
import snap.gfx.*;
import snap.text.*;
import snap.project.*;
import snap.util.PropChange;
import snap.view.*;
import snap.web.WebFile;

/**
 * A TextArea subclass for Java source editing.
 */
public class JavaTextArea extends TextArea {

    // The max column
    int                    _printMarginColumn = 120;
    
    // Whether to draw line for print margin column
    boolean                _showPrintMargin = true;

    // The default font
    Font                   _defaultFont;
    
    // The selected JNode
    JNode _selNode = new JFile(), _deepNode;
    
    // The node that the mouse is hoving over (if command is down)
    JNode                  _hoverNode;

    // The list of selected tokens
    List <TextBoxToken>    _tokens = new ArrayList();
    
    // A PopupList to show code completion stuff
    JavaPopupList          _popup;
    
    // The TextPane
    JavaTextPane           _textPane;
    
    // The code builder
    CodeBuilder            _codeBuilder;
    
    // The RowHeader
    RowHeader              _rowHeader;
    
    // The OverviewPane
    OverviewPane           _overviewPane;

    /**
     * Creates a new JavaTextArea.
     */
    public JavaTextArea()  { setFill(Color.WHITE); setEditable(true); }

    /**
     * Returns the Java text pane.
     */
    public JavaTextPane getTextPane()  { return _textPane; }

    /**
     * Override to return text as JavaText.
     */
    public JavaTextBox getTextBox()  { return (JavaTextBox)super.getTextBox(); }

    /**
     * Override to create JavaText.
     */
    protected TextBox createTextBox()  { return new JavaTextBox(); }

    /**
     * Returns the code completion popup.
     */
    public JavaPopupList getPopup()  { return _popup!=null? _popup : (_popup = new JavaPopupList(this)); }

    /**
     * Activates the popup list (shows popup if multiple suggestions, does replace for one, does nothing for none).
     */
    public void activatePopupList()
    {
        // Get suggestions
        JNode selectedNode = getSelectedNode();
        JavaDecl sugs[] = new JavaCompleter().getSuggestions(selectedNode);
        if(sugs.length==0) return; // || !doReplace && !isVariableFieldOrMethod(suggestions[0]))

        // If one suggestion and doReplace, perform replace
        //if(sugs.length==1) { setSel(selectedNode.getStart(), getSelEnd()); replaceChars(sugs[0].getReplaceString()); }
        // else {     ... <stuff_below> ...   }

        // If multiple suggestions
        JavaPopupList popup = getPopup(); popup.setItems(sugs);
        TextBoxLine line = getSel().getStartLine();
        double x = line.getXForChar(getSelStart() - line.getStart()), y = line.getMaxY() + 3;
        popup.show(this, x, y);
    }

    /**
     * Handle TextEditor PropertyChange to update Popup Suggestions when SelectedNode changes.
     */
    public void updatePopupList()
    {
        // If Java Popup is visible, get new suggestions and set
        if(getPopup().isShowing()) {
            JNode node = getSelectedNode(); boolean atEnd = isSelEmpty() && getSelStart()==node.getEnd();
            JavaDecl sugs[] = atEnd? new JavaCompleter().getSuggestions(node) : null;
            if(sugs!=null && sugs.length>0) getPopup().setItems(sugs);
            else getPopup().hide();
        }

        // If CodeBuilder Visible, update CodeBlocks
        if(getCodeBuilder().isVisible())
            getCodeBuilder().setCodeBlocks();
    }

    /**
     * Returns the CodeBuilder.
     */
    public CodeBuilder getCodeBuilder()  { return _codeBuilder!=null? _codeBuilder : (_codeBuilder=new CodeBuilder(this)); }

    /**
     * Returns the max column index that text should not exceed.
     */
    public int getPrintMarginColumn()  { return _printMarginColumn; }

    /**
     * Returns whether to draw line for print margin column.
     */
    public boolean getShowPrintMargin()  { return _showPrintMargin; }

    /**
     * Selects a given line number.
     */
    public void selectLine(int anIndex)
    {
        TextBoxLine textLine = anIndex>=0 && anIndex<getLineCount()? getLine(anIndex) : null;
        if(textLine!=null)
            setSel(textLine.getStart(), textLine.getEnd());
    }

    /**
     * Returns the default font.
     */
    public Font getDefaultFont()
    {
        if(_defaultFont==null) {
            String names[] = { "Monaco", "Consolas", "Courier" };
            for(int i=0; i<names.length; i++) {
                _defaultFont = new Font(names[i], 10);
                if(_defaultFont.getFamily().startsWith(names[i]))
                    break;
            }
        }
        return _defaultFont;
    }

    /**
     * Returns the JFile (parsed representation of Java file).
     */
    public JFile getJFile()  { return getTextBox().getJFile(); }

    /**
     * Override to update selected node and tokens.
     */
    public void setSel(int aStart, int anEnd)
    {
        super.setSel(aStart, anEnd);
        JNode node = getJFile().getNodeAtCharIndex(getSelStart(), getSelEnd());
        setSelectedNode(node);
    }

    /**
     * Returns the selected JNode.
     */
    public JNode getSelectedNode()  { return _selNode; }

    /**
     * Sets the selected JNode.
     */
    public void setSelectedNode(JNode aNode)
    {
        // If new node, set, update tokens and repaint
        if(aNode!=getSelectedNode()) {
            _selNode = _deepNode = aNode;
            setSelectedTokensForNode(aNode);
            repaintAll();
        }

        // Update SelectedNode
        updatePopupList();
        JavaTextPane tp = getTextPane(); if(tp!=null) tp.resetLater();
    }

    /**
     * Returns the class name for the currently selected JNode.
     */
    public Class getSelectedNodeClass()  { return _selNode!=null? _selNode.getEvalClass() : null; }

    /**
     * Returns the list of selected tokens.
     */
    public List <TextBoxToken> getSelectedTokens()  { return _tokens; }

    /**
     * Sets the list of selected tokens.
     */
    protected void setSelectedTokens(List<TextBoxToken> theTkns) { _tokens.clear(); _tokens.addAll(theTkns); }

    /**
     * Sets the list of selected tokens (should be in background).
     */
    protected void setSelectedTokensForNode(JNode aNode)
    {
        // Create list for tokens
        List <TextBoxToken> tokens = new ArrayList();

        // If node is JType, select all of them
        JavaDecl decl = aNode!=null? aNode.getDecl() : null;
        if(decl!=null) {
            List <JNode> others = new ArrayList();
            JavaDeclOwner.getMatches(aNode.getFile(), decl, others);
            for(JNode other : others) {
                TextBoxToken tt = (TextBoxToken)other.getStartToken();
                tokens.add(tt);
            }
        }

        // Set tokens
        setSelectedTokens(tokens);
    }

    /**
     * Returns the node under the mouse (if command is down).
     */
    public JNode getHoverNode()  { return _hoverNode; }

    /**
     * Sets the node under the mouse (if command is down).
     */
    public void setHoverNode(JNode aNode)  { _hoverNode = aNode; repaint(); }

    /**
     * Sets the text that is to be edited.
     */
    public void setText(String aString)  { super.setText(aString); repaintAll(); }

    /**
     * Replaces the current selection with the given string.
     */
    public void replaceChars(String aStr, TextStyle aStyle, int aStart, int anEnd, boolean doUpdateSel)
    {
        super.replaceChars(aStr, aStyle, aStart, anEnd, doUpdateSel); repaintAll();
    }

    /**
     * Repaint text and reset/repaint Overview/RowHeader.
     */
    public void repaintAll()
    {
        repaint();
        if(_overviewPane!=null) _overviewPane.resetAll();
        if(_rowHeader!=null) _rowHeader.resetAll();
    }

    /**
     * Override to draw print margin.
     */
    protected void paintBack(Painter aPntr)
    {
        // Do normal version
        super.paintBack(aPntr);

        // Configure MarginLine
        if(getShowPrintMargin()) {
            double x = getPadding().getLeft() + getFont().charAdvance('X')*120 + .5;
            aPntr.setColor(Color.LIGHTGRAY); aPntr.setStroke(Stroke.Stroke1); aPntr.drawLine(x,0,x,getHeight());
        }

        // Underline build issues
        BuildIssue issues[] = getBuildIssues();
        for(BuildIssue issue : issues) {
            int istart = issue.getStart(), iend = issue.getEnd(); if(iend<istart || iend>length()) continue;
            TextBoxLine line = getLineAt(iend); int lstart = line.getStart(); if(istart<lstart) istart = lstart;
            TextBoxToken token = getTokenAt(istart);
            if(token!=null) { int tend = token.getLine().getStart()+token.getEnd(); if(iend<tend) iend = tend; }
            if(istart==iend && iend<line.getEnd()) iend++; // If possible, make sure we underline at least one char
            int yb = (int)Math.round(line.getBaseline()) + 2;
            double x1 = line.getXForChar(istart - lstart), x2 = line.getXForChar(iend - lstart);
            Line inode = new Line(x1, yb, x2, yb);
            aPntr.setPaint(issue.isError()? Color.RED : new Color(244,198,60));
            aPntr.setStroke(Stroke.StrokeDash1); aPntr.drawLine(x1, yb, x2, yb); aPntr.setStroke(Stroke.Stroke1);
        }

        // Add box around balancing bracket
        if(getSel().getSize()<2) {
            int ind = getSelStart(), ind2 = -1; char c1 = ind>0? charAt(ind-1) : 0, c2 = ind<length()? charAt(ind) : 0;
            if(c2=='{' || c2=='}') {    // || c2=='(' || c2==')'
                JNode jnode = getJFile().getNodeAtCharIndex(ind);
                ind2 = c2=='}' || c2==')'? jnode.getStart() : jnode.getEnd()-1;
                if(ind2+1>length()) { System.err.println("JavaTextArea.paintBack: Invalid-A " + ind2); ind2 = -1; }
            }
            else if(c1=='{' || c1=='}') {  //  || c1=='(' || c1==')'
                JNode jnode = getJFile().getNodeAtCharIndex(ind-1);
                ind2 = c1=='}' || c1==')'? jnode.getStart() : jnode.getEnd()-1;
                if(ind2+1>length()) { System.err.println("JavaTextArea.paintBack: Invalid-B" + ind2); ind2 = -1; }
            }
            if(ind2>=0) {
                TextBoxLine line = getLineAt(ind2); int s1 = ind2-line.getStart(), s2 = ind2+1-line.getStart();
                double x1 = line.getXForChar(s1), x2 = line.getXForChar(s2);
                aPntr.setColor(Color.LIGHTGRAY); aPntr.drawRect(x1, line.getY(), x2-x1, line.getHeight());
            }
        }

        // Paint program counter
        int progCounterLine = getProgramCounterLine();
        if(progCounterLine>=0 && progCounterLine<getLineCount()) {
            TextBoxLine line = getLine(progCounterLine);
            aPntr.setPaint(new Color(199,218,175,200));
            aPntr.fillRect(line.getX(), line.getY()+.5, line.getWidth(), line.getHeight());
        }

        // Paint boxes around selected tokens
        Color tcolor = new Color("#FFF3AA");
        for(TextBoxToken token : getSelectedTokens()) {
            double x = Math.round(token.getTextBoxX()) - 1, w = Math.ceil(token.getTextBoxMaxX()) - x + 1;
            double y = Math.round(token.getTextBoxY()) - 1, h = Math.ceil(token.getTextBoxMaxY()) - y + 1;
            aPntr.setColor(tcolor); aPntr.fillRect(x,y,w,h);
        }

        // If HoverNode, underline
        if(_hoverNode!=null) {
            TextBoxToken ttoken = (TextBoxToken)_hoverNode.getStartToken();
            double x1 = ttoken.getTextBoxX(), y = ttoken.getTextBoxStringY() + 1, x2 = x1 + ttoken.getWidth();
            aPntr.setColor(Color.BLACK); aPntr.drawLine(x1,y,x2,y);
        }
    }

    /**
     * Called when a key is typed.
     */
    protected void keyTyped(ViewEvent anEvent)
    {
        // Get event info
        char keyChar = anEvent.getKeyChar(); if(keyChar==KeyCode.CHAR_UNDEFINED) return;
        boolean charDefined = !Character.isISOControl(keyChar);
        boolean commandDown = anEvent.isShortcutDown(), controlDown = anEvent.isControlDown();

        // Handle suppression of redundant paired char (parens, quotes, square brackets)
        // TODO: Don't do if in string/comment - but should also work if chars are no longer adjacent
        if(charDefined && !commandDown && !controlDown && isSelEmpty()) {
            int start = getSelStart() - 1;
            char c1 = start>=0 && start+1<length()? charAt(start) : 0;
            char c2 = c1!=0? charAt(start+1) : 0;
            switch(keyChar) {
                case '\'': if(c2=='\'' && getSelectedNode() instanceof JExprLiteral) start = -9; break;
                case '"': if(c2=='"' && getSelectedNode() instanceof JExprLiteral) start = -9; break;
                case ')': if(c1=='(' && c2==')') start = -9; break;
                case ']': if(c1=='[' && c2==']') start = -9; break;
                case '{':
                    TextBoxLine line = getSel().getStartLine();
                    if(line.getString().trim().length()==0 && line.getString().length()>=4) {
                        start = getSelStart();
                        delete(line.getStart(), line.getStart()+4, false);
                        setSel(start-4);
                    }
            }
            if(start==-9) {
                setSel(getSelStart()+1); anEvent.consume(); return; }
        }

        // Do normal version
        super.keyTyped(anEvent);

        // Handle insertion of paired chars (parens, quotes, square brackets) - TODO: Don't do if in string/comment
        if(charDefined && !commandDown && !controlDown) {
            String closer = null;
            switch(keyChar) {
                 case '\'': closer = "'"; break; // TODO: need inComment() check
                 case '"': closer = "\""; break;
                 case '(': closer = ")"; break;
                 case '[': closer = "]"; break;
                 case '}':
                     TextBoxLine line = getSel().getStartLine(), line2 = line.getPrevLine();
                     int indent = getIndent(line), indent2 = line2!=null? getIndent(line2) : 0;
                     if(line.getString().trim().startsWith("}") && indent>indent2 && indent>4) {
                         delete(line.getStart(), line.getStart()+(indent-indent2), false);
                         setSel(getSelStart()-4);
                     }
            }
            if(closer!=null) {
                int i = getSelStart(); replaceChars(closer, null, i, i, false); setSel(i); return; }
        }
    }

    /**
     * Called when a key is pressed.
     */
    protected void keyPressed(ViewEvent anEvent)
    {
        // Get event info
        int keyCode = anEvent.getKeyCode();
        boolean commandDown = anEvent.isShortcutDown(), shiftDown = anEvent.isShiftDown();

        // Handle tab
        if(keyCode==KeyCode.TAB) {
            if(!anEvent.isShiftDown()) indentLines();
            else outdentLines(); //replace("    ");
            anEvent.consume(); return;
        }

        // Handle newline special
        if(keyCode==KeyCode.ENTER && isSelEmpty()) {
            processNewline(); anEvent.consume(); return; }

        // Handle delete of adjacent paired chars (parens, quotes, square brackets) - TODO: don't do if in string/comment
        boolean isDelete = keyCode==KeyCode.BACK_SPACE || commandDown && !shiftDown && keyCode==KeyCode.X;
        if(isDelete && getSel().getSize()<=1) {
            int start = getSelStart(); if(isSelEmpty()) start--;
            char c1 = start>=0 && start+1<length()? charAt(start) : 0, c2 = c1!=0? charAt(start+1) : 0;
            switch(c1) {
                case '\'': if(c2=='\'') delete(start+1, start+2, false); break;
                case '"': if(c2=='"') delete(start+1, start+2, false); break;
                case '(': if(c2==')') delete(start+1, start+2, false); break;
                case '[': if(c2==']') delete(start+1, start+2, false); break;
            }
        }

        // Do normal version
        super.keyPressed(anEvent);
    }

    /**
     * Process newline key event.
     */
    protected void processNewline()
    {
        // Get line and its indent
        TextBoxLine line = getSel().getStartLine();
        int indent = getIndent(line);

        // Determine if this line is start of code block and/or not terminated
        // TODO: Need real startOfMultilineComment and inMultilineComment
        String lineString = line.getString().trim();
        boolean startOfMultiLineComment = lineString.startsWith("/*") && !lineString.endsWith("*/");
        boolean inMultiLineComment = lineString.startsWith("*") && !lineString.endsWith("*/");
        boolean endMultiLineComment = lineString.startsWith("*") && lineString.endsWith("*/");
        boolean startOfCodeBlock = lineString.endsWith("{");
        boolean terminated = lineString.endsWith(";") || lineString.endsWith("}") ||
            lineString.endsWith("*/") || lineString.indexOf("//")>=0 || lineString.length()==0;

        // Create indent string
        StringBuffer sb = new StringBuffer().append('\n');
        for(int i=0; i<indent; i++) sb.append(' ');

        // If start of multi-line comment, add " * "
        if(startOfMultiLineComment)
            sb.append(" * ");

        // If in multi-line comment, add "* "
        else if(inMultiLineComment)
            sb.append("* ");

        // If after multi-line comment, remove space from indent
        else if(endMultiLineComment) {
            if(sb.length()>0) sb.delete(sb.length()-1, sb.length()); }

        // If line not terminated increase indent
        else if(!terminated)
            for(int i=0; i<4; i++) sb.append(' ');

        // Do normal version
        replaceChars(sb.toString());

        // If start of multi-line comment, append terminator
        if(startOfMultiLineComment) { int start = getSelStart();
            replaceChars(sb.substring(0, sb.length()-1) + "/", null, start, start, false); setSel(start); }

        // If start of code block, append terminator
        else if(startOfCodeBlock && getJFile().getException()!=null) { int start = getSelStart();
            replaceChars(sb.substring(0, sb.length()-4) + "}", null, start, start, false); setSel(start); }
    }

    /**
     * Returns the indent of a given line.
     */
    public int getIndent(TextBoxLine aLine)  { int i = 0; while(i<aLine.length() && aLine.charAt(i)==' ') i++; return i; }

    /**
     * Indents the text.
     */
    public void indentLines()
    {
        int sline = getSel().getStartLine().getIndex();
        int eline = getSel().getEndLine().getIndex();
        for(int i=sline; i<=eline; i++) { TextBoxLine line = getLine(i);
            replaceChars("    ", null, line.getStart(), line.getStart(), false); }
    }

    /**
     * Indents the text.
     */
    public void outdentLines()
    {
        int sline = getSel().getStartLine().getIndex();
        int eline = getSel().getEndLine().getIndex();
        for(int i=sline; i<=eline; i++) { TextBoxLine line = getLine(i);
            if(line.length()<4 || !line.subSequence(0, 4).toString().equals("    ")) continue;
            replaceChars("", null, line.getStart(), line.getStart() + 4, false);
        }
    }

    /**
     * Override to setTextModified.
     */
    protected void richTextPropChange(PropChange anEvent)
    {
        // Do normal version and update TextPane.TextModified (just return if not chars change)
        super.richTextPropChange(anEvent); if(anEvent.getPropertyName()!=RichText.Chars_Prop) return;

        // Set TextPane modified
        JavaTextPane tp = getTextPane(); if(tp!=null) tp.setTextModified(getUndoer().hasUndos());

        // Call didAddChars/didRemoveChars
        RichText.CharsChange cc = (RichText.CharsChange)anEvent;
        int start = anEvent.getIndex(); CharSequence oval = cc.getOldValue(), nval = cc.getNewValue();
        if(nval!=null) didAddChars(start, nval);
        else didRemoveChars(start, oval);
    }

    /**
     * Called when characters are added.
     */
    protected void didAddChars(int aStart, CharSequence theChars)
    {
        // Iterate over BuildIssues and shift start/end for removed chars
        int len = theChars.length(), endNew = aStart + len;
        for(BuildIssue is : getBuildIssues()) {
            int istart = is.getStart(), iend = is.getEnd();
            if(aStart<=istart) is.setStart(istart + len);
            if(aStart<iend) is.setEnd(iend + len);
        }

        // Iterate over Breakpoints and shift start/end for removed chars
        int sline = getLineAt(aStart).getIndex(), eline = getLineAt(aStart+len).getIndex(), dline = eline - sline;
        if(sline!=eline) for(Breakpoint bp : getBreakpoints().toArray(new Breakpoint[0])) {
            int bline = bp.getLine();
            if(sline<bline && eline<=bline) { bp.setLine(bline + dline);
                getProjBreakpoints().writeFile(); }
        }
    }

    /**
     * Called when characters are removed.
     */
    protected void didRemoveChars(int aStart, CharSequence theChars)
    {
        // See if we need to shift BuildIssues
        int endOld = aStart + theChars.length();
        for(BuildIssue is : getBuildIssues()) {
            int istart = is.getStart(), iend = is.getEnd(), start = istart, end = iend;
            if(aStart<istart) start = istart - (Math.min(istart, endOld) - aStart);
            if(aStart<iend) end = iend - (Math.min(iend, endOld) - aStart);
            is.setStart(start); is.setEnd(end);
        }

        // Get number of newlines removed (in chars)
        int nlc = 0; for(int i=0,iMax=theChars.length();i<iMax;i++) { char c = theChars.charAt(i);
            if(c=='\r') { nlc++; if(i+1<iMax && theChars.charAt(i+1)=='\n') i++; }
            else if(c=='\n') nlc++;
        }

        // See if we need to remove Breakpoints
        int sline = getLineAt(aStart).getIndex(), eline = sline + nlc, dline = eline - sline;
        if(sline!=eline) for(Breakpoint bp : getBreakpoints().toArray(new Breakpoint[0])) {
            int bline = bp.getLine();
            if(sline<bline && eline<=bline) { bp.setLine(bline - dline);
                getProjBreakpoints().writeFile(); }
            else if(sline<bline && eline>bline)
                getProjBreakpoints().remove(bp);
        }
    }

    /**
     * Returns the project.
     */
    public Project getProject()
    {
        WebFile file = getSourceFile();
        return file!=null? Project.get(file) : null;
    }

    /**
     * Returns the top level project.
     */
    public Project getRootProject()  { Project proj = getProject(); return proj!=null? proj.getRootProject() : null; }

    /**
     * Returns BuildIssues from ProjectFile.
     */
    public BuildIssue[] getBuildIssues()
    {
        WebFile file = getSourceFile(); Project proj = getRootProject();
        return proj!=null? proj.getBuildIssues().getIssues(file) : BuildIssues.NO_ISSUES;
    }

    /**
     * Returns the project breakpoints.
     */
    private Breakpoints getProjBreakpoints()  { Project p = getRootProject(); return p!=null? p.getBreakpoints() : null; }

    /**
     * Returns Breakpoints from ProjectFile.
     */
    public List <Breakpoint> getBreakpoints()
    {
        WebFile file = getSourceFile(); Breakpoints bpnts = file!=null? getProjBreakpoints() : null;
        return bpnts!=null? bpnts.get(file) : (List)Collections.emptyList();
    }

    /**
     * Adds breakpoint at line.
     */
    public void addBreakpoint(int aLine)
    {
        WebFile file = getSourceFile();
        Project proj = getRootProject(); if(proj==null) return;
        proj.addBreakpoint(file, aLine);
    }

    /**
     * Remove breakpoint.
     */
    public void removeBreakpoint(Breakpoint aBP)
    {
        WebFile file = getSourceFile();
        Project proj = getRootProject(); if(proj==null) return;
        proj.getBreakpoints().remove(aBP);
    }

    /**
     * Returns the ProgramCounter line.
     */
    public int getProgramCounterLine()
    {
        JavaTextPane tpane = getTextPane();
        return tpane!=null? tpane.getProgramCounterLine() : -1;
    }
}