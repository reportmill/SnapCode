/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.gfx.Image;
import snap.view.*;
import snap.web.WebFile;

/**
 * A page for images.
 */
public class ImagePage extends WebPage {

    /**
     * Create UI.
     */
    protected View createUI()
    {
        ColView topColView = new ColView();
        topColView.setPadding(10, 10, 10, 10);
        topColView.setSpacing(5);

        // Get File
        WebFile file = getFile();
        Image image = Image.getImageForSource(getFile());

        // Get info
        String fileName = file.getName();
        int fileSize = (int) Math.round(file.getSize() / 1000d);
        String imageSize = image.getPixWidth() + " x " + image.getPixHeight() + " pixels";

        // Create/add Labels
        Label fileNameLabel = new Label("File name: " + fileName);
        Label fileSizeLabel = new Label("File size: " + fileSize);
        Label imageSizeLabel = new Label("Image size: " + imageSize);
        Label imageDpiLabel = new Label("Image DPI: " + image.getDPIX());
        topColView.setChildren(fileNameLabel, fileSizeLabel, imageSizeLabel, imageDpiLabel);

        // Create/add ImageView
        ImageView imageView = new ImageView(image);
        imageView.setKeepAspect(true);
        imageView.setGrowWidth(true);
        imageView.setGrowHeight(true);
        topColView.addChild(imageView);

        // Return
        return topColView;
    }
}