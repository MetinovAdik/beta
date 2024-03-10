package com.chatbot.tele;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class VideoToAudioService {

    private static final Logger logger = LoggerFactory.getLogger(VideoToAudioService.class);
    private final OkHttpClient httpClient;

    @Autowired
    public VideoToAudioService(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Path extractAudioFromVideo(String videoUrl) throws IOException, InterruptedException {
        logger.info("Starting to extract audio from video URL: {}", videoUrl);
        Response response = null;
        Path tempVideoFile = null;
        try {
            Request request = new Request.Builder().url(videoUrl).build();
            response = httpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("Failed to download video: " + response);
            }

            tempVideoFile = Files.createTempFile("video-", ".mp4");
            Files.write(tempVideoFile, response.body().bytes());
            logger.info("Video downloaded and saved to temp file: {}", tempVideoFile);

            Path tempAudioFile = Files.createTempFile("audio-", ".mp3");
            String ffmpegPath = "ffmpeg";
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath,  "-i", tempVideoFile.toString(),"-y", "-vn", "-ar", "44100", "-ac", "2", "-b:a", "192k", tempAudioFile.toString());

            Process process = pb.start();

            // Log standard output and error from the FFmpeg process
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

            stdInput.lines().forEach(line -> logger.info("FFmpeg output: {}", line));
            stdError.lines().forEach(line -> logger.error("FFmpeg error: {}", line));

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Audio extracted and saved to temp file: {}", tempAudioFile);
                return tempAudioFile;
            } else {
                throw new IOException("FFmpeg failed with exit code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error extracting audio from video", e);
            throw e;
        } finally {
            if (tempVideoFile != null) {
                Files.deleteIfExists(tempVideoFile);
                logger.info("Temporary video file deleted: {}", tempVideoFile);
            }
            if (response != null) {
                response.close();
            }
        }
    }
}
