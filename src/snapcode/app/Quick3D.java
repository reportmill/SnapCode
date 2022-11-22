/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.app;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.gfx.Color;
import snap.gfx.Image;
import snap.gfx3d.*;
import snap.util.SnapUtils;

/**
 * Utility methods to create basic 3D quickly.
 */
public class Quick3D {

    /**
     * Create a Cube quickly.
     */
    public static CameraView createCube()
    {
        // Create/configure CameraView
        CameraView cameraView = new CameraView();
        cameraView.setPrefSize(360, 240);
        cameraView.setFill(Color.WHITE);
        cameraView.setShowCubeView(true);

        // Configure camera
        Camera camera = cameraView.getCamera();
        camera.setYaw(-30);
        camera.setPitch(30);

        // Add a simple rect path
        Shape path = new Rect(100, 100, 100, 100);
        PathBox3D pathBox3D = new PathBox3D(path, 0, 100);
        pathBox3D.setColor(Color.GREEN);
        pathBox3D.setStroke(Color.BLACK, 1);

        // Add to scene
        Scene3D scene3D = cameraView.getScene();
        scene3D.addChild(pathBox3D);

        // Return
        return cameraView;
    }

    /**
     * Create a Cube quickly.
     */
    public static CameraView createImage3D(Image anImage)
    {
        // Create/configure CameraView
        CameraView cameraView = new CameraView();
        cameraView.setPrefSize(360, 240);
        cameraView.setFill(Color.WHITE);
        cameraView.setShowCubeView(true);

        // Configure camera
        Camera camera = cameraView.getCamera();
        camera.setYaw(-30);
        camera.setPitch(30);

        // Create texture
        Image image = anImage;
        if (image == null)
            image = Image.get("https://reportmill.com/examples/Weird.jpg");
        if (!SnapUtils.isTeaVM)
            image = image.getImageScaled(1);
        Texture texture = new Texture(image);

        // Create simple Poly3D
        double width = 200;
        double height = 200;
        double z = 0;
        Poly3D poly3D = new Poly3D();
        poly3D.setColor(Color.LIGHTBLUE);
        poly3D.addPoint(0, 0, z);
        poly3D.addPoint(width, 0, z);
        poly3D.addPoint(width, height, z);
        poly3D.addPoint(0, height, z);
        poly3D.addTexCoord(0, 0);
        poly3D.addTexCoord(1, 0);
        poly3D.addTexCoord(1, 1);
        poly3D.addTexCoord(0, 1);
        poly3D.setTexture(texture);

        // Add to scene
        Scene3D scene3D = cameraView.getScene();
        scene3D.addChild(poly3D);

        // Return
        return cameraView;
    }
}
