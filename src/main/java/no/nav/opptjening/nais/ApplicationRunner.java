package no.nav.opptjening.nais;

import no.nav.opptjening.nais.signals.Signaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class ApplicationRunner {
    static final int GRACEFUL_TERMINATION_TIMEOUT = 5000;

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationRunner.class);

    private final ThreadPoolExecutor executorService;
    /**
     * a task that should run forever, or as long as the application should run
     */
    private final Runnable main;
    /**
     * other tasks that runs in the background. their exit status doesn't affect
     * the application
     */
    private final Runnable[] tasks;

    private final List<ApplicationShutdownListener> shutdownListeners;

    private final Signaller.CallbackSignaller shutdownSignal = new Signaller.CallbackSignaller();

    public ApplicationRunner(Runnable main, Runnable... tasks) {
        this.main = main;
        this.tasks = tasks;
        this.executorService = new DefaultFixedThreadPoolExecutor(1 + tasks.length);
        this.shutdownListeners = new LinkedList<>();

        this.shutdownSignal.addListener(this::shutdownAndAwaitTermination);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.debug("Shutdown hook called, signalling shutdown");
            shutdown();
        }));
    }

    public void shutdown() {
        LOG.debug("Received shutdown signal");
        shutdownSignal.signal();
    }

    public void addShutdownListener(ApplicationShutdownListener l) {
        shutdownListeners.add(l);
    }

    private void shutdownAndAwaitTermination() {
        executorService.shutdownNow();
        try {
            LOG.info("Waiting up to {} ms before shutting down", GRACEFUL_TERMINATION_TIMEOUT);
            if (executorService.awaitTermination(GRACEFUL_TERMINATION_TIMEOUT, TimeUnit.MILLISECONDS)) {
                LOG.info("Shutdown gracefully");
            } else {
                LOG.warn("Shutdown timeout");
            }
        } catch (InterruptedException e) {
            LOG.error("Got interrupted while awaiting termination", e);
        }

        LOG.debug("Calling shutdown listeners");
        for (ApplicationShutdownListener l : shutdownListeners) {
            try {
                l.onShutdown();
            } catch (Exception e) {
                LOG.error("Shutdown listener threw exception", e);
            }
        }

        LOG.info("Exiting app, goodbye");
    }

    public void run() {
        boolean shutdownOk = runTasksAndWait();

        if (!shutdownOk) {
            LOG.info("Stopping because main task shutdown");
        } else {
            LOG.info("Stopping because executor service shutdown");
        }

        LOG.info("Exiting main loop");
        System.exit(!shutdownOk ? 1 : 0);
    }

    private Future<?> scheduleTasks() {
        Future<?> future = executorService.submit(main);
        for (Runnable r : tasks) {
            executorService.execute(r);
        }
        return future;
    }

    public boolean runTasksAndWait() {
        Future<?> mainTask = scheduleTasks();

        while (!executorService.isShutdown() && !mainTask.isDone()) {
            /* do nothing while mainTask is running */
            Thread.yield();
        }

        /* main task should never exit, only in case of failure */
        return !mainTask.isDone();
    }

    private static class DefaultFixedThreadPoolExecutor extends ThreadPoolExecutor {

        private static final Logger LOG = LoggerFactory.getLogger(DefaultFixedThreadPoolExecutor.class);

        public DefaultFixedThreadPoolExecutor(int poolSize) {
            super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>());
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            if (t == null && r instanceof Future<?>) {
                try {
                    ((Future<?>) r).get();
                } catch (CancellationException ce) {
                    t = ce;
                } catch (ExecutionException ee) {
                    t = ee.getCause();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // ignore/reset
                }
            }

            if (t != null) {
                LOG.error("Uncaught exception in runnable {}", r, t);
            } else {
                LOG.debug("Runnable ({}) exited", r);
            }
        }

        @Override
        protected void terminated() {
            super.terminated();
            LOG.debug("Thread pool executor terminated");
        }
    }

    public interface ApplicationShutdownListener {
        void onShutdown();
    }
}
