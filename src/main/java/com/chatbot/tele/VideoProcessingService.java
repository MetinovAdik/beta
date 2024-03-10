package com.chatbot.tele;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class VideoProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingService.class);

    public Path createTranslatedVideo(Path originalVideoPath, Path translatedAudioPath) throws IOException, InterruptedException {
        // Prepare the output video file path
        Path outputVideoPath = Files.createTempFile("translated-video-", ".mp4");

        // Construct the FFmpeg command for replacing the original audio with the translated audio
        String command = String.format("ffmpeg -y -i \"%s\" -i \"%s\" -c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0 \"%s\"",
                originalVideoPath.toString(), translatedAudioPath.toString(), outputVideoPath.toString());

        // Execute the FFmpeg command
        executeCommand(command);

        logger.info("Translated video created: {}", outputVideoPath);
        return outputVideoPath;
    }

    private void executeCommand(String command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("/bin/sh", "-c", command).start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
        }

        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                logger.error(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg command execution failed with exit code " + exitCode);
        }
    }
}