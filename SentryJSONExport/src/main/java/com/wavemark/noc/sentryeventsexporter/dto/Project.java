package com.wavemark.noc.sentryeventsexporter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wavemark.noc.sentryeventsexporter.EventsExporter;
import com.wavemark.noc.sentryeventsexporter.EventFileFunctions;
import com.wavemark.noc.sentryeventsexporter.Util;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Project implements Runnable {

    @JsonProperty("name")
    private String name;

    @JsonProperty("environments")
    private String environments;

    @JsonProperty("tag_columns")
    private List<String> tagColumns;

    @JsonProperty("matchers")
    private List<Match> matchers;

    private List<String> environmentsList;

    public JSONArray event_Array = new JSONArray();

    public boolean noMoreEvents = false;
    public boolean exceededLimit = false;

    private static final Logger log = LoggerFactory.getLogger(Project.class.getName());

    private int totalEvents = 0;

    private final Map<String, String> tagmap = new HashMap<>();
    private int startBatchAt = 0;
    private final Util util=new Util();
    private final EventFileFunctions eventfiles= new EventFileFunctions();
    public long apiCallsTotalTime = util.totalTime;




    public String getName() {
        return name;
    }
    //for yml
    public List<String> getTagColumns() {
        return tagColumns;
    }
    public String getEnvironments() {
        return environments;
    }
    public List<Match> getMatchers() {
        return matchers;
    }

    private final Date date = EventsExporter.getDate();
    private final SimpleDateFormat output = new SimpleDateFormat("YYYY_MM_DD_HH_mm_SS");
    private final String dateExport_String = output.format(date);
    private final String jsonFileExtension="_" + dateExport_String + ".json";

    private int eventsSeen = 0;

    private String getJsonFileName()
    { return name+"_" + dateExport_String + ".json"; }
    private String getEventIdsFileName()
    { return name+".txt";}


    /**
     * this initializes a hashmap from the tags specified in the yml file
     * it Sets the keys as the tag columns in the yml and initializes their values to null
     * the value of a key (tag) that we later on retrieve, will be changed to its actual value
     */
    public void getHashMapFromList() {
        if (tagColumns != null) {
            for (String extraTag : tagColumns)
                if (extraTag != null && !extraTag.equals("") && !extraTag.equals(null)) {
                    tagmap.put(extraTag, null);
                }
        }
        if (environments != "" && environments != null) {
            environmentsList = Arrays.asList(environments.split(","));
        } else {
            environmentsList = Collections.singletonList("");
        }
    }

    @Override
    public void run() {
        getHashMapFromList();
        try {
            if (EventsExporter.getLookback() >= 3000) {
                startBack();
            } else {
                goBackThenForward();
            }
            apiCallsTotalTime = util.totalTime;
            EventFileFunctions.moveJsonFile(getJsonFileName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * retrieves batches of 100 events, starts api from lookback-100
     * instead of scrolling api backwards then forwards, starts backwards
     *
     * @throws Exception
     */
    public void startBack() throws Exception {
        log.debug("start backwards");
        //start api from back
        startBatchAt = EventsExporter.getLookback() - 100;
        //scroll api forward until start_batch_at is 0 so we traversed all lookbackevents
        try {
            while (startBatchAt > -100) {
                if (startBatchAt < 0) {
                    startBatchAt = 0;
                }
                event_Array = util.getBatch(startBatchAt, name);
                if (event_Array.length() != 0) {
                    traverseForward(event_Array.length() - 1, event_Array);
                }
                startBatchAt -= 100;
            }
        }catch (Exception e)
        {
            log.error("No project {} found",name);
        }
    }

    /**
     * scrolls api backwards then forwards
     * stops when no more events or when we exceeded 'lookback' events specified in env.conf
     *
     * @throws Exception
     */
    public void goBackThenForward() throws Exception {

        try {
            while (!noMoreEvents && !exceededLimit) {
                log.debug("Going back");
                event_Array = util.getBatch(startBatchAt, name);
                log.debug("array length: " + event_Array.length());
                totalEvents = totalEvents + event_Array.length();
                if (totalEvents >= EventsExporter.getLookback()) {
                    //we reached the specified number of events (lookback) that we want to search
                    exceededLimit = true;
                    break;
                }
                if (event_Array.length() == 0) {
                    //we have no more events in this project
                    noMoreEvents = true;
                    break;
                }
                startBatchAt += 100;
            }
        } catch (Exception e) {
            log.error("No such project {}",name);
        }
        log.debug("No more events?" + noMoreEvents);
        log.debug("exceeeded limit?" + exceededLimit);
        //if exceeded lookback events, traverse the json array for the last allowed event then start traversing the array and api  forward
        if (exceededLimit) {
            log.debug("totalevents:" + totalEvents);
            totalEvents = totalEvents - event_Array.length();
            int indexlast = searchLastAllowedIndex(event_Array);
            traverseForward(indexlast, event_Array);
            if (startBatchAt > 0) {
                while (startBatchAt > 0) {
                    startBatchAt -= 100;
                    if (startBatchAt < 0) {
                        startBatchAt = 0;
                    }
                    event_Array = util.getBatch(startBatchAt, name);
                    //traverse events forward starting from last allowed
                    traverseForward(event_Array.length() - 1, event_Array);
                }
            }
        }

        if (noMoreEvents) {
            //traverse events forward starting from oldest event (last in array)
            traverseForward(event_Array.length() - 1, event_Array);
            if (startBatchAt > 0) {
                while (startBatchAt > 0) {
                    startBatchAt -= 100;
                    event_Array = util.getBatch(startBatchAt, name);
                    traverseForward(event_Array.length() - 1, event_Array);
                }
            }
        }

    }

    /**
     * returns a comma separated string of all the tags of an event to insert in output
     *
     * @param json the event in json format
     * @return the comma separated string of all the tags of an event
     */
    private String getTagsString(JSONObject event) {
        JSONArray tags = (JSONArray) event.get("tags");
        Iterator<Object> iterator = tags.iterator();
        StringBuilder Tag_String = new StringBuilder();
        while (iterator.hasNext()) {
            JSONObject tagsjsonObject = (JSONObject) iterator.next();
            log.debug("keys:" + tagsjsonObject.getString("key"));
            String actualtag = tagsjsonObject.getString("key");
            String actualValue = tagsjsonObject.getString("value");
            Tag_String.append(actualtag).append("=").append(actualValue).append(",");
            boolean matches = false;
            for (String tagtomatch : tagColumns) {
                if (actualtag.equals(tagtomatch)) {
                    tagmap.replace(tagtomatch, actualValue);
                }
            }

        }
        Tag_String = new StringBuilder(Tag_String.substring(0, Tag_String.length() - 1));
        return Tag_String.toString();
    }



    /**
     * when we find a matching event, this function is called to export it to json
     * it checks first if the event is already exported, if it is it does nothing
     * if not, it creates a json object, populates it with the relevant fields, and exports it to out dir
     *
     * @param event the event that needs to be exported
     * @throws IOException
     */
    private void buildJsonObject(JSONObject event) throws IOException {

        boolean alreadyexported =eventfiles.checkIfExported(event.getString("eventID"),getEventIdsFileName());
        if (!alreadyexported) {
            JSONObject newEvent = new JSONObject();
            try {
                newEvent.put("projectSlug", name);
                newEvent= EventFileFunctions.populateJson(event,newEvent);
                String Tag_String = getTagsString(event);
                newEvent.put("tags", Tag_String);
                newEvent=addExtraTags(newEvent);
            } catch (Exception e) {
                log.debug("an event may not printed");
                log.error("couldnt insert tags {}", e);
            }
            Iterator<String> keys = newEvent.keys();
            int countkeys=0;
            while(keys.hasNext()) {
                String key = keys.next();
                countkeys+=1;
            }
            log.debug("{} keys for project {}",countkeys,name);

            eventfiles.appendEventToJson(newEvent, getJsonFileName(), name);
        }
        else {
            log.debug("Already exported {}",event.getString("eventID"));
        }

    }

    public JSONObject addExtraTags(JSONObject newEvent)
    {
        if (tagmap != null) {
            Iterator it = tagmap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();

                if (!(pair.getKey().equals(null)) && !(pair.getValue()==(null))) {
                    log.debug(pair.getKey() + " = " + pair.getValue());
                    newEvent.put((String) pair.getKey(), pair.getValue());
                }
                else if (pair.getValue()==(null))
                {
                    newEvent.put((String) pair.getKey(),"");
                }

            }
        }
        return newEvent;
    }

    /**
     * checks if event environment matches the environments specified in the yml file
     *
     * @param json the event
     * @return boolean if it matches
     */
    private boolean checkEnvMatch(JSONObject event) {
        log.debug("Size" + environmentsList.size());
        if (environmentsList.get(0) == "") {
            log.debug("0 size");
            return true; }
        JSONArray tags = (JSONArray) event.get("tags");
        Iterator<Object> iterator = tags.iterator();
        String Tag_String = "";
        while (iterator.hasNext()) {
            JSONObject tagJsonObject = (JSONObject) iterator.next();
            log.debug("keys:" + tagJsonObject.getString("key"));
            String actualtag = tagJsonObject.getString("key");
            String actualValue = tagJsonObject.getString("value");
            log.debug(actualtag);
            if (actualtag.equals("environment")) {
                for (String envtomatch : environmentsList) {
                    log.debug("ENVS: " + envtomatch + " " + actualValue);

                    if (envtomatch.toLowerCase().equals(actualValue.toLowerCase())) {
                        log.debug("ENVS " + envtomatch + " " + actualValue);
                        return true;
                    }
                }
                log.debug("env does not match " + actualValue);
                return false;
            }
        }
        return false;
    }

    /**
     * checks if the event title matches the titles specified in yml file through regex matching
     *
     * @param json the event
     * @return boolean if it matches
     */
    private boolean checkTitleMatch(JSONObject event) {
        boolean matches = false;
        String eventTitle = event.getString("title");
        if (matchers != null) {
            for (Match issue : matchers) {
                String regexFromYml = issue.getTitle_regex();
                String regex="";
                if (regexFromYml==null){
                    regex="";
                }
                else{
                    regex=regexFromYml.toLowerCase();
                }
                /*
                else if (regexFromYml.startsWith(".*")|| regexFromYml.endsWith(".*"))
                {
                    regex= regexFromYml.toLowerCase();
                }
                else{
                    regex = ".*" + regexFromYml.toLowerCase() + ".*";
                }

                 */
                // matches = Pattern.matches(regex, eventTitle.toLowerCase());
                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(eventTitle.toLowerCase());
                boolean matchFound = matcher.find();
                if(matchFound) {
                    //  System.out.println("Match found");
                    log.debug("This title matches" + event.get("title") + regex);
                    return true;
                } else {
                    ;
                    //   System.out.println("Match not found");
                }
                log.debug("Matches??? " + regex + " " + eventTitle + matches);
            }
        }
        log.debug("Title does not match " + event.get("title"));
        return false;
    }





    /**
     * this function traverses forward the array of 100 events (returned by the api)
     * and calls functions to check which events match yml requirements and calls a function to export them individually
     *
     * @param i         the index on which we start traversing the array forward
     * @param jsonArray the array containing the batch of 100 events
     * @throws IOException
     */
    private void traverseForward(int startindex, JSONArray eventArray) throws IOException {


        log.debug("Found the event at" + startindex);
        //traverse array forward
        for (; startindex >= 0; startindex--) {
            JSONObject event = eventArray.getJSONObject(startindex);
            log.debug("start index: " + startindex);
            boolean matches = checkTitleMatch(event);
            //this function checks if title regex matches the titles specified in yml file
            boolean matchesEvent = checkEnvMatch(event);
            //this function checks if environments of that event match any of those in yml
            log.debug("{}", matches);
            log.debug("Matchesssss");
            if (matches && matchesEvent) {
                log.debug("should be exported");
                log.debug("seen " + eventsSeen + " events");
                eventsSeen += 1;
                buildJsonObject(event);
            } else {
                log.debug("Did not match");
            }

        }
    }

    /**
     * this function is called when the project has more events than the lookback events
     * once we reach the batch of 100 events that will make the total events searched > lookback events this function is called
     * it searches for the index where lookback events = total events searched, essentially the index of the last (oldes) allowed event to be exported
     * then that index will be used to traverse forward and export the events that preceed it and match the yml configuration
     *
     * @param jsonArray
     * @return the index of the last allowed event to be exported this run
     * @throws ParseException
     */
    private int searchLastAllowedIndex(JSONArray eventArray) {
        log.debug("{}", eventArray.length());
        for (int i = 0; i < eventArray.length(); i++) {
            totalEvents += 1;
            if (totalEvents >= EventsExporter.getLookback()) {
                return i;
            }
        }
        return -1;
    }



}




