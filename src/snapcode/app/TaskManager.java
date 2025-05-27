package snapcode.app;
import snap.geom.Pos;
import snap.props.PropChange;
import snap.util.TaskMonitor;
import snap.util.TaskRunner;
import snap.view.*;
import java.util.Objects;

/**
 * This class manages a list of tasks.
 */
public class TaskManager extends ViewOwner {

    // Whether task manager is running tasks
    private boolean _runningTasks;

    // The activity text
    private String _activityText;

    // The progress
    private double _progress;

    // The ActivityLabel
    private Label _activityLabel;

    // The ProgressBar
    private ProgressBar _progressBar;

    // Constants for properties
    private static final String RunningTasks_Prop = "RunningTasks";
    private static final String ActivityText_Prop = "ActivityText";
    private static final String Progress_Prop = "Progress";

    /**
     * Constructor.
     */
    public TaskManager()
    {
        super();
    }

    /**
     * Creates a task.
     */
    public TaskManagerTask<?> createTask()  { return new TaskManagerTask<>(this); }

    /**
     * Returns whether task manager is running tasks.
     */
    public boolean isRunningTasks()  { return _runningTasks; }

    /**
     * Sets whether task manager is running tasks.
     */
    public void setRunningTasks(boolean aValue)
    {
        if (aValue == isRunningTasks()) return;
        firePropChange(RunningTasks_Prop, _runningTasks, _runningTasks = aValue);
        resetLater();
    }

    /**
     * Returns the activity text.
     */
    public String getActivityText()  { return _activityText; }

    /**
     * Sets the activity text.
     */
    public void setActivityText(String aValue)
    {
        if (Objects.equals(aValue, getActivityText())) return;
        firePropChange(ActivityText_Prop, _activityText, _activityText = aValue);
        resetLater();
    }

    /**
     * Returns the progress.
     */
    public double getProgress()  { return _progress; }

    /**
     * Sets the progress.
     */
    public void setProgress(double aValue)
    {
        if (aValue == getProgress()) return;
        firePropChange(Progress_Prop, _progress, _progress = aValue);
        resetLater();
    }

    /**
     * Returns the UI.
     */
    @Override
    protected View createUI()
    {
        // Create ActivityLabel
        _activityLabel = new Label();

        // Create ProgressBar
        _progressBar = new ProgressBar();
        _progressBar.setPrefSize(100, 16);

        // Create row and add ActivityLabel and ProgressBar
        RowView rowView = new RowView();
        rowView.setMargin(0, 40, 5, 0);
        rowView.setLean(Pos.BOTTOM_RIGHT);
        rowView.setManaged(false);
        rowView.setChildren(_activityLabel, _progressBar);
        rowView.setSizeToPrefSize();

        // Return
        return rowView;
    }

    /**
     * Resets the UI.
     */
    @Override
    protected void resetUI()
    {
        // Update visible
        boolean isRunningTasks = isRunningTasks();
        getUI().setVisible(isRunningTasks);
        if (!isRunningTasks)
            return;

        getUI().setSizeToPrefSize();
        getUI().getParent().relayout();

        // Update ActivityLabel, ProgressBar
        setViewValue(_activityLabel, getActivityText());
        double progress = getProgress();
        _progressBar.setIndeterminate(progress >= 0);
        if (progress >= 0)
            _progressBar.setProgress(progress);
    }

    /**
     * Called when task monitor has prop change.
     */
    protected void handleTaskPropChange(TaskManagerTask<?> task, PropChange propChange)
    {
        switch (propChange.getPropName()) {

            // Handle TaskRunner Status change
            case TaskRunner.Status_Prop:
                setRunningTasks(task.getTaskRunner().getStatus() == TaskRunner.Status.Running);
                break;

            // Handle TaskMonitor TaskTitle or TaskWorkUnitIndex change
            case TaskMonitor.TaskTitle_Prop: setActivityText(task.getTaskMonitor().getTaskTitle()); break;
            case TaskMonitor.TaskWorkUnitIndex_Prop: setProgress(task.getTaskMonitor().getTaskProgress()); break;
        }
    }
}
