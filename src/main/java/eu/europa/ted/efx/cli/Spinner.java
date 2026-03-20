package eu.europa.ted.efx.cli;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A terminal spinner that shows progress while a long-running task executes.
 *
 * <p>While active, intercepts {@link System#out} and {@link System#err} so that any output
 * (log messages, println calls) clears the spinner line first, prints the message, then
 * redraws the spinner — preventing garbled output.</p>
 *
 * <p>Usage:
 * <pre>
 * try (Spinner spinner = new Spinner("Translating rules")) {
 *     doWork();
 * }
 * </pre>
 */
public class Spinner implements AutoCloseable {

    private static final String[] FRAMES = { "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏" };
    private static final int INTERVAL_MS = 80;

    private final Object lock = new Object();
    private final Thread thread;
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private final String message;
    private volatile boolean running = true;
    private int spinnerLineLength;

    public Spinner(final String message) {
        this.message = message;
        this.originalOut = System.out;
        this.originalErr = System.err;

        if (!isTty()) {
            this.thread = null;
            return;
        }

        System.setOut(new SpinnerAwarePrintStream(this.originalOut));
        System.setErr(new SpinnerAwarePrintStream(this.originalErr));

        this.thread = new Thread(() -> {
            int i = 0;
            while (this.running) {
                synchronized (this.lock) {
                    final String frame = "\r\033[36m" + FRAMES[i % FRAMES.length] + " " + message + "\033[0m";
                    this.spinnerLineLength = message.length() + 3;
                    this.originalOut.print(frame);
                    this.originalOut.flush();
                }
                i++;
                try {
                    Thread.sleep(INTERVAL_MS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        this.thread.setDaemon(true);
        this.thread.start();
    }

    private static boolean isTty() {
        return System.console() != null;
    }

    @Override
    public void close() {
        this.running = false;
        if (this.thread == null) {
            return;
        }
        try {
            this.thread.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (this.lock) {
            this.originalOut.print("\r" + " ".repeat(this.spinnerLineLength) + "\r");
            this.originalOut.flush();
        }
        System.setOut(this.originalOut);
        System.setErr(this.originalErr);
    }

    private void clearAndReprint(final PrintStream target, final byte[] data) {
        synchronized (this.lock) {
            target.print("\r" + " ".repeat(this.spinnerLineLength) + "\r");
            target.write(data, 0, data.length);
            target.flush();
            if (this.running) {
                final String redraw = "\033[36m" + FRAMES[0] + " " + this.message + "\033[0m";
                this.originalOut.print(redraw);
                this.originalOut.flush();
            }
        }
    }

    private class SpinnerAwarePrintStream extends PrintStream {
        private final PrintStream target;

        SpinnerAwarePrintStream(final PrintStream target) {
            super(new ByteArrayOutputStream(0), false);
            this.target = target;
        }

        @Override
        public void write(final byte[] buf, final int off, final int len) {
            final byte[] data = new byte[len];
            System.arraycopy(buf, off, data, 0, len);
            clearAndReprint(this.target, data);
        }

        @Override
        public void write(final int b) {
            clearAndReprint(this.target, new byte[] { (byte) b });
        }

        @Override
        public void flush() {
            this.target.flush();
        }
    }
}
