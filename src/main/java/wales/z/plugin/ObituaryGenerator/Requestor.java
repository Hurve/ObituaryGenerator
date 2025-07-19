package wales.z.plugin.ObituaryGenerator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletionException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;


public class Requestor {
	public static String requestObit(String prompt, String apiKey) throws Exception {
	    String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

	    JsonObject textPart = new JsonObject();
	    textPart.addProperty("text", prompt);

	    JsonArray parts = new JsonArray();
	    parts.add(textPart);

	    JsonObject content = new JsonObject();
	    content.add("parts", parts);

	    JsonArray contents = new JsonArray();
	    contents.add(content);

	    JsonObject requestBody = new JsonObject();
	    requestBody.add("contents", contents);

	    HttpClient client = HttpClient.newHttpClient();
	    HttpRequest request = HttpRequest.newBuilder()
	            .uri(URI.create(endpoint))
	            .header("Content-Type", "application/json")
	            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
	            .build();

	    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

	    String responseBody = response.body();
	    JsonElement root = JsonParser.parseString(responseBody);
	    JsonObject rootObj = root.getAsJsonObject();

	    // Defensive checks
	    if (!rootObj.has("candidates") || !rootObj.get("candidates").isJsonArray()) {
	        System.out.println("[ObituaryGenerator] No 'candidates' in response: " + responseBody);
	        return "No obituary could be generated (missing candidates).";
	    }

	    JsonArray candidates = rootObj.getAsJsonArray("candidates");

	    if (candidates.size() == 0) {
	        System.out.println("[ObituaryGenerator] Empty 'candidates' array: " + responseBody);
	        return "No obituary could be generated (empty candidates).";
	    }

	    JsonObject firstCandidate = candidates.get(0).getAsJsonObject();

	    JsonObject content2 = firstCandidate.getAsJsonObject("content");
	    JsonArray parts2 = content2.getAsJsonArray("parts");

	    if (parts2.size() == 0 || !parts2.get(0).isJsonObject()) {
	        System.out.println("[ObituaryGenerator] No valid parts in first candidate: " + responseBody);
	        return "No obituary could be generated (no valid content).";
	    }

	    JsonObject firstPart = parts2.get(0).getAsJsonObject();

	    if (!firstPart.has("text")) {
	        System.out.println("[ObituaryGenerator] No 'text' field in first part: " + responseBody);
	        return "No obituary could be generated (missing text).";
	    }

	    return firstPart.get("text").getAsString();
		
	}
	
	public static String sendMessage(String message, String channelID, String apiKey) {
		JsonObject payload = new JsonObject();
		
		payload.addProperty("content", message);
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://discord.com/api/v10/channels/" + channelID + "/messages"))
				.header("Authorization", "Bot " + apiKey)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
				.build();
		
		HttpClient client = HttpClient.newHttpClient();
		
	    try {
	        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
	              .join();
	        return null;
	    } catch (CompletionException e) {
	        return e.getCause().getMessage();
	    }
	    
	}
}
