package com.wavemark.noc.sentryeventsexporter;

import com.wavemark.noc.sentryeventsexporter.dto.AllProjects;
import com.wavemark.noc.sentryeventsexporter.dto.Project;
import com.wavemark.noc.sentryeventsexporter.settings.ConfigHandler;
import com.wavemark.noc.sentryeventsexporter.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventsExporter {

    private static EventsExporter instance = null;

    private final static Logger log = LoggerFactory.getLogger(EventsExporter.class);

    public static List<Thread> allThreadsList = new ArrayList<>();

    private static final String SENTRY_DSN;
    private static final String SENTRY_HOST;
    private static final String SENTRY_TOKEN;

    private static final Date date = new Date();

    public static String getSentryDsn() {
        return SENTRY_DSN;
    }

    public static String getSentryHost() {
        return SENTRY_HOST;
    }

    public static String getSentryToken() {
        return SENTRY_TOKEN;
    }

    public static String getOrgSlug() {
        return ORG_SLUG;
    }

    public static int getLookback() {
        return LOOKBACK;
    }

    public static Date getDate() {
        return date;
    }

    public static String getOutDir() {
        return OUT_DIR;
    }

    private static final String ORG_SLUG;
    private static final String OUT_DIR;
    private static final int LOOKBACK;

    public static EventsExporter getInstance() {
        if (instance == null)
            instance = new EventsExporter();
        return instance;
    }

    private static final Settings settingsInstance = Settings.getInstance();

    static {
        SENTRY_DSN = settingsInstance.getSentryDsn();
        SENTRY_HOST = settingsInstance.getSentryHost();
        SENTRY_TOKEN = settingsInstance.getSentryToken();
        ORG_SLUG = settingsInstance.getOrgSlug();
        if (settingsInstance.getOutDir().isEmpty())
        {
            OUT_DIR = "json-events";
        }
        else{OUT_DIR = (settingsInstance.getOutDir());}
        if(settingsInstance.getLookback().isEmpty())
        {LOOKBACK=5000;}
        else{
            LOOKBACK=Integer.parseInt(settingsInstance.getLookback());
        }
    }


    /**
     * This starts the program. It maps the projects in the yaml file to their designated classes
     * Then it loops through each project and creates a thread for each and starts it
     * Then after it waits for threads to finish executing, it moves all files in tmp to the out dir
     * Then
     */
    public void start() throws InterruptedException {
        log.info("---------------------------------------Starting Exporter---------------------------------------");
        final long startTime = System.currentTimeMillis();

        createDirectories();

        AllProjects allProjects = null;
        try {
            allProjects = ConfigHandler.getMappedProjects();
        } catch (IOException ex) {
            log.error("Error parsing project config", ex);
            System.exit(1);
        }
        if (allProjects != null) {

            for (Project project : allProjects.getProjects()) {
                Thread thread = new Thread(project);
                thread.start();
                allThreadsList.add(thread);
            }

            for (Thread thread : allThreadsList) {
                thread.join();
            }

            for (Project project : allProjects.getProjects()) {
                log.info("Api calls taken for {} is {} seconds", project.getName(), project.apiCallsTotalTime);
            }

            //used a different function in project in Project class to move individual files when done
            //then this moves any remaining files (in case something goes wrong in one of the project files)
            final String extension = ".json";
            final File sourceDir = new File(Settings.TEMP_DIR_PATH);
            final File[] files = sourceDir.listFiles((File pathname) -> pathname.getName().endsWith(extension));

            FileUtil.moveFiles(files, Settings.TEMP_DIR_PATH, OUT_DIR);
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("Finished in " + (int) elapsedTime / 1000 + " seconds");
    }

    public void createDirectories() {
        log.info("Creating required directories if missing");
        Util.createDirectoryIfMissing(Settings.EVENT_IDS_FILE_PATH);
        Util.createDirectoryIfMissing(Settings.TEMP_DIR_PATH);
        Util.createDirectoryIfMissing(OUT_DIR);
    }

}
