package com.wavemark.noc.sentryeventsexporter;

import com.wavemark.noc.sentryeventsexporter.settings.Settings;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.Queue;

public class EventFileFunctions {

//a
    public Queue<String> queue = new LinkedList<>();

    static Logger log = LoggerFactory.getLogger(EventFileFunctions.class);


    /**
     * checks if event is already exported by checking if event id is in the eventsid file of the project
     *
     * @param id the event id
     * @return boolean to indicate if exported
     * @throws IOException
     */
    public boolean checkIfExported(String id, String eventFileName) throws IOException {
        //check if it exist
        File file = new File(Settings.EVENT_IDS_FILE_PATH +File.separator + eventFileName);
        if (!file.isFile()) {
            return false;
        } else {
            return isEventExported(file, id);
        }
    }

    public static void moveJsonFile(String JsonFileName) throws IOException {
        File file = new File(Settings.TEMP_DIR_PATH +File.separator + JsonFileName);
        if (file.isFile()) {
                Files.move(Paths.get(Settings.TEMP_DIR_PATH + File.separator + JsonFileName),
                        Paths.get(EventsExporter.getOutDir() + File.separator + JsonFileName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void checkEventFileSize(File file) {
        if (queue.size() >= 2 * EventsExporter.getLookback()) {
            log.debug("file capacity reached");
            while (queue.size() >= 2 * EventsExporter.getLookback()) {
                queue.remove();
                FileUtil.removeFirstLineFromFile(file);
            }
        }
    }

    /**
     * called by isEventExported,
     * checks if event is already exported by checking if event id is in the eventsid file of the project
     *
     * @param file the target file that contains event ids of exported events
     * @param id the id of the event to be exported
     * @return boolean to indicate if event exported
     */
    public boolean isEventExported(File file, String id) {

        BufferedReader bufferedReader = null;

        if (queue.isEmpty()) {
            try {
                bufferedReader = new BufferedReader(new FileReader(file));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    queue.add(line);
                }
            } catch (FileNotFoundException e) {
                log.error(String.valueOf(e));
            } catch (IOException e) {
                log.error(String.valueOf(e));
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.error(String.valueOf(e));
                }
            }

            if (queue.contains(id)) {
                return true;
            }
        }
        //queue not empty, already read from file
        if (queue.contains(id)) {
            return true;
        } else {
            //check if queue reached max size
            checkEventFileSize(file);
            return false;
        }
    }

    public void appendEventToJson(JSONObject newEvent, String fileName, String name) throws IOException {
        try {
            String eventString=newEvent.toString();
            String eventStringNoBrackets=eventString.substring(1, eventString.length() - 1);
            FileWriter file = new FileWriter(EventsExporter.getOutDir()+File.separator + fileName, true);
            file.write(eventString + "\n");
            //file.write(eventStringNoBrackets + "\n");
            file.flush();
            file.close();
            markEventAsExported(newEvent.getString("id"),name);

        } catch (IOException e) {
            e.printStackTrace();
            log.error("Not written to file, {}", e);
        } catch (JSONException e) {
            e.printStackTrace();
            log.error("Not written to file, {}", e);
        }

    }

    /**
     * writing the event id to the eventsid file once it is exported
     *
     * @param id the event id
     * @throws IOException
     */
    public void markEventAsExported(String id, String name) throws IOException {
        String EventFile=name+".txt";
        try (FileWriter fw = new FileWriter(Settings.EVENT_IDS_FILE_PATH+File.separator + EventFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(id);
            queue.add(id);
        } catch (Exception e) {
            log.error("{} ",e);
        }

    }


    public static JSONObject populateJson(JSONObject event, JSONObject newEvent) {
        try {
            newEvent.put("eventId", (event.getString("eventID").equals(null)) ? "" : event.getString("eventID"));
            newEvent.put("projectId", (event.getString("projectID").equals(null)) ? "" : event.getString("projectID"));
            newEvent.put("dateCreated", (event.getString("dateCreated").equals(null)) ? "" : event.getString("dateCreated"));
            JSONObject user;
            if (!event.get("user").equals(JSONObject.NULL)) {
                user = (JSONObject) event.get("user");
                newEvent.put("userID", (user.get("id").equals(null)) ? "" : user.get("id"));
            } else {
                newEvent.put("userID", "");
            }
            newEvent.put("message", (event.getString("message").equals(null)) ? "" : event.getString("message"));
            newEvent.put("id", (event.get("id").equals(null)) ? "" : event.get("id"));
            newEvent.put("culprit", (event.getString("culprit").equals(null)) ? "" : event.getString("culprit"));
            newEvent.put("title", (event.getString("title").equals(null)) ? "" : event.getString("title"));
            newEvent.put("platform", (event.getString("platform").equals(null)) ? "" : event.getString("platform"));
            newEvent.put("location", (event.get("location").equals(null)) ? "" : event.get("location"));
            newEvent.put("crashFile", (event.get("crashFile").equals(null)) ? "" : event.get("crashFile"));
            newEvent.put("event.type", (event.getString("event.type").equals(null)) ? "" : event.getString("event.type"));
            newEvent.put("groupID", (event.getString("groupID").equals(null)) ? "" : event.getString("groupID"));

        } catch (JSONException e) {
            log.error("Unable to parse event, {}",e);
        }

        return newEvent;
    }

}
