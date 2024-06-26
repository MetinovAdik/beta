package com.chatbot.tele;

import com.chatbot.tele.resultbox.TranscriptionResult;
import com.chatbot.tele.resultbox.TranscriptionSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AudioMerger {

    private static final Logger logger = LoggerFactory.getLogger(AudioMerger.class);
    private double timeStringToSeconds(String timeString) {
        // Дополнительно проверяем, содержит ли строка часы
        if (!timeString.contains(":")) {
            return Double.parseDouble(timeString); // Предполагаем, что строка уже в секундах
        }

        String[] parts = timeString.split(":");
        double hours = 0;
        double minutes = 0;
        double seconds = 0;

        // Корректируем для строки времени, которая может быть в формате mm:ss.SSS или hh:mm:ss.SSS
        if (parts.length == 3) {
            hours = Double.parseDouble(parts[0]);
            minutes = Double.parseDouble(parts[1]);
            seconds = Double.parseDouble(parts[2]);
        } else if (parts.length == 2) {
            minutes = Double.parseDouble(parts[0]);
            seconds = Double.parseDouble(parts[1]);
        } else if (parts.length == 1) {
            seconds = Double.parseDouble(parts[0]);
        } else {
            throw new IllegalArgumentException("Invalid time format: " + timeString);
        }

        return hours * 3600 + minutes * 60 + seconds;
    }
    public Path processAudioWithTranslations(String sourceFilePath, TranscriptionResult transcriptionResult) throws IOException, InterruptedException {
        logger.info("Starting processAudioWithTranslations with sourceFilePath: {}", sourceFilePath);

        Path tempDir = Files.createTempDirectory("audio_processing");
        logger.info("Temporary directory created at: {}", tempDir);

        Path sourceFile = Paths.get(sourceFilePath);
        if (!Files.exists(sourceFile)) {
            logger.error("Source file does not exist: {}", sourceFile);
            throw new IOException("Source file does not exist: " + sourceFile);
        }

        List<Path> allSegmentsPaths = new ArrayList<>();
        double lastSegmentEndTime = 0.0;

        for (TranscriptionSegment segment : transcriptionResult.getSegments()) {
            double startSeconds = timeStringToSeconds(segment.getStartTime());
            double endSeconds = timeStringToSeconds(segment.getEndTime());
            if (startSeconds > lastSegmentEndTime) {
                Path originalSegmentPath = createSegment(sourceFile, lastSegmentEndTime, startSeconds, tempDir);
                allSegmentsPaths.add(originalSegmentPath);
            }

            Path segmentAudioPath = Paths.get(segment.getAudioFilePath());
            if (!Files.exists(segmentAudioPath)) {
                logger.error("Segment audio file does not exist: {}", segmentAudioPath);
                continue; // You might want to throw an exception or handle this case differently
            }

            allSegmentsPaths.add(segmentAudioPath);
            lastSegmentEndTime = endSeconds;
        }

        Path finalOutput = mergeSegments(allSegmentsPaths, tempDir);
        logger.info("Final audio file created at: {}", finalOutput);
        if (!Files.exists(finalOutput)) {
            logger.error("Final audio file does not exist after creation: {}", finalOutput);
        }

        return finalOutput;
    }

    private Path createSegment(Path sourceFile, double startSeconds, double endSeconds, Path tempDir) throws IOException, InterruptedException {
        String segmentFileName = "segment_" + startSeconds + "_" + endSeconds + ".mp3";
        Path segmentPath = tempDir.resolve(segmentFileName);

        String command = String.format("ffmpeg -y -i %s -ss %s -to %s -acodec copy %s",
                sourceFile, startSeconds, endSeconds, segmentPath);

        executeCommand(command);
        return segmentPath;
    }

    private Path mergeSegments(List<Path> segmentsPaths, Path tempDir) throws IOException, InterruptedException {
        Path outputFile = tempDir.resolve("final_output.mp3");

        StringBuilder fileListContent = new StringBuilder();
        for (Path segmentPath : segmentsPaths) {
            fileListContent.append("file '").append(segmentPath).append("'\n");
        }

        Path fileListPath = tempDir.resolve("filelist.txt");
        Files.write(fileListPath, fileListContent.toString().getBytes());

        String command = String.format("ffmpeg -y -f concat -safe 0 -i %s -c copy %s", fileListPath, outputFile);

        executeCommand(command);
        return outputFile;
    }

    private void executeCommand(String command) throws IOException, InterruptedException {
        logger.info("Executing command: {}", command);
        Process process = new ProcessBuilder("/bin/sh", "-c", command).start();

        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
                logger.error(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Command execution failed with exit code {}. Error output: {}", exitCode, errorOutput);
            throw new RuntimeException("Command execution failed with exit code " + exitCode);
        }
    }
}