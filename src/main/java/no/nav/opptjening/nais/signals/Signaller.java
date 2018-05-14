package no.nav.opptjening.nais.signals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public interface Signaller {

    boolean signalled();
    void signal();

    interface SignalListener {
        void onSignal();
    }

    class BlockingSignaller implements Signaller {

        private volatile boolean signalled;

        public boolean signalled() {
            return signalled;
        }

        @Override
        public void signal() {
            synchronized (this) {
                signalled = true;
                notifyAll();
            }
        }

        public void waitForSignal() throws InterruptedException {
            synchronized (this) {
                while (!signalled) {
                    wait();
                }
            }
        }
    }

    class CallbackSignaller extends BlockingSignaller {

        private static final Logger LOG = LoggerFactory.getLogger(Signaller.class);

        private final List<SignalListener> listeners;

        private final Thread notifyThread;

        public CallbackSignaller() {
            this.listeners = new LinkedList<>();
            this.notifyThread = new Thread(() -> {
                try {
                    waitForSignal();
                } catch (InterruptedException e) {
                    LOG.error("Thread got interrupted while waiting for signal", e);
                    return;
                }

                signalListeners();
            });

            this.notifyThread.start();
        }

        public Thread getNotifyThread() {
            return notifyThread;
        }

        public void addListener(SignalListener listener) {
            listeners.add(listener);
        }

        private void signalListeners() {
            for (SignalListener l : listeners) {
                try {
                    l.onSignal();
                } catch (Exception e) {
                    LOG.error("Signal listener threw exception", e);
                }
            }
        }
    }
}
