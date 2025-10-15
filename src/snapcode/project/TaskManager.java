package snapcode.project;
import snap.geom.Pos;
import snap.props.PropChange;
import snap.util.TaskMonitor;
import snap.util.TaskRunner;
import snap.view.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class manages a list of tasks.
 */
public class TaskManager extends ViewOwner {

    // The currently running tasks
    private List<TaskManagerTask<?>> _tasks;

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
    public static final String RunningTasks_Prop = "RunningTasks";
    public static final String ActivityText_Prop = "ActivityText";
    public static final String Progress_Prop = "Progress";

    /**
     * Constructor.
     */
    public TaskManager()
    {
        super();
        _tasks = new ArrayList<>();
    }

    /**
     * Creates a task.
     */
    public TaskManagerTask<?> createTask()  { return new TaskManagerTask<>(this); }

    /**
     * Returns the main task.
     */
    public TaskManagerTask<?> getMainTask()  { return !_tasks.isEmpty() ? _tasks.get(0) : null; }

    /**
     * Starts the given task.
     */
    public void startTask(TaskManagerTask<?> aTask)
    {
        _tasks.add(aTask);
        aTask.getTaskRunner().start();
        setRunningTasks(!_tasks.isEmpty());
        resetLater();
    }

    /**
     * Removes the given task.
     */
    private void removeTask(TaskManagerTask<?> aTask)
    {
        _tasks.remove(aTask);
        setRunningTasks(!_tasks.isEmpty());
        resetLater();
    }

    /**
     * Returns whether task manager is running tasks.
     */
    public boolean isRunningTasks()  { return _runningTasks; }

    /**
     * Sets whether task manager is running tasks.
     */
    protected void setRunningTasks(boolean aValue)
    {
        if (aValue == isRunningTasks()) return;
        firePropChange(RunningTasks_Prop, _runningTasks, _runningTasks = aValue);
        if (aValue)
            _progress = 0;
        if (_progressBar != null)
            _progressBar.setProgress(0);
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
    protected void setProgress(double aValue)
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
        _activityLabel.setMargin(0, 8, 0, 0);

        // Create ProgressBar
        _progressBar = new ProgressBar();
        _progressBar.setPrefSize(100, 16);

        // Create row and add ActivityLabel and ProgressBar
        RowView rowView = new RowView();
        rowView.setMargin(0, 40, 5, 0);
        rowView.setLean(Pos.BOTTOM_RIGHT);
        rowView.setManaged(false);
        rowView.setChildren(_activityLabel, _progressBar);

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

        // Update ActivityLabel
        setViewValue(_activityLabel, getActivityText());

        // Clear any previous animation
        _progressBar.getAnimCleared(0);

        // If
        TaskManagerTask<?> mainTask = getMainTask();
        if (mainTask != null && mainTask.getTaskMonitor().isIndeterminate())
            _progressBar.setIndeterminate(true);

        else {
            _progressBar.setIndeterminate(false);

            // Update ProgressBar
            double progress = getProgress();
            if (progress > 0) {
                if (progress < _progressBar.getProgress())
                    _progressBar.setProgress(0);
                _progressBar.getAnim(500).setValue(ProgressBar.Progress_Prop, progress).play();
            }
            else _progressBar.setProgress(progress);
        }

        // Resize and relayout
        getUI().setSizeToPrefSize();
        getUI().getParent().relayout();
    }

    /**
     * Called when task has prop change.
     */
    protected void handleTaskPropChange(TaskManagerTask<?> task, PropChange propChange)
    {
        // If not main task, just return
        if (task != getMainTask()) {
            if (propChange.getPropName().equals(TaskRunner.Status_Prop) && task.getTaskRunner().getStatus() != TaskRunner.Status.Running)
                runLater(() -> removeTask(task));
            return;
        }

        switch (propChange.getPropName()) {

            // Handle TaskRunner Status change
            case TaskRunner.Status_Prop -> handleTaskStatusChange(task);

            // Handle TaskMonitor TaskTitle, TaskWorkUnitIndex change
            case TaskMonitor.TaskTitle_Prop -> setActivityText(task.getTaskMonitor().getTaskTitle());
            case TaskMonitor.TaskIndex_Prop,
                 TaskMonitor.TaskWorkUnitIndex_Prop -> setProgress(task.getTaskMonitor().getTaskProgress());
        }
    }

    /**
     * Called when task has status change to update RunningTasks property.
     */
    private void handleTaskStatusChange(TaskManagerTask<?> task)
    {
        TaskRunner.Status taskStatus = task.getTaskRunner().getStatus();

        // If finished, set progress to 1 and remove task after delay so progress bar finishes
        if (taskStatus == TaskRunner.Status.Finished) {
            setProgress(1);
            runDelayed(() -> removeTask(task), 500);
        }

        // Otherwise, if not running, remove
        else if (taskStatus != TaskRunner.Status.Running)
            runLater(() -> removeTask(task));
    }
}
