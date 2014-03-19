package no.nkk.dogpopulation.concurrent;

import java.util.Date;

/**
 * Metadata for a given task.
 *
 */
public class TaskMetadata {
    private final Thread thread;
    private final Runnable task;
    private final Date start;

    public TaskMetadata(Thread thread, Runnable task, Date start) {
        this.thread = thread;
        this.task = task;
        this.start = start;
    }

    public Thread getThread() {
        return thread;
    }

    public Runnable getTask() {
        return task;
    }

    public Date getStart() {
        return start;
    }

    @Override
    public String toString() {
        return "TaskMetadata{" +
                "thread=" + thread +
                ", task=" + task +
                ", start=" + start +
                '}';
    }

    public String shortStatus() {
        long durationSeconds = (System.currentTimeMillis() - start.getTime()) / 1000;
        return String.format("%d sec, %s", durationSeconds, task.toString());
    }
}