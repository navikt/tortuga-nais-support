package no.nav.opptjening.nais.signals;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockingSignallerTest {
    @Test
    public void that_WaitingBlocks_Until_When_Signalled() throws InterruptedException {
        Signaller.BlockingSignaller signaller = new Signaller.BlockingSignaller();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final CountDownLatch threadStopped = new CountDownLatch(1);

        Thread t1 = new Thread(() -> {
            try {
                threadStarted.countDown();
                signaller.waitForSignal();
                threadStopped.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.start();

        threadStarted.await(500, TimeUnit.MILLISECONDS);

        signaller.signal();

        threadStopped.await(500, TimeUnit.MILLISECONDS);
        t1.interrupt();
    }

    @Test
    public void that_SignallingTwiceDoesNotDoAnything() throws InterruptedException {
        Signaller.BlockingSignaller signaller = new Signaller.BlockingSignaller();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final CountDownLatch threadStopped = new CountDownLatch(1);
        final AtomicInteger calledCount = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            try {
                threadStarted.countDown();
                signaller.waitForSignal();
                calledCount.incrementAndGet();
                threadStopped.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.start();

        threadStarted.await(500, TimeUnit.MILLISECONDS);

        signaller.signal();
        signaller.signal();

        threadStopped.await(500, TimeUnit.MILLISECONDS);
        t1.interrupt();

        Assert.assertEquals(1, calledCount.get());
    }
}
