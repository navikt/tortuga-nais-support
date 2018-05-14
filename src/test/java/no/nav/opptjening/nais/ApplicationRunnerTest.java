package no.nav.opptjening.nais;

import no.nav.opptjening.nais.ApplicationRunner;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ApplicationRunnerTest {

    @Test
    public void that_ApplicationStops_When_MainTaskStops() {
        ApplicationRunner appRunner = new ApplicationRunner(() -> {
            // return immediately
        }, () -> {
            // run until interrupted
            while (!Thread.currentThread().isInterrupted()) {
                Thread.yield();
            }
        });

        Assert.assertFalse(appRunner.runTasksAndWait());
    }

    @Test
    public void that_ListenersAreCalled_When_ApplicationStops() throws InterruptedException {
        ApplicationRunner appRunner = new ApplicationRunner(() -> {
            // return immediately
        });

        final CountDownLatch latch = new CountDownLatch(1);
        appRunner.addShutdownListener(latch::countDown);

        appRunner.runTasksAndWait();

        latch.await(50, TimeUnit.MILLISECONDS);
    }

    @Test
    public void that_ThreadsAreKilledIfNotStoppedBeforeTimeout_When_ApplicationStops() throws InterruptedException {
        ApplicationRunner appRunner = new ApplicationRunner(() -> {
            while (true) {
                Thread.yield();
            }
        }, () -> {
            // run until interrupted
            while (true) {
                Thread.yield();
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        appRunner.addShutdownListener(latch::countDown);

        new Thread(() -> {
            try {
                Thread.sleep(100L);
                appRunner.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        Assert.assertTrue(appRunner.runTasksAndWait());

        latch.await(ApplicationRunner.GRACEFUL_TERMINATION_TIMEOUT, TimeUnit.MILLISECONDS);
    }

}
