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
        String[] parts = timeString.split(":");
        double hours = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }
    public Path processAudioWithTranslations(String sourceFilePath, TranscriptionResult transcriptionResult) throws IOException, InterruptedException {
        // Создаем временную директорию для работы
        Path tempDir = Files.createTempDirectory("audio_processing");
        Path sourceFile = Paths.get(sourceFilePath);

        List<Path> allSegmentsPaths = new ArrayList<>();

        // Создаем сегменты из исходного файла на основе временных меток
        double lastSegmentEndTime = 0.0;
        for (TranscriptionSegment segment : transcriptionResult.getSegments()) {
            double startSeconds = timeStringToSeconds(segment.getStartTime());
            double endSeconds = timeStringToSeconds(segment.getEndTime());
            if (startSeconds > lastSegmentEndTime) {
                Path originalSegmentPath = createSegment(sourceFile, lastSegmentEndTime, startSeconds, tempDir);
                allSegmentsPaths.add(originalSegmentPath);
            }
            allSegmentsPaths.add(Paths.get(segment.getAudioFilePath()));
            lastSegmentEndTime = endSeconds;
        }


        // Сливаем все сегменты в один файл
        Path finalOutput = mergeSegments(allSegmentsPaths, tempDir);

        logger.info("Final audio file created at: {}", finalOutput);
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
        Process process = new ProcessBuilder("/bin/sh", "-c", command).start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.error(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command execution failed with exit code " + exitCode);
        }
    }
}