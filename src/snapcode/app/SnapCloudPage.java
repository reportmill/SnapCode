package snapcode.app;
import snap.gfx.Image;
import snap.view.Label;
import snap.web.WebURL;
import snapcode.webbrowser.WebPage;

/**
 * This page helps manage default cloud storage.
 */
public class SnapCloudPage extends WebPage {

    /**
     * Constructor.
     */
    public SnapCloudPage(WorkspacePane workspacePane)
    {
        super();
    }

    @Override
    protected void initUI()
    {
        // Configure label
        Label snapCloudLabel = getView("SnapCloudLabel", Label.class);
        WebURL snapCloudImageUrl = WebURL.getURL("https://reportmill.com/SnapCode/images/SnapCloud.png");
        Image snapCloudImage = Image.getImageForUrl(snapCloudImageUrl);
        snapCloudLabel.setImage(snapCloudImage);
    }
}
