package edu.upenn.cis.testing;

import org.junit.runner.Computer;
import org.junit.runner.Runner;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.RunnerScheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author davix
 */
public class GlobalTimeoutParallelComputer extends Computer {
    private final boolean parallelizeMethods;
    private final boolean parallelizeClasses;

    private final ExecutorService executorService;
    private long timeoutNanos;
    private int numTotalClasses;
    private int numCompletedClasses;
    private boolean timedOut;

    public GlobalTimeoutParallelComputer(boolean parallelizeClasses, boolean parallelizeMethods,
                                         ExecutorService executorService, long timeout,
                                         TimeUnit timeUnit) {
        this.parallelizeClasses = parallelizeClasses;
        this.parallelizeMethods = parallelizeMethods;
        this.executorService = executorService;
        this.timeoutNanos = timeUnit.toNanos(timeout);
    }

    public static GlobalTimeoutParallelComputer classes(ExecutorService executorService,
                                                        long timeout, TimeUnit timeUnit) {
        return new GlobalTimeoutParallelComputer(true, false, executorService, timeout, timeUnit);
    }

    private Runner wrap(Runner runner) {
        if (runner instanceof ParentRunner) {
            ((ParentRunner<?>) runner).setScheduler(new RunnerScheduler() {
                private final Collection<Callable<Void>> jobs = new LinkedList<>();

                public void schedule(Runnable childStatement) {
                    jobs.add(() -> {
                        childStatement.run();
                        return null;
                    });
                }

                public void finished() {
                    Instant before = Instant.now();
                    try {
                        List<Future<Void>> futures = executorService.invokeAll(jobs,
                                timeoutNanos, TimeUnit.NANOSECONDS);
                        numTotalClasses = futures.size();
                        int numIncompleteClasses = (int) futures.stream()
                                .filter(Future::isCancelled).count();
                        numCompletedClasses = numTotalClasses - numIncompleteClasses;
                        timedOut = numCompletedClasses != numTotalClasses;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long delta = before.until(Instant.now(), ChronoUnit.NANOS);
                    timeoutNanos -= delta;
                }
            });
        } else {
            System.err.printf("%s: WARNING: Cannot configure runner: %s%n",
                    GlobalTimeoutParallelComputer.class.getCanonicalName(), runner);
        }
        return runner;
    }

    @Override
    public Runner getSuite(RunnerBuilder builder, Class<?>[] classes) throws InitializationError {
        Runner runner = super.getSuite(builder, classes);
        return parallelizeClasses ? wrap(runner) : runner;
    }

    @Override
    protected Runner getRunner(RunnerBuilder builder, Class<?> testClass) throws Throwable {
        Runner runner = super.getRunner(builder, testClass);
        return parallelizeMethods ? wrap(runner) : runner;
    }

    public int getNumCompletedClasses() {
        return numCompletedClasses;
    }

    public int getNumTotalClasses() {
        return numTotalClasses;
    }

    public boolean hasTimedOut() {
        return timedOut;
    }
}
