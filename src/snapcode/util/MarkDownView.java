package snapcode.util;
import snap.geom.Pos;
import snap.gfx.Color;
import snap.text.TextBlock;
import snap.text.TextStyle;
import snap.util.ArrayUtils;
import snap.view.*;

/**
 * This view class renders mark down.
 */
public class MarkDownView extends ChildView {

    // The root markdown node
    private MDNode _rootMarkdownNode;

    // The selected code block node
    private MDNode _selCodeBlockNode;

    /**
     * Constructor.
     */
    public MarkDownView()
    {
        super();
        setPadding(10, 20, 20, 20);
        setFill(Color.WHITE);
    }

    /**
     * Sets MarkDown.
     */
    public void setMarkDown(String markDown)
    {
        _rootMarkdownNode = new MDParser().parseMarkdownChars(markDown);
        MDNode[] rootNodes = _rootMarkdownNode.getChildNodes();

        for (MDNode mdnode : rootNodes) {
            View nodeView = createViewForNode(mdnode);
            if (nodeView != null)
                addChild(nodeView);
        }
    }

    /**
     * Returns the selected code block node.
     */
    public MDNode getSelCodeBlockNode()
    {
        if (_selCodeBlockNode != null)
            return _selCodeBlockNode;

        MDNode[] rootNodes = _rootMarkdownNode.getChildNodes();
        return ArrayUtils.findMatch(rootNodes, node -> node.getNodeType() == MDNode.NodeType.CodeBlock);
    }

    /**
     * Creates view for node.
     */
    private View createViewForNode(MDNode markNode)
    {
        switch (markNode.getNodeType()) {
            case Header1: case Header2: return createViewForHeaderNode(markNode);
            case Text: return createViewForContentNode(markNode);
            case CodeBlock: return createViewForCodeBlockNode(markNode);
            default:
                System.err.println("MarkDownView.createViewForNode: No support for type: " + markNode.getNodeType());
                return null;
        }
    }

    /**
     * Creates a view for header node.
     */
    private View createViewForHeaderNode(MDNode headerNode)
    {
        TextArea textArea = new TextArea();
        textArea.setMargin(16, 8, 16, 8);
        if (headerNode.getNodeType() == MDNode.NodeType.Header2)
            textArea.setMargin(8, 8, 8, 8);

        // Reset style
        TextStyle textStyle = headerNode.getNodeType() == MDNode.NodeType.Header1 ? MDUtils.getHeader1Style() : MDUtils.getHeader2Style();
        TextBlock textBlock = textArea.getTextBlock();
        textBlock.setDefaultStyle(textStyle);

        // Set text
        textBlock.addChars(headerNode.getText());

        // Return
        return textArea;
    }

    /**
     * Creates a view for header node.
     */
    private View createViewForContentNode(MDNode contentNode)
    {
        TextArea textArea = new TextArea();
        textArea.setMargin(8, 8, 8, 8);

        // Reset style
        TextStyle textStyle = MDUtils.getContentStyle();
        TextBlock textBlock = textArea.getTextBlock();
        textBlock.setDefaultStyle(textStyle);

        // Set text
        textBlock.addChars(contentNode.getText());

        // Return
        return textArea;
    }

    /**
     * Creates a view for code block.
     */
    private View createViewForCodeBlockNode(MDNode codeNode)
    {
        TextArea textArea = new TextArea();
        textArea.setMargin(8, 8, 8, 8);
        textArea.setPadding(16, 16, 16, 16);
        textArea.setBorderRadius(8);
        textArea.setFill(new Color(.96, .97, .98));
        textArea.setEditable(true);
        textArea.setFocusPainted(true);

        // Reset style
        TextStyle textStyle = MDUtils.getCodeStyle();
        TextBlock textBlock = textArea.getTextBlock();
        textBlock.setDefaultStyle(textStyle);

        // Set text
        textBlock.addChars(codeNode.getText());

        // Add listener to select code block
        textArea.addEventFilter(e -> _selCodeBlockNode = codeNode, MousePress);

        // Wrap in box and return
        BoxView codeBlockBox = new BoxView(textArea);
        codeBlockBox.setAlign(Pos.CENTER_LEFT);
        return codeBlockBox;
    }

    /**
     * Returns the preferred width.
     */
    protected double getPrefWidthImpl(double aH)
    {
        return ColView.getPrefWidth(this, aH);
    }

    /**
     * Returns the preferred height.
     */
    protected double getPrefHeightImpl(double aW)
    {
        return ColView.getPrefHeight(this, aW);
    }

    /**
     * Override to layout children with ColView layout.
     */
    protected void layoutImpl()
    {
        ColView.layout(this, true);
    }
}
