package com.wavemark.noc.sentryeventsexporter;
import com.wavemark.noc.sentryeventsexporter.settings.ConfigHandler;
import com.wavemark.noc.sentryeventsexporter.settings.Settings;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClass {

    private final static Logger log = LoggerFactory.getLogger(MainClass.class);

    public static String SENTRY_CACHE_DIR="SentryCache";
    public static int SENTRY_RETRY_INTERVAL_SECONDS=3;

    public static void main(String[] args) throws Exception {
        initializeSentry();
        final SentryOfflineErrorReporter sentryOfflineErrorReporter = getSentryOfflineErrorReporter();
       // sentryOfflineErrorReporter.start();
      //  log.error("wc error");
        Settings.getInstance().loadConfig();

           EventsExporter.getInstance().start();

   //    EventsExporter.getInstance().start();
        sentryOfflineErrorReporter.stop();


    }

    private static SentryOfflineErrorReporter getSentryOfflineErrorReporter() {
        return SentryOfflineErrorReporter.SentryOfflineErrorReporterBuilder.aSentryOfflineEventReporter()
                .withCacheDir(SENTRY_CACHE_DIR)
                .withRetryIntervalSeconds(SENTRY_RETRY_INTERVAL_SECONDS)
                .build();
    }

    public static void initializeSentry(){
        Sentry.init(options -> {
            //options.setCacheDirPath();
            options.setCacheDirPath("SentryCache");
            options.setMaxCacheItems(5);
            options.setDsn("https://73ac8d28cdfe4f56adcbe5ee8d7290ae@o815566.ingest.sentry.io/5813490");
        });
    }

}

