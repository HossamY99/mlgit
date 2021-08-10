package com.wavemark.noc.sentryeventsexporter.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.wavemark.noc.sentryeventsexporter.dto.AllProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConfigHandler {

    private static ConfigHandler instance = null;


    Properties props = new Properties();

    private final static Logger log = LoggerFactory.getLogger(ConfigHandler.class);

    public static ConfigHandler getInstance() {
        if (instance == null)
            instance = new ConfigHandler();
        return instance;
    }

    public void loadProp() {
        try (FileInputStream fis = new FileInputStream(Settings.CONFIG_FILE_PATH)) {
            props.load(fis);
        } catch (IOException e) {
            log.error("Error reading config file");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * reads yml file using jackson and stores them in corresponding classes
     *
     * @return AllProjects containing yml Project instances
     * @throws IOException
     */
    public static AllProjects getMappedProjects() throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        return (AllProjects) mapper.readValue(new File(Settings.PROJECT_CONFIG_FILE_PATH), AllProjects.class);
    }

    public String getProperty(final String key) {
        return props.getProperty(key);
    }

}
