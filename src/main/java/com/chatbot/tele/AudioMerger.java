package com.chatbot.tele;

import com.chatbot.tele.resultbox.TranscriptionResult;
import com.chatbot.tele.resultbox.TranscriptionSegment;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AudioMerger {

    public Path mergeAudioWithSegments(String sourceFilePath, TranscriptionResult transcriptionResult) throws IOException, InterruptedException {
        Path outputPath = Paths.get(sourceFilePath).getParent().resolve("output_merged.mp3");

        // Строим FFmpeg команду для слияния
        StringBuilder ffmpegCmd = new StringBuilder("ffmpeg -y");

        // Добавляем входной файл
        ffmpegCmd.append(" -i ").append(sourceFilePath);

        // Фильтр комплексной обработки
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

        // Выполнение команды FFmpeg
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", ffmpegCmd.toString());
        Process process = processBuilder.start();

        int exitValue = process.waitFor();
        if (exitValue != 0) {
            throw new RuntimeException("FFmpeg execution error");
        }

        return outputPath;
    }
}