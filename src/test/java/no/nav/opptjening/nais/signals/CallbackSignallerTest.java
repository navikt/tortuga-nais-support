package no.nav.opptjening.nais.signals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CallbackSignallerTest {
    private Signaller.CallbackSignaller signaller;
    private AtomicInteger calledCount;
    private CountDownLatch callbacksCalled;
    private Thread t1;
    private Thread t2;

    @Before
    public void setUp() {
        signaller = new Signaller.CallbackSignaller();

        calledCount = new AtomicInteger(0);
        callbacksCalled = new CountDownLatch(2);

        CallbackTask c1 = new CallbackTask(callbacksCalled, calledCount);
        CallbackTask c2 = new CallbackTask(callbacksCalled, calledCount);
        t1 = new Thread(c1);
        t2 = new Thread(c2);

        t1.start();
        t2.start();

        signaller.addListener(c1);
        signaller.addListener(c2);
    }

    @Test
    public void that_CallbacksaAreCalled_When_Signalled() throws InterruptedException {
        signaller.signal();

        callbacksCalled.await(250, TimeUnit.MILLISECONDS);

        t1.interrupt();
        t2.interrupt();
    }

    @Test
    public void that_CallbacksAreCalledOnce_When_SignalledTwice() throws InterruptedException {
        signaller.signal();
        signaller.signal();

        callbacksCalled.await(250, TimeUnit.MILLISECONDS);

        t1.interrupt();
        t2.interrupt();

        Assert.assertEquals(2, calledCount.get());
    }

    private static class CallbackTask implements Runnable, Signaller.SignalListener {
        private final CountDownLatch latch;
        private final AtomicInteger calledCount;

        public CallbackTask(CountDownLatch latch, AtomicInteger calledCount) {
            this.latch = latch;
            this.calledCount = calledCount;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.yield();
            }
        }

        @Override
        public void onSignal() {
            calledCount.incrementAndGet();
            latch.countDown();
        }
    }
}
