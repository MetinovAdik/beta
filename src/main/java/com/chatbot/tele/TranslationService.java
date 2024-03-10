package com.chatbot.tele;
import okhttp3.*;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TranslationService {

    private String apiKey; // Your OpenAI API key
    private OkHttpClient client; // Reuse your OkHttpClient instance
    @Autowired
    public TranslationService(String apiKey, OkHttpClient client) {
        this.apiKey = apiKey;
        this.client = client;
    }

    public String translateTextToKyrgyz(String text) throws IOException {
        // Create the body of the request with org.json
        JSONObject body = new JSONObject();
        body.put("model", "gpt-4-0125-preview");
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", "Translate the following English text to Kyrgyz."));
        messages.put(new JSONObject().put("role", "user").put("content", text));
        body.put("messages", messages);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Parse the response using org.json
            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            // Extracting the translated text, adjust this part according to the actual JSON structure of the response
            String translatedText = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            return translatedText;
        }
    }
}
