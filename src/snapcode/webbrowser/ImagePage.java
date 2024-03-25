/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.gfx.Image;
import snap.util.FormatUtils;
import snap.view.*;
import snap.web.WebFile;

/**
 * A page for images.
 */
public class ImagePage extends WebPage {

    // The image
    private Image _image;

    /**
     * Constructor.
     */
    public ImagePage()
    {
        super();
    }

    /**
     * Returns the image.
     */
    public Image getImage()
    {
        if (_image != null) return _image;
        WebFile file = getFile();
        return _image = Image.getImageForSource(file);
    }

    /**
     * Create UI.
     */
    protected View createUI()
    {
        ColView topColView = new ColView();
        topColView.setPadding(10, 10, 10, 10);
        topColView.setSpacing(5);

        // Get info
        WebFile file = getFile();
        String fileNameStr = "File name: " + file.getName();
        String fileSizeKB = FormatUtils.formatNum(file.getSize() / 1000d);
        String fileSizeBytes = FormatUtils.formatNum("#,###", file.getSize());
        String fileSizeStr = "File size: " + fileSizeKB + " KB (" + fileSizeBytes + " bytes)";

        // Create/add Labels
        Label fileNameLabel = new Label(fileNameStr);
        Label fileSizeLabel = new Label(fileSizeStr);
        Label imageSizeLabel = new Label();
        imageSizeLabel.setName("ImageSizeLabel");
        Label imageDpiLabel = new Label();
        imageDpiLabel.setName("ImageDPILabel");
        topColView.setChildren(fileNameLabel, fileSizeLabel, imageSizeLabel, imageDpiLabel);

        // Create/add ImageView
        Image image = getImage();
        ImageView imageView = new ImageView(image);
        imageView.setKeepAspect(true);
        imageView.setGrowWidth(true);
        imageView.setGrowHeight(true);
        topColView.addChild(imageView);

        // Return
        return topColView;
    }

    /**
     * Override to resetUI if image is loaded later.
     */
    @Override
    protected void initShowing()
    {
        Image image = getImage();
        if (!image.isLoaded())
            image.addLoadListener(this::resetLater);
    }

    /**
     * ResetUI.
     */
    @Override
    protected void resetUI()
    {
        // Reset ImageSizeLabel, ImageDPILabel
        Image image = getImage();
        String imageSizeStr = "Image size: " + image.getPixWidth() + " x " + image.getPixHeight() + " pixels";
        if (image.getDpiX() != 72)
            imageSizeStr += ", " + ((int) image.getWidth()) + " x " + ((int) image.getHeight()) + " points";
        setViewValue("ImageSizeLabel", imageSizeStr);
        String imageDpiStr = "Image DPI: " + image.getDpiX();
        setViewValue("ImageDPILabel", imageDpiStr);
    }
}