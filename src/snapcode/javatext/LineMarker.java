package snapcode.javatext;
import snap.geom.Insets;
import snap.geom.Rect;
import snap.view.PopupWindow;
import snap.view.TextArea;
import snap.view.View;

/**
 * The class that describes a marker in LineHeadView or LineFootView.
 */
public abstract class LineMarker<T> extends Rect {

    // The JavaTextPane
    protected JavaTextPane _textPane;

    // The JavaTextArea
    protected JavaTextArea _textArea;

    // The object that is being marked.
    protected T  _target;

    // The Popup window
    private PopupWindow _popupWindow;

    /**
     * Constructor.
     */
    public LineMarker(JavaTextPane aJavaTextPane, T aTarget)
    {
        super();
        _textPane = aJavaTextPane;
        _textArea = aJavaTextPane.getTextArea();
        _target = aTarget;
    }

    /**
     * Returns the marker text.
     */
    public abstract String getMarkerText();

    /**
     * Shows the popup.
     */
    public void showPopup(View aView)
    {
        PopupWindow popupWindow = getPopupWindow();
        if (popupWindow.isShowing())
            return;
        popupWindow.setSizeToBestSize();

        // Get popup location
        int MARGIN_OFFSET = 15;
        double popupX = -MARGIN_OFFSET - popupWindow.getWidth();
        if (aView instanceof LineHeadView)
            popupX = 30;
        double popupY = getMidY() - Math.round(popupWindow.getHeight() / 2);

        // Show popup
        popupWindow.show(aView, popupX, popupY);
    }

    /**
     * Hides the popup.
     */
    public void hidePopup()
    {
        if (_popupWindow != null && _popupWindow.isShowing())
            _popupWindow.hide();
    }

    /**
     * Returns the popup window.
     */
    private PopupWindow getPopupWindow()
    {
        if (_popupWindow != null) return _popupWindow;
        TextArea textArea = new TextArea();
        textArea.setDefaultTextStyleString("Font:Arial 14");
        textArea.setMargin(new Insets(6));
        textArea.setMaxWidth(500);
        textArea.setWrapLines(true);
        textArea.setText(getMarkerText());
        PopupWindow popupWindow = new PopupWindow();
        popupWindow.setContent(textArea);
        return _popupWindow = popupWindow;
    }

    /**
     * Override to expand hit area.
     */
    @Override
    public boolean contains(double aX, double aY)
    {
        return (x - 2 <= aX) && (aX <= x + width + 4) && (y - 1 <= aY) && (aY <= y + height + 2);
    }
}
