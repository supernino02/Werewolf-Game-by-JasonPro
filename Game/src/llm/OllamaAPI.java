package llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

public class OllamaAPI {

    //if defined, log every interaction with the ollama api
    private static String activeDebugDirectory = null;
    private static final String subdirectory = "OLLAMA_CALLS";

    //counter to ensure unique log filenames
    private static final AtomicInteger logCounter = new AtomicInteger(1);

    //open connection to the ollama api
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    //hard coded URL for local ollama instance
    private static final String URL = "http://localhost:11434/api/generate";

    //name of the model to use
    private static String activeModel;

    //jason logger fo the agentstpeak console
    private static Logger jasonLogger;

    //ping the ollama api and setup debug directory if requested
    public static String ping(String modelName, String statsDirectory) {
        //initialize the logger and model name
        activeModel = modelName;
        jasonLogger = Logger.getLogger("llm.OllamaAPI");

        //create the ollama_calls subdirectory if a valid stats directory is passed
        if (statsDirectory != null && !statsDirectory.trim().isEmpty() && !statsDirectory.equals("none")) {
            try {
                Path dirPath = Paths.get(statsDirectory, subdirectory);
                Files.createDirectories(dirPath);
                activeDebugDirectory = dirPath.toString();
                //reset counter for the new session
                logCounter.set(1); 
                jasonLogger.info("Ollama logs will be saved to: " + activeDebugDirectory);
            } catch (IOException e) {
                jasonLogger.warning("Failed to create OLLAMA_CALLS directory: " + e.getMessage());
            }
        } else {
            //logging disabled
            activeDebugDirectory = null; 
        }
        
        //test the connection with a simple request to load the model
        try {
            JSONObject json = new JSONObject();
            json.put("model", activeModel); 
            json.put("keep_alive", -1);         //KEEP THE MODEL LOADED IN THE GPU

            //use the http api
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            //check the response
            if (response.statusCode() == 200) {
                jasonLogger.info("Successfully pinged and loaded model: " + activeModel);
                return "ok";
            } else {
                jasonLogger.warning("Model load failed. HTTP " + response.statusCode());
                return "Model load failed. HTTP " + response.statusCode();
            }
        } catch (Exception e) {
            //cannot even connect to the api
            if (jasonLogger != null) jasonLogger.severe("Connection refused. Error: " + e.getMessage());
            return "Connection refused. Error: " + e.getMessage();
        }
    }

    //generate a response from the ollama api
    public static String generate(String agentName, String actionName, String prompt, String uidPhase) throws Exception {
        if (activeModel == null) throw new IllegalStateException("Active model not set. Call ping(modelName) first.");

        //build the request payload
        JSONObject json = new JSONObject();
        json.put("model", activeModel);
        json.put("prompt", prompt);
        json.put("stream", false);
        json.put("keep_alive", -1);
        json.put("think", false);

        //make the http request to the ollama api
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .timeout(Duration.ofMinutes(5))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        //check for errors in the response
        if (response.statusCode() != 200) {
            if (jasonLogger != null) {
                jasonLogger.severe("Ollama API Error HTTP " + response.statusCode() + ": " + response.body());
            }
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        //get the response text from the json response
        JSONObject answer = new JSONObject(response.body());
        String extractedResponse = answer.getString("response").trim();

        //pass payload strings to the logger (if enaabled) for debugging and analysis
        if (activeDebugDirectory != null) {
            logInteraction(agentName, actionName, prompt, extractedResponse, uidPhase);
        }

        //log the response to the jason console
        if (jasonLogger != null) {
            jasonLogger.info(extractedResponse);
        }

        return extractedResponse;
    }

    //log the interaction to a json file in the debug directory defined
    private static void logInteraction(String agentName, String actionName, String prompt, String output, String uidPhase) {
        if (activeDebugDirectory == null) return;

        int currentId = logCounter.getAndIncrement();
        
        String safeAgentName = (agentName != null && !agentName.isEmpty()) 
                ? agentName.replaceAll("[^a-zA-Z0-9.-]", "_") 
                : "narrator";
                
        String safeActionName = (actionName != null && !actionName.isEmpty()) 
                ? actionName.replaceAll("[^a-zA-Z0-9.-]", "_") 
                : "unknown_action";
                
        String safeUidPhase = (uidPhase != null && !uidPhase.isEmpty()) 
                ? uidPhase.replaceAll("[^a-zA-Z0-9.-]", "_") 
                : "unknown_phase";
                
        //format filename securely using parameters
        String fileName = String.format("%s_%s_%s_%03d.json", safeUidPhase, safeAgentName, safeActionName, currentId);
        Path filePath = Paths.get(activeDebugDirectory, fileName);

        JSONObject logFile = new JSONObject();
        logFile.put("uid_phase", uidPhase);
        logFile.put("agentName", agentName);
        logFile.put("Prompt", prompt);
        logFile.put("output", output);

        try (FileWriter fw = new FileWriter(filePath.toFile());
             PrintWriter out = new PrintWriter(fw)) {
            
            out.print(logFile.toString(4));
            
        } catch (IOException e) {
            if (jasonLogger != null) {
                jasonLogger.warning("Failed to write to LLM debug file (" + fileName + "): " + e.getMessage());
            }
        }
    }
}