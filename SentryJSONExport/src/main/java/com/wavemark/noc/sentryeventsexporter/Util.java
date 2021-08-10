package com.wavemark.noc.sentryeventsexporter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;


public class Util {


    static Logger log = LoggerFactory.getLogger(Util.class.getName());

    public long totalTime = 0;
    public long startTime;
    public long endTime;
    public String httpGet(final URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + EventsExporter.getSentryToken());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestMethod("GET");
        StringBuffer response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String output;
            response = new StringBuffer();
            while ((output = in.readLine()) != null) {
                response.append(output.toString());
            }
        }
        return response.toString();
    }

    /**
     * This is the function that calls the api and retrieves 100 events
     *
     * @param start_batch_at the middle element of the cursor
     * @param proj           the project
     * @return json array of 100 events
     * @throws Exception
     */
    public JSONArray getBatch(int start_batch_at, String proj) throws Exception {

        startTime = System.currentTimeMillis();
        final URL url = new URL("https://" + EventsExporter.getSentryHost() + "/api/0/projects/" +
                EventsExporter.getOrgSlug() + "/" + proj + "/events/?cursor=0:" + start_batch_at + ":0");
        final String response = httpGet(url);
        JSONArray jsonArray = new JSONArray(response);

        endTime = System.currentTimeMillis() - startTime;
        totalTime += (int) endTime / 1000;

        return jsonArray;
    }

    /**
     * checks if certain path is a directory, creates one if doesnt exist
     *
     * @param dir the string path to a directory
     */
    public static void createDirectoryIfMissing(final String dir) {
        final File file = new File(dir);
        if (!file.isDirectory()) {
            boolean directoryCreated = file.mkdir();
            if (!directoryCreated) {
                System.out.println("Couldn't create directory " + dir);
                System.exit(1);
            }
            log.info("Created directory " + dir);
        }
    }


}