package snapcode.views;
import snap.view.UILoader;
import snap.view.View;
import snap.view.ViewOwner;

/**
 * Stage Pane
 */
public class StagePane extends ViewOwner {

    /**
     * Constructor.
     */
    public StagePane()
    {
        super();
    }

    @Override
    protected View createUI()
    {
        return UILoader.loadViewForString(STAGE_PANE_UI);
    }

    // The UI
    private static final String STAGE_PANE_UI = """
        <ColView Name="MainColView" PrefWidth="500" FillWidth="true">
          <BoxView Margin="5" Fill="#FF" Border="#C0 1" BorderRadius="4" PrefHeight="300" GrowHeight="true" />
          <BoxView Margin="5" Fill="#FF" Border="#C0 1" BorderRadius="4" PrefHeight="300" GrowHeight="true" />
        </ColView>
        """;
}
