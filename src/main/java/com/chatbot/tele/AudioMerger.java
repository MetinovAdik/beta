package com.chatbot.tele;

import com.chatbot.tele.resultbox.TranscriptionResult;
import com.chatbot.tele.resultbox.TranscriptionSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AudioMerger {

    private static final Logger logger = LoggerFactory.getLogger(AudioMerger.class);

    public Path mergeAudioWithSegments(String sourceFilePath, TranscriptionResult transcriptionResult) throws IOException, InterruptedException {
        Path outputPath = Paths.get(sourceFilePath).getParent().resolve("output_merged.mp3");
        StringBuilder ffmpegCmd = new StringBuilder("ffmpeg -y");

        // Добавляем входной файл
        ffmpegCmd.append(" -i ").append(sourceFilePath);

        StringBuilder filterComplex = new StringBuilder(" -filter_complex \"");

        // Переменная для хранения времени начала следующего сегмента
        String nextSegmentStartTime = "0";

        int index = 0; // Инициализируем счетчик индекса
        for (TranscriptionSegment segment : transcriptionResult.getSegments()) {
            // Используем индекс в качестве части идентификатора
            filterComplex.append(String.format("[0]atrim=0:%s[pre%d]; ", segment.getStartTime(), index));
            filterComplex.append(String.format("[0]atrim=%s[post%d]; ", segment.getEndTime(), index));
            // Вставляем переведенный сегмент
            filterComplex.append(String.format("[1]ainsert=atrim=0:%s:apad=1[insert%d]; ", segment.getDuration(), index));
            index++; // Увеличиваем счетчик индекса
        }

        // Соединяем все части
        filterComplex.append("[pre][insert][post]concat=n=3:v=0:a=1[out]\" -map \"[out]\"");

        // Финализируем команду
        ffmpegCmd.append(filterComplex).append(" ").append(outputPath.toString());

        logger.info("Executing FFmpeg command: {}", ffmpegCmd);

        // Выполнение команды FFmpeg
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", ffmpegCmd.toString());
        Process process = processBuilder.start();

        // Чтение вывода ошибок из процесса и логирование
        logProcessOutput(process);

        int exitValue = process.waitFor();
        if (exitValue != 0) {
            logger.error("FFmpeg execution error, exit code: {}", exitValue);
            throw new RuntimeException("FFmpeg execution error: ");
        }

        logger.info("FFmpeg command executed successfully, output file: {}", outputPath);
        return outputPath;
    }

    private void logProcessOutput(Process process) throws IOException {
        try (InputStream stderr = process.getErrorStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stderr))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.error("FFmpeg error output: {}", line);
            }
        }

        try (InputStream stdout = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stdout))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("FFmpeg output: {}", line);
            }
        }
    }
}
