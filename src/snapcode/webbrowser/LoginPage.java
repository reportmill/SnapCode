/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.view.Button;
import snap.view.View;
import snap.view.ViewEvent;
import snap.viewx.DialogBox;
import snap.web.WebSite;
import java.util.Objects;

/**
 * A Page to handle UNAUTHORISED responses.
 */
public class LoginPage extends WebPage {

    // The site, if running as panel
    private WebSite  _site;

    /**
     * Shows a login panel. Returns true is username or password were updated.
     */
    public boolean showPanel(View aView, WebSite aSite)
    {
        String usr = aSite.getUserName(), pw = aSite.getPassword();
        _site = aSite;
        DialogBox dbox = new DialogBox("Login Panel");
        dbox.setContent(getUI());
        boolean confirmed = dbox.showConfirmDialog(aView);
        if (confirmed) confirmed = !Objects.equals(usr, getName()) || !Objects.equals(pw, getPassword());
        if (confirmed) ClientUtils.setAccess(getSite(), getName(), getPassword());
        _site = null;
        return confirmed;
    }

    /**
     * Show panel for given data source.
     */
    protected void initUI()
    {
        // Set FirstFocus and SiteText
        setFirstFocus("NameText");
        setViewValue("SiteText", "Site: " + getSite().getURLString());

        // Make LoginButton default
        getView("LoginButton", Button.class).setDefaultButton(true);

        // If previous access found, try again
        if (ClientUtils.setAccess(getSite()))
            getBrowser().setURL(getURL());
    }

    /**
     * Reset UI.
     */
    @Override
    protected void resetUI()
    {
        setViewText("NameText", getName());
        setViewText("PasswordText", getPassword());
    }

    /**
     * Respond to UI.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        // Handle LoginButton
        if (anEvent.equals("LoginButton")) {
            if (getRememberLogin())
                ClientUtils.setAccess(getSite(), getName(), getPassword());
            if (getBrowser() != null)
                getBrowser().setURL(getURL());
        }
    }

    /**
     * Returns the site.
     */
    public WebSite getSite()
    {
        return _site != null ? _site : super.getSite();
    }

    /**
     * Returns the name.
     */
    public String getName()
    {
        return getSite().getUserName();
    }

    /**
     * Sets the name.
     */
    public void setName(String aString)
    {
        getSite().setUserName(aString);
    }

    /**
     * Returns the password.
     */
    public String getPassword()
    {
        return getSite().getPassword();
    }

    /**
     * Sets the password.
     */
    public void setPassword(String aString)
    {
        getSite().setPassword(aString);
    }

    /**
     * Returns whether to remember username/password.
     */
    public boolean getRememberLogin()
    {
        return getViewBoolValue("RememberLoginCheckBox");
    }
}