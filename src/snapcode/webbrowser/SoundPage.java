/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapcode.webbrowser;
import snap.gfx.SoundClip;
import snap.view.*;

/**
 * A WebPage subclass for a sound file.
 */
public class SoundPage extends WebPage {

    // Contents of a sampled audio file
    private SoundClip _clip;

    // A Timer
    private ViewTimer _timer = new ViewTimer(() -> fireActionEventForObject("ProgressTimer", null), 100);

    /**
     * Constructor.
     */
    public SoundPage()
    {
        super();
    }

    /**
     * Returns the clip to be edited.
     */
    public SoundClip getClip()
    {
        if (_clip == null)
            try { _clip = SoundClip.get(getFile()); }
            catch (Exception e) { System.err.println("SoundPage.getClip: Error loading clip: " + e.getMessage()); }
        return _clip;
    }

    /**
     * Override to stop sound when file pane removed.
     */
    @Override
    protected void handlePageRemovedFromBrowser(WebBrowser aBrowser)
    {
        stop();
    }

    /**
     * Start playing the sound at the current position.
     */
    public void play()
    {
        getClip().play();
        _timer.stop();
        _timer.start();
        setViewText("PlayButton", "Stop");
    }

    /**
     * Stop playing the sound, but retain the current position.
     */
    public void stop()
    {
        _timer.stop();
        if (_clip != null) {
            _clip.stop();
            _clip.setTime(0);
        }
        setViewText("PlayButton", "Play");
    }

    /**
     * Skip to the specified position. Called when user drags the slider.
     */
    public void skip(int position)
    {
        if (position < 0 || position > _clip.getLength()) return;
        _clip.setTime(position);
    }

    /**
     * Creates the UI panel.
     */
    protected View createUI()  { return UILoader.loadViewForControllerAndString(this, SOUND_PAGE_UI); }

    // SoundPage UI
    private static String SOUND_PAGE_UI = """
        <ColView Align="TOP_CENTER" Padding="50" Spacing="10">
          <Label Text="Playback" />
          <RowView Padding="20,10,20,10" Spacing="5" Border="#00">
            <Button Name="PlayButton" Text="Play" MinWidth="100" />
            <Slider Name="ProgressSlider" PrefWidth="200" />
            <Label Name="TimeLabel" Text="0" PrefWidth="36" />
          </RowView>
          <Label Text="Record/Save" />
          <RowView Padding="20" Spacing="5" Border="#00">
            <Button Name="RecordButton" Text="Record" MinWidth="100" />
            <Button Name="SaveButton" Text="Save" MinWidth="100" />
          </RowView>
        </ColView>
        """;

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        SoundClip soundClip = getClip();

        // Update PlayButton
        setViewEnabled("PlayButton", soundClip != null);

        // Update ProgressSlider
        setViewValue("ProgressSlider", soundClip != null ? _timer.getTime() : 0);
        setViewEnabled("ProgressSlider", soundClip != null);
        getView("ProgressSlider", Slider.class).setMax(soundClip != null ? soundClip.getLength() : 0);

        // Update TimeLabel
        setViewValue("TimeLabel", soundClip != null ? soundClip.getLength() : 0);
    }

    /**
     * Respond to UI changes.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        SoundClip soundClip = getClip();

        switch (anEvent.getName()) {

            // Handle ProgressTimer
            case "ProgressTimer" -> {
                if (soundClip.isPlaying())
                    setViewValue("ProgressSlider", soundClip.getTime());
                else stop();
            }

            // Handle PlayButton
            case "PlayButton" -> {
                if (getClip().isPlaying())
                    stop();
                else play();
            }

            // Handle ProgressSlider
            case "ProgressSlider" -> {
                int value = getViewIntValue("ProgressSlider");
                setViewText("TimeLabel", value / 1000 + "." + (value % 1000) / 100); // Update the time label
                if (value != _timer.getTime()) // If not already there, skip there.
                    skip(value);
            }

            // Handle RecordButton
            case "RecordButton" -> {
                if (soundClip.isRecording()) {
                    soundClip.recordStop();
                    setViewText("RecordButton", "Record");
                }
                else {
                    soundClip.recordStart();
                    setViewText("RecordButton", "Stop");
                }
            }

            // Handle SaveButton
            case "SaveButton" -> {
                try { soundClip.save(); }
                catch (Exception e2) { throw new RuntimeException(e2); }
            }
        }
    }
}