package com.chatbot.tele;

import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TextToSpeechService {

    private final OkHttpClient client;
    private final String ttsApiUrl = "http://tts.ulut.kg/api/tts";
    private final String bearerToken;

    @Autowired
    public TextToSpeechService(OkHttpClient client, @Value("${tts.api.bearer.token}") String bearerToken) {
        this.client = client;
        this.bearerToken = bearerToken;
    }

    public Response sendTextForSpeech(String text, int speakerId) throws IOException {
        JSONObject body = new JSONObject();
        body.put("text", text);
        body.put("speaker_id", speakerId);

        Request request = new Request.Builder()
                .url(ttsApiUrl)
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + this.bearerToken)
                .build();

        return client.newCall(request).execute();
    }
}
