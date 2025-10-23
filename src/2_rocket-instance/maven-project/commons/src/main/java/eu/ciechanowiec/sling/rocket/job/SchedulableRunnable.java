package eu.ciechanowiec.sling.rocket.job;

/**
 * {@link Runnable} scheduled for periodic execution.
 */
interface SchedulableRunnable extends Runnable {

    String scheduleCycleCronExpression();

    /**
     * Returns a unique {@link SchedulableRunnableID} for this {@link SchedulableRunnable}.
     *
     * @return unique {@link SchedulableRunnableID} for this {@link SchedulableRunnable}
     */
    SchedulableRunnableID id();
}
