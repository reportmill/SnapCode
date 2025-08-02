package snapcode.views;
import snap.web.WebFile;
import snap.web.WebSite;
import snapcode.project.Project;

/**
 * Utilities for block coding.
 */
public class BlocksUtils {

    /**
     * Creates default files for project.
     */
    public static void addDefaultFilesForProject(Project project)
    {
        // Add Sprite1.java
        WebSite projSite = project.getSite();
        WebFile sprite1JavaFile = projSite.createFileForPath("/src/Sprite1.java", false);
        sprite1JavaFile.setText(SPRITE1_JAVA_TEXT);
        sprite1JavaFile.save();

        // Add Stage1.java
        WebFile stage1JavaFile = projSite.createFileForPath("/src/Stage1.java", false);
        stage1JavaFile.setText(STAGE1_JAVA_TEXT);
        stage1JavaFile.save();

        // Add Stage1.snp
        WebFile stage1SnapFile = projSite.createFileForPath("/src/Stage1.snp", false);
        stage1SnapFile.setText(STAGE1_SNAP_TEXT);
        stage1SnapFile.save();
    }

    // Template for first sprite class
    public static final String SPRITE1_JAVA_TEXT = """
        import snap.games.*;

        /**
         * This actor subclass represents the first sprite in our block code app.
         */
        public class Sprite1 extends Actor {

            /**
             * Constructor.
             */
            public Sprite1()
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
         * This game controller subclass represents the first sprite in our block code app.
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
        <GameView PrefWidth="500" PrefHeight="500">
          <Actor X="100" Y="100" ImageName="Duke" Class="Sprite1" />
        </GameView>
        """;
}
