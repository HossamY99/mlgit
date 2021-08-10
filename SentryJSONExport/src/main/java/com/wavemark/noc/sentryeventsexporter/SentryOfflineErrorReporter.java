
package com.wavemark.noc.sentryeventsexporter;

//package com.wavemark.noc.cmdagent.sentry;

//import com.wavemark.noc.cmdagent.common.MDCCloseableArray;
import io.sentry.EnvelopeReader;
import io.sentry.Sentry;
import io.sentry.SentryEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
public class SentryOfflineErrorReporter {
    private static final Logger log = LoggerFactory.getLogger(SentryOfflineErrorReporter.class);
    private String cacheDir;
    private int retryIntervalSeconds;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    public void start() {
        Runnable reportingTask = () -> {
                final File[] files = new File(cacheDir).listFiles();
                if (files != null && files.length > 1) {
                    final AtomicInteger reportedOfflineErrors = new AtomicInteger();
                    log.info("--------------------------OFFLINE ERROR REPORTING-----------------------------");
                    Arrays.stream(files).forEach(file -> {
                        if (!file.getName().equals("outbox")) {
                            try (InputStream in = new FileInputStream(file)) {
                                //System.out.println(in.read());
                                System.out.println(file.getName());
                                final SentryEnvelope envelope = new EnvelopeReader().read(in);
                                System.out.println(envelope);
                                try{
                                    Sentry.getCurrentHub().captureEnvelope(envelope);
                                } catch (Exception e) {
                                    System.out.println(e);
                                }

                                System.out.println("tried capturing");
                                Thread.sleep(3000);
                                if (!file.exists()) {
                                    reportedOfflineErrors.getAndIncrement();
                                    System.out.println("reported");
                                } else {
                                    log.debug("Failed to report offline event.");
                                    System.out.println("failed");
                                }
                            } catch (IOException | InterruptedException ex) {
                                System.out.println(ex);
                                log.debug("Unable to report offline event, retrying in " + retryIntervalSeconds + " seconds...", ex);
                                System.out.println("retrying");
                            }
                        }
                    });
                    log.info("Reported " + reportedOfflineErrors + " offline events.");
                }
        };
        executorService.scheduleAtFixedRate(reportingTask, 5, retryIntervalSeconds, TimeUnit.SECONDS);
    }
    public void stop() {
        executorService.shutdownNow();
    }
    public static final class SentryOfflineErrorReporterBuilder {
        private String cacheDir;
        private int retryIntervalSeconds;
        private SentryOfflineErrorReporterBuilder() {
        }
        public static SentryOfflineErrorReporterBuilder aSentryOfflineEventReporter() {
            return new SentryOfflineErrorReporterBuilder();
        }
        public SentryOfflineErrorReporterBuilder withCacheDir(String cacheDir) {
            this.cacheDir = cacheDir;
            return this;
        }
        public SentryOfflineErrorReporterBuilder withRetryIntervalSeconds(int retryIntervalSeconds) {
            this.retryIntervalSeconds = retryIntervalSeconds;
            return this;
        }
        public SentryOfflineErrorReporter build() {
            SentryOfflineErrorReporter sentryOfflineErrorReporter = new SentryOfflineErrorReporter();
            sentryOfflineErrorReporter.retryIntervalSeconds = this.retryIntervalSeconds;
            sentryOfflineErrorReporter.cacheDir = this.cacheDir;
            return sentryOfflineErrorReporter;
        }
    }
}
