package snapcode.util;
import snap.geom.*;
import snap.gfx.*;
import snap.util.FilePathUtils;
import snap.util.SnapUtils;
import snap.view.*;
import snap.web.WebFile;
import snap.web.WebURL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class holds a sample doc.
 */
public class SampleDoc {

    // The name
    private String _name;

    // The main file type
    private String _fileType;

    // The image
    private Image _image;

    // The doc view
    private ColView _docView;

    // The SamplePane current showing this doc
    protected SamplesPane _samplesPane;

    // The sample docs
    private static SampleDoc[] _sampleDocs;

    // Constants
    private static final String SAMPLES_ROOT = "https://reportmill.com/SnapCode/Samples/";
    private static final String SAMPLES_INDEX = SAMPLES_ROOT +  "index.txt";
    private static final Effect SHADOW = new ShadowEffect(20, Color.GRAY, 0, 0);
    private static final Effect SHADOW_SEL = new ShadowEffect(20, Color.get("#038ec3"), 0, 0);
    private static final Size DOC_SIZE = new Size(130, 102);

    /**
     * Constructor.
     */
    public SampleDoc(String aName)
    {
        _name = FilePathUtils.getFilenameSimple(aName);
        _fileType = FilePathUtils.getType(aName);
    }

    /**
     * Returns the name.
     */
    public String getName()  { return _name; }

    /**
     * Returns the doc url at given index.
     */
    public WebURL getURL()
    {
        String name = getName();
        String usrString = SAMPLES_ROOT + name + '/' + name + '.' + _fileType;
        return WebURL.getURL(usrString);
    }

    /**
     * Returns the doc thumbnail image at given index.
     */
    public Image getImage()
    {
        // If image already set, just return
        if (_image != null) return _image;

        // Get image name, URL string, and URL
        String name = getName();
        String imageUrlString = SAMPLES_ROOT + name + '/' + name + ".png";
        WebURL imageUrl = WebURL.getURL(imageUrlString);

        // Create Image. Then make sure image is loaded by requesting Image.Native.
        Image image = Image.getImageForSource(imageUrl);
        image.getNative();

        // Set and return
        return _image = image;
    }

    /**
     * Returns the view for this sample doc.
     */
    public ColView getDocView()
    {
        // If already set, just return
        if (_docView != null) return _docView;

        // Create ImageView to show thumbnail image
        ImageView imageView = new ImageView();
        if (SnapUtils.isWebVM)
            imageView.setImage(getImage());
        else imageView.setPrefSize(DOC_SIZE);

        // Create label to show name
        String name = getName();
        String labelText = name.replace('_', ' ');
        Label label = new Label(labelText);
        label.setFont(Font.Arial13);
        label.setPadding(3, 4, 3, 4);
        label.setLeanY(VPos.BOTTOM);

        // Create col view to hold image and label
        ColView docView = new ColView();
        docView.setPrefSize(200, 200);
        docView.setAlign(Pos.CENTER);
        docView.setPadding(0, 0, 8, 0);
        docView.setChildren(imageView, label);

        // Listen for mouse press (to forward to samples pane for selection or double-click)
        docView.addEventHandler(e -> docViewWasPressed(e), View.MousePress);

        // Return
        return _docView = docView;
    }

    /**
     * Called when doc view gets mouse press.
     */
    private void docViewWasPressed(ViewEvent anEvent)
    {
        if (_samplesPane != null)
            _samplesPane.docBoxWasPressed(this, anEvent);
    }

    /**
     * Sets this doc selected.
     */
    public void setSelected(boolean isSelected)
    {
        // Set ImageView.Effect
        ColView docView = getDocView();
        ImageView imageView = (ImageView) docView.getChild(0);
        imageView.setEffect(isSelected ? SHADOW_SEL : SHADOW);

        // Set Label Fill and TextFill
        Label oldLabel = (Label) docView.getChild(1);
        oldLabel.setFill(isSelected ? Color.BLUE : null);
        oldLabel.setTextFill(isSelected ? Color.WHITE : null);
    }

    /**
     * Loads SampleDocs.
     */
    public static SampleDoc[] getSampleDocs()
    {
        if (_sampleDocs != null) return _sampleDocs;

        // Load sample docs
        SampleDoc[] sampleDocs = getSampleDocsImpl();

        // Load images in background - maybe silly
        new Thread(() -> loadImagesInBackground(sampleDocs)).start();

        // Set and return
        return _sampleDocs = sampleDocs;
    }

    /**
     * Loads SampleDocs.
     */
    private static SampleDoc[] getSampleDocsImpl()
    {
        // Get index file URL
        WebURL indexFileURL = WebURL.getURL(SAMPLES_INDEX);
        assert (indexFileURL != null);

        // Get index file and text
        WebFile indexFile = indexFileURL.createFile(false);
        String indexText = indexFile.getText();
        if (indexText == null) {
            System.out.println("SampleDoc.getSampleDocsImpl: Unable to fetch index: " + indexFileURL);
            return new SampleDoc[0];
        }

        // Get text and break into lines
        String[] lines = indexText.split("\\s*\n\\s*");

        // Vars
        List<SampleDoc> sampleDocsList = new ArrayList<>();

        // Get names list from lines
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 0) {
                SampleDoc doc = new SampleDoc(line);
                sampleDocsList.add(doc);
            }
        }

        // Return
        return sampleDocsList.toArray(new SampleDoc[0]);
    }

    /**
     * Load images in background so showing panel isn't such an all or nothing deal.
     */
    private static void loadImagesInBackground(SampleDoc[] sampleDocs)
    {
        for (SampleDoc sampleDoc : sampleDocs) {
            Image image = sampleDoc.getImage();
            ViewUtils.runLater(() -> {
                ColView docView = sampleDoc.getDocView();
                ImageView imageView = (ImageView) docView.getChild(0);
                imageView.setPrefSize(-1, -1);
                imageView.setImage(image);
            });
        }
    }
}
