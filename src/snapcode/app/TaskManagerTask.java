package snapcode.app;
import snap.props.PropChange;
import snap.util.TaskMonitor;
import snap.util.TaskRunner;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * This class represents a background task to be run by the workspace pane.
 */
public class TaskManagerTask<T> {

    // The workspace pane
    private TaskManager _taskManager;

    // The task monitor
    private TaskMonitor _taskMonitor;

    // The task runner
    private TaskRunner<T> _taskRunner;

    /**
     * Constructor.
     */
    public TaskManagerTask(TaskManager taskManager)
    {
        super();
        _taskManager = taskManager;
        _taskMonitor = new TaskMonitor();
        _taskRunner = new TaskRunner<>();
        _taskRunner.setMonitor(_taskMonitor);

        _taskMonitor.addPropChangeListener(this::handleTaskMonitorPropChange);
        _taskRunner.addPropChangeListener(this::handleTaskRunnerPropChange);
    }

    /**
     * Returns the task monitor.
     */
    public TaskMonitor getTaskMonitor()  { return _taskMonitor; }

    /**
     * Returns the task runner.
     */
    public TaskRunner<?> getTaskRunner()  { return _taskRunner; }

    /**
     * Returns a string to describe task.
     */
    public String getTitle()  { return _taskMonitor.getTitle(); }

    /**
     * Sets a string to describe task.
     */
    public void setTitle(String aValue)  { _taskMonitor.setTitle(aValue); }

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
    public void start()  { _taskRunner.start(); }

    /**
     * Called when task monitor has prop change.
     */
    private void handleTaskMonitorPropChange(PropChange propChange)
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
