package com.wavemark.noc.sentryeventsexporter.settings;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Settings {

    private static Settings instance = null;

    public static Settings getInstance() {
        if (instance == null)
            instance = new Settings();
        return instance;
    }

    private final static Logger log = LoggerFactory.getLogger(Settings.class);

    public static String CONFIG_FILE_PATH = "env.conf";

    public static String EVENT_IDS_FILE_PATH = "event_ids";
    public static String TEMP_DIR_PATH = "tmp";
    public static String PROJECT_CONFIG_FILE_PATH = "project-config.yml";

    private String SENTRY_DSN = "";
    private String SENTRY_HOST = "";
    private String SENTRY_TOKEN = "";
    private String ORG_SLUG = "";

    private String LOOKBACK = "";
    private String OUT_DIR = "json-events";





    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    public String getSentryDsn() {
        return SENTRY_DSN;
    }

    public String getSentryHost() {
        return SENTRY_HOST;
    }

    public String getSentryToken() {
        return SENTRY_TOKEN;
    }

    public String getOrgSlug() {
        return ORG_SLUG;
    }

    public String getLookback() {
        return LOOKBACK;
    }

    public String getOutDir() {
        return OUT_DIR;
    }



    public void loadConfig() {
        configHandler.loadProp();

        final String configSentryDsn = ConfigHandler.getInstance().getProperty("SENTRY_DSN");
        if (configSentryDsn.isEmpty()) {
            log.error("Sentry DSN is empty");
            log.info("Sentry DSN is empty");
            System.exit(1);
        }
        SENTRY_DSN = ConfigHandler.getInstance().getProperty("SENTRY_DSN");

        final String configSentryHost = ConfigHandler.getInstance().getProperty("SENTRY_HOST");
        if (configSentryHost.isEmpty()) {
            log.error("Sentry Host is empty");
            log.info("Sentry Host is empty");
            System.exit(1);
        }
        SENTRY_HOST = ConfigHandler.getInstance().getProperty("SENTRY_HOST");

        final String configSentryToken = ConfigHandler.getInstance().getProperty("SENTRY_TOKEN");
        if (configSentryToken.isEmpty()) {
            log.error("Sentry Token is empty");
            log.info("Sentry Token is empty");
            System.exit(1);
        }
        SENTRY_TOKEN = ConfigHandler.getInstance().getProperty("SENTRY_TOKEN");

        final String configSentryOrg_Slug = ConfigHandler.getInstance().getProperty("ORGANIZATION_SLUG");
        if (configSentryOrg_Slug.isEmpty()) {
            log.error("Sentry organization slug is empty");
            log.info("Sentry organization slug is empty");
            System.exit(1);
        }
        ORG_SLUG = ConfigHandler.getInstance().getProperty("ORGANIZATION_SLUG");

        final String configOutDir = ConfigHandler.getInstance().getProperty("OUT_DIR");
        if (!configOutDir.isEmpty()) {
            OUT_DIR = configOutDir;
        }
        final String configLookBack = ConfigHandler.getInstance().getProperty("LOOKBACK");
        if (!configLookBack.isEmpty()) {
            LOOKBACK=configLookBack;
            try {
                Integer.parseInt(configLookBack);
            } catch (NumberFormatException e) {
                log.error("Lookback should be a number!");
                log.info("Lookback should be a number!");
                System.exit(1);
            }
        }
    }


}
