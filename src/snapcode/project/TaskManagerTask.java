package snapcode.project;
import snap.props.PropChange;
import snap.util.ActivityMonitor;
import snap.util.TaskRunner;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * This class represents a background task to be run by the workspace pane.
 */
public class TaskManagerTask<T> {

    // The workspace pane
    private TaskManager _taskManager;

    // The activity monitor
    private ActivityMonitor _activityMonitor;

    // The task runner
    private TaskRunner<T> _taskRunner;

    /**
     * Constructor.
     */
    public TaskManagerTask(TaskManager taskManager)
    {
        super();
        _taskManager = taskManager;
        _activityMonitor = new ActivityMonitor();
        _taskRunner = new TaskRunner<>();
        _taskRunner.setMonitor(_activityMonitor);
    }

    /**
     * Returns the activity monitor.
     */
    public ActivityMonitor getActivityMonitor()  { return _activityMonitor; }

    /**
     * Returns the task runner.
     */
    public TaskRunner<?> getTaskRunner()  { return _taskRunner; }

    /**
     * Returns a string to describe task.
     */
    public String getTitle()  { return _activityMonitor.getTitle(); }

    /**
     * Sets a string to describe task.
     */
    public void setTitle(String aValue)  { _activityMonitor.setTitle(aValue); }

    /**
     * Returns the task callable function.
     */
    public Callable<T> getTaskFunction()  { return _taskRunner.getTaskFunction(); }

    /**
     * Sets the task callable function.
     */
    public void setTaskFunction(Callable<T> aCallable)  { _taskRunner.setTaskFunction(aCallable); }

    /**
     * Sets the success handler.
     */
    public void setOnSuccess(Consumer<T> aHandler)  { _taskRunner.setOnSuccess(aHandler); }

    /**
     * Sets the failure handler.
     */
    public void setOnFailure(Consumer<Exception> aHandler)  { _taskRunner.setOnFailure(aHandler); }

    /**
     * Sets the cancelled handler.
     */
    public void setOnCancelled(Runnable aRun)  { _taskRunner.setOnCancelled(aRun); }

    /**
     * Sets the finished handler.
     */
    public void setOnFinished(Runnable aRun)  { _taskRunner.setOnFinished(aRun); }

    /**
     * Starts the runner.
     */
    public void start()
    {
        _activityMonitor.addPropChangeListener(this::handleActivityMonitorPropChange);
        _taskRunner.addPropChangeListener(this::handleTaskRunnerPropChange);
        _taskManager.startTask(this);
    }

    /**
     * Called when activity monitor has prop change.
     */
    private void handleActivityMonitorPropChange(PropChange propChange)
    {
        _taskManager.handleTaskPropChange(this, propChange);
    }

    /**
     * Called when task runner has prop change.
     */
    private void handleTaskRunnerPropChange(PropChange propChange)
    {
        _taskManager.handleTaskPropChange(this, propChange);
    }
}
