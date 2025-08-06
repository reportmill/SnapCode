package snapcode.views;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.project.Project;

/**
 * Utilities for block coding.
 */
public class BlocksUtils {

    /**
     * Returns whether given project is a blocks project.
     */
    public static boolean isBlocksProject(Project project)
    {
        WebSite projSite = project.getSite();
        return projSite.getFileForPath("/src/Actor1.java") != null;
    }

    /**
     * Creates default files for project.
     */
    public static void configureNewBlockCodeProject(Project newBlockCodeProject)
    {
        // Add Actor1.java
        WebSite projSite = newBlockCodeProject.getSite();
        WebFile actor1JavaFile = projSite.createFileForPath("/src/Actor1.java", false);
        actor1JavaFile.setText(ACTOR1_JAVA_TEXT);
        actor1JavaFile.save();

        // Add Stage1.java
        WebFile stage1JavaFile = projSite.createFileForPath("/src/Stage1.java", false);
        stage1JavaFile.setText(STAGE1_JAVA_TEXT);
        stage1JavaFile.save();

        // Add Stage1.snp
        WebFile stage1SnapFile = projSite.createFileForPath("/src/Stage1.snp", false);
        stage1SnapFile.setText(STAGE1_SNAP_TEXT);
        stage1SnapFile.save();

        // Set main file
        newBlockCodeProject.getBuildFile().setMainClassName("Stage1");
    }

    // Template for first actor class
    public static final String ACTOR1_JAVA_TEXT = """
        import snap.games.*;

        /**
         * This actor subclass represents the first actor in our block code app.
         */
        public class Actor1 extends Actor {

            /**
             * Constructor.
             */
            public Actor1()
            {
            }
            
            @Override
            protected void act()
            {
            }
        }
        """;

    // Template for first stage class
    public static final String STAGE1_JAVA_TEXT = """
        import snap.games.*;

        /**
         * This game controller subclass represents the first actor in our block code app.
         */
        public class Stage1 extends GameController {

            /**
             * Constructor.
             */
            public Stage1()
            {
            }

            /**
             * Standard main implementation.
             */
            public static void main(String[] args)
            {
                Game.showGameForClass(Stage1.class);
            }
        }
        """;

    // Template for first stage class
    public static final String STAGE1_SNAP_TEXT = """
        <GameView Width="500" Height="500" PrefWidth="500" PrefHeight="500">
          <Actor X="80" Y="80" ImageName="Cat" Class="Actor1" />
        </GameView>
        """;
}
