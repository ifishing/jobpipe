package org.deephacks.jobpipe;

import java.util.Optional;

public class TaskStatus {
  private TaskContext context;
  private Throwable failReason;
  private TaskContext failedDep;
  private TaskStatusCode code;
  private long lastUpdate = 0;
  private final JobObserver observer;
  private final boolean verbose;

  TaskStatus(TaskContext context, JobObserver observer, boolean verbose) {
    this.context = context;
    this.observer = observer;
    this.verbose = verbose;
  }

  public Optional<Throwable> getFailReason() {
    return Optional.ofNullable(failReason);
  }

  public TaskStatusCode code() {
    return code;
  }

  public boolean isDone() {
    return code != TaskStatusCode.NEW
      && code != TaskStatusCode.RUNNING
      && code != TaskStatusCode.SCHEDULED;
  }

  public boolean hasFailed() {
    return TaskStatusCode.ERROR_DEPENDENCY == code ||
      TaskStatusCode.ERROR_EXECUTE == code ||
      TaskStatusCode.ERROR_NO_INPUT == code ||
      TaskStatusCode.ERROR_SIGTERM == code ||
      TaskStatusCode.ERROR_ABORTED == code;
  }

  public TaskContext getContext() {
    return context;
  }

  public long getLastUpdate() {
    return lastUpdate;
  }

  boolean setCode(TaskStatusCode code) {
    if (hasFailed()) {
      return false;
    }
    if (this.code != code) {
      this.code = code;
      Debug.debug(context + " -> " + this.code, verbose);
      if (code == TaskStatusCode.ERROR_EXECUTE) {
        Debug.debug((Throwable) this.failReason, verbose);
      }
    }
    setLastUpdate();
    try {
      return observer != null ? observer.notify(this) : true;
    } catch (Throwable e) {
      Debug.debug(e, verbose);
      return false;
    }
  }

  boolean newTask() {
    return setCode(TaskStatusCode.NEW);
  }

  void failed(Throwable e) {
    this.failReason = e;
    setCode(TaskStatusCode.ERROR_EXECUTE);
  }

  void failedDep(TaskContext failedDep) {
    this.failedDep = failedDep;
    setCode(TaskStatusCode.ERROR_DEPENDENCY);
  }

  void failedDepNoInput(TaskContext failedDep) {
    this.failedDep = failedDep;
    setCode(TaskStatusCode.ERROR_NO_INPUT);
  }

  void finished() {
    setCode(TaskStatusCode.FINISHED);
  }

  void skipped() {
    setCode(TaskStatusCode.SKIPPED);
  }

  boolean running() {
    return setCode(TaskStatusCode.RUNNING);
  }

  boolean scheduled() {
    return setCode(TaskStatusCode.SCHEDULED);
  }

  void setLastUpdate() {
    this.lastUpdate = System.currentTimeMillis();
  }

  public void abort() {
    setCode(TaskStatusCode.ERROR_ABORTED);
  }

  public enum TaskStatusCode {
    /** the task has just been created */
    NEW,
    /** the task has been scheduled for execution */
    SCHEDULED,
    /** the task has completed successfully */
    FINISHED,
    /** the output for this task already exist */
    SKIPPED,
    /** the task is running {@link org.deephacks.jobpipe.Task#execute(TaskContext)} */
    RUNNING,
    /** input to this task did not exist */
    ERROR_NO_INPUT,
    /** {@link org.deephacks.jobpipe.Task#execute(TaskContext)} threw a runtime exception */
    ERROR_EXECUTE,
    /** a dependency has failed which also failed this task */
    ERROR_DEPENDENCY,
    /** observer aborted the task */
    ERROR_ABORTED,
    /** the program aborted with SIGTERM */
    ERROR_SIGTERM
  }
}
