package bert;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

public class python_BERT_API {

    // If defined, log every interaction with the BERT API
    private static String activeDebugDirectory = null;
    private static final String subdirectory = "BERT_CALLS";

    // Counter to ensure unique log filenames
    private static final AtomicInteger logCounter = new AtomicInteger(1);

    // Open connection to the Python FastAPI server
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    // Hard-coded URL for the local Python BERT instance
    private static final String URL = "http://127.0.0.1:41818/predict";

    // Jason logger for the AgentSpeak console
    private static Logger jasonLogger;

    // Ping the BERT API. If offline, start the Python server automatically.
    public static String ping(String statsDirectory) {
        jasonLogger = Logger.getLogger("bert.python_BERT_API");

        // Create the BERT_CALLS subdirectory if a valid stats directory is passed
        if (statsDirectory != null && !statsDirectory.trim().isEmpty() && !statsDirectory.equals("none")) {
            try {
                Path dirPath = Paths.get(statsDirectory, subdirectory);
                Files.createDirectories(dirPath);
                activeDebugDirectory = dirPath.toString();
                logCounter.set(1); 
                jasonLogger.info("BERT logs will be saved to: " + activeDebugDirectory);
            } catch (IOException e) {
                jasonLogger.warning("Failed to create BERT_CALLS directory: " + e.getMessage());
            }
        } else {
            activeDebugDirectory = null; 
        }
        
        // 1. Attempt initial connection
        if (attemptPing()) {
            jasonLogger.info("Successfully pinged and warmed up BERT API (Server was already running).");
            return "ok";
        }

        // 2. If connection fails, try to start the server
        jasonLogger.info("BERT API not responding. Attempting to boot Python FastAPI server...");
        if (startPythonServer()) {
            return "ok";
        } else {
            return "Failed to start or connect to BERT API.";
        }
    }

    // Helper method to send a silent warmup request
    private static boolean attemptPing() {
        try {
            JSONObject json = new JSONObject();
            json.put("message", "warmup_ping_sequence"); 

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
                    
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false; // Connection refused, timeout, etc.
        }
    }

    // Helper method to spawn the Python process and poll until it's ready
    private static boolean startPythonServer() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "inference_API.py");
            
            // Ensure this folder exists relative to where you launch Jason!
            pb.directory(new File("BERT_API")); 
            
            pb.redirectErrorStream(true); // Merge stderr into stdout
            Process p = pb.start();
            
            // --- NEW DEBUGGING CODE: Read and print the Python console output ---
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            new Thread(() -> {
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        // Print exactly what Python is doing/crashing with
                        System.out.println("[PYTHON CONSOLE] " + line); 
                    }
                } catch (IOException e) {
                    // Ignore stream closed errors when process dies
                }
            }).start();
            // --------------------------------------------------------------------

            // Polling loop: Wait for PyTorch to load the model into VRAM
            jasonLogger.info("Python process launched. Waiting for PyTorch weights to load into GPU (Timeout: 30s)...");
            
            for (int i = 0; i < 15; i++) {
                Thread.sleep(2000); // Wait 2 seconds between pings
                if (attemptPing()) {
                    jasonLogger.info("Python BERT server started successfully and is ready for inference.");
                    return true;
                }
            }
            
            jasonLogger.severe("Python server failed to respond within 30 seconds. Check [PYTHON CONSOLE] outputs above for the crash reason.");
            p.destroy(); // Kill the hung process
            return false;
            
        } catch (Exception e) {
            jasonLogger.severe("Failed to execute python system command: " + e.getMessage());
            return false;
        }
    }

    // Queries the API, parses the JSON, and returns the Jason literal string
    public static String predict(String agentName, String message) {
        try {
            // Build the request payload
            JSONObject json = new JSONObject();
            json.put("message", message);

            // Make the HTTP request to the Python API
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .timeout(Duration.ofSeconds(15)) 
                    .build();
                    
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check for HTTP errors
            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }

            // Extract variables from JSON
            JSONObject answer = new JSONObject(response.body());
            String performative = answer.getString("performative");
            String target = answer.getString("target");

            // Log if enabled
            if (activeDebugDirectory != null) {
                logInteraction(agentName, message, performative, target);
            }

            jasonLogger.info("[" + agentName + "] , performative=" + performative + ", target=" + target);

            // Format directly into the AgentSpeak literal
            return "performative_result(" + performative + "(" + target + "))"; 
            
        } catch (Exception e) {
            if (jasonLogger != null) {
                jasonLogger.severe("Failed to extract performative/target from BERT: " + e.getMessage());
            }
            // Fallback belief prevents Jason .wait() locks
            return "performative_result(unknown(unknown))"; 
        }
    }

    // Log the interaction to a JSON file
    private static void logInteraction(String agentName, String message, String performative, String target) {
        if (activeDebugDirectory == null) return;

        int currentId = logCounter.getAndIncrement();
        String safeAgentName = (agentName != null && !agentName.isEmpty()) ? agentName.replaceAll("[^a-zA-Z0-9.-]", "_") : "unknown_agent";
                
        String fileName = String.format("bert_call_%s_%03d.json", safeAgentName, currentId);
        Path filePath = Paths.get(activeDebugDirectory, fileName);

        JSONObject logFile = new JSONObject();
        logFile.put("agentName", agentName);
        logFile.put("input_message", message);
        logFile.put("predicted_performative", performative);
        logFile.put("predicted_target", target);

        try (FileWriter fw = new FileWriter(filePath.toFile());
             PrintWriter out = new PrintWriter(fw)) {
            out.print(logFile.toString(4));
        } catch (IOException e) {
            if (jasonLogger != null) {
                jasonLogger.warning("Failed to write to BERT debug file (" + fileName + "): " + e.getMessage());
            }
        }
    }
}