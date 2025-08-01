/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.gfx.Color;
import snap.gfx.Font;
import snap.gfx.ShadowEffect;
import snap.util.StringUtils;
import snap.view.*;

/**
 * A WebPage subclass for SnapKit files.
 */
public class SnapPage extends WebPage {

    // The content
    private View _content;

    /**
     * Returns the content.
     */
    public View getContent()  { return _content; }

    /**
     * Returns the SnapKit file root view.
     */
    protected View createUI()
    {
        ViewArchiver.setUseRealClass(false);

        // Load UI
        View superUI = null;
        try { superUI = _content = UILoader.loadViewForOwnerAndUrl(this, getFile().getUrl()); }
        catch (Exception e) { return createExceptionUI(e); }
        finally { ViewArchiver.setUseRealClass(true); }

        if (!(superUI instanceof DocView)) {
            superUI.setFill(ViewUtils.getBackFill());
            superUI.setBorder(Color.BLACK, 1);
            superUI.setEffect(new ShadowEffect().copySimple());
            BoxView box = new BoxView(superUI);
            box.setFill(ViewTheme.get().getGutterFill());
            if (!(superUI instanceof SpringView))
                superUI.setMinSize(500, 500);
            superUI = box;
        }

        return new ScrollView(superUI);
    }

    /**
     * Returns UI to show an exception.
     */
    protected View createExceptionUI(Exception e)
    {
        TextView text = new TextView();
        text.setFont(Font.Arial14);
        text.setText(StringUtils.getStackTraceString(e));
        return text;
    }
}