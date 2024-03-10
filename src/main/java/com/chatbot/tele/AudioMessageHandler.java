package com.chatbot.tele;

import com.chatbot.tele.resultbox.TranscriptionResult;
import com.chatbot.tele.resultbox.TranscriptionSegment;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.request.InputFile;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
@Service
public class AudioMessageHandler {
    private final OkHttpClient httpClient;
    private final SpeechRecognitionService speechRecognitionService;
    private final TelegramService telegramService;
    private final TranslationService translationService;
    private final TextToSpeechService textToSpeechService; // Reference to TextToSpeechService
    private final VideoToAudioService videoToAudioService;
    private final OkHttpClient client;
    private static final Logger logger = LoggerFactory.getLogger(AudioMessageHandler.class);
    @Autowired
    public AudioMessageHandler(OkHttpClient httpClient, SpeechRecognitionService speechRecognitionService,
                               TelegramService telegramService, TranslationService translationService,
                               TextToSpeechService textToSpeechService, VideoToAudioService videoToAudioService, OkHttpClient client) { // Include TextToSpeechService in constructor
        this.httpClient = httpClient;
        this.speechRecognitionService = speechRecognitionService;
        this.telegramService = telegramService;
        this.translationService = translationService;
        this.textToSpeechService = textToSpeechService; // Initialize TextToSpeechService
        this.videoToAudioService = videoToAudioService;
        this.client = client;
    }
    private Path downloadAudio(String audioUrl) throws IOException {
        Request request = new Request.Builder().url(audioUrl).build();
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Failed to download audio from " + audioUrl);
        }

        Path tempAudioFile = Files.createTempFile("downloaded-audio", ".mp3");
        Files.copy(response.body().byteStream(), tempAudioFile, StandardCopyOption.REPLACE_EXISTING);

        response.close();
        return tempAudioFile;
    }
    public Path downloadVideo(String videoUrl) throws IOException {
        Request request = new Request.Builder()
                .url(videoUrl)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Failed to download video: " + response);

            // Creating a temporary file to store the video. Adjust the prefix and suffix as necessary.
            Path tempVideoFile = Files.createTempFile("video-", ".mp4");

            // Writing the response body to the temporary file
            Files.copy(response.body().byteStream(), tempVideoFile, StandardCopyOption.REPLACE_EXISTING);

            return tempVideoFile;
        }
    }
    public void handleAudio(String fileId, long chatId) {
        logger.info("Handling audio for chat ID: {}, file ID: {}", chatId, fileId);
        try {
            File file = telegramService.getFile(fileId);
            String filePath = file.filePath();
            String audioUrl = telegramService.getFullFilePath(filePath);
            Path audioPath = downloadAudio(audioUrl);
            // Распознавание речи с новым возвращаемым типом
            TranscriptionResult transcriptionResult = speechRecognitionService.recognizeSpeechFromAudio(audioUrl);

            if (transcriptionResult.getSegments().isEmpty()) {
                telegramService.sendMessage(chatId, "Не удалось распознать текст.");
                return;
            }

            for (TranscriptionSegment segment : transcriptionResult.getSegments()) {
                String translatedText = translationService.translateTextToKyrgyz(segment.getText());
                Response ttsResponse = textToSpeechService.sendTextForSpeech(translatedText, 1); // Пример ID диктора

                if (!ttsResponse.isSuccessful() || ttsResponse.body() == null) {
                    logger.error("Ошибка при преобразовании текста в речь для сегмента.");
                    continue; // Пропускаем текущий сегмент в случае ошибки
                }

                // Создание временного файла для каждого аудиофрагмента TTS
                Path tempFile = Files.createTempFile("tts-", ".mp3");
                Files.copy(ttsResponse.body().byteStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                // Сохранение информации о сгенерированном аудиофайле в объекте сегмента
                segment.setAudioFilePath(tempFile.toString());
                logger.info("TTS audio segment saved to temp file: {}", tempFile.toString());
                // Здесь можно добавить логику для расчета длительности и сохранения ее в segment
            }

            // Здесь можно добавить логику для сборки финального аудио из сегментов с учетом их временных меток
            AudioMerger audioMerger = new AudioMerger();
            Path finalAudioPath = audioMerger.processAudioWithTranslations(String.valueOf(audioPath), transcriptionResult);
            InputFile inputFile = new InputFile(new java.io.File(finalAudioPath.toString()),"tts_audio.mp3","audio/mp3");
            telegramService.getBot().execute(new com.pengrad.telegrambot.request.SendAudio(chatId, inputFile.getFile()));
            logger.info("Final audio file sent to chat ID: {}", chatId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error handling audio for chat ID: " + chatId, e);
            telegramService.sendMessage(chatId, "Произошла ошибка при обработке аудио.");

        }
    }
    public void handleVideo(String fileId, long chatId) {
        logger.info("Handling video for chat ID: {}, file ID: {}", chatId, fileId);
        try {
            File file = telegramService.getFile(fileId);
            String filePath = file.filePath();
            String videoUrl = telegramService.getFullFilePath(filePath);
            Path videoPath = downloadVideo(videoUrl);
            Path audioPath = videoToAudioService.extractAudioFromVideo(videoUrl);

            if (!Files.exists(audioPath)) {
                logger.error("Audio file does not exist after extraction: {}", audioPath);
                telegramService.sendMessage(chatId, "Произошла ошибка при обработке видео: аудиофайл не был создан.");
                return; // Выход из метода, если аудиофайл не существует
            } else {
                logger.info("Audio file exists and ready for further processing: {}", audioPath);
            }
            // Распознавание речи с новым возвращаемым типом
            TranscriptionResult transcriptionResult = speechRecognitionService.recognizeSpeechFromAudioLocal(audioPath);
            if (!Files.exists(audioPath)) {
                logger.error("Audio file does not exist after TranscriptionResult transcriptionResult = speechRecognitionService.recognizeSpeechFromAudioLocal(audioPath);: {}", audioPath);
                telegramService.sendMessage(chatId, "Произошла ошибка при обработке видео: аудиофайл не был создан.");
                return; // Выход из метода, если аудиофайл не существует
            } else {
                logger.info("Audio file exists and ready for further processing2: {}", audioPath);
            }
            if (transcriptionResult.getSegments().isEmpty()) {
                telegramService.sendMessage(chatId, "Не удалось распознать текст.");
                return;
            }

            for (TranscriptionSegment segment : transcriptionResult.getSegments()) {
                String translatedText = translationService.translateTextToKyrgyz(segment.getText());
                Response ttsResponse = textToSpeechService.sendTextForSpeech(translatedText, 1); // Пример ID диктора

                if (!ttsResponse.isSuccessful() || ttsResponse.body() == null) {
                    logger.error("Ошибка при преобразовании текста в речь для сегмента.");
                    continue; // Пропускаем текущий сегмент в случае ошибки
                }

                // Создание временного файла для каждого аудиофрагмента TTS
                Path tempFile = Files.createTempFile("tts-", ".mp3");
                Files.copy(ttsResponse.body().byteStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                // Сохранение информации о сгенерированном аудиофайле в объекте сегмента
                segment.setAudioFilePath(tempFile.toString());
                logger.info("TTS audio segment saved to temp file: {}", tempFile.toString());
                // Здесь можно добавить логику для расчета длительности и сохранения ее в segment
            }
            AudioMerger audioMerger = new AudioMerger();
            Path finalAudioPath = audioMerger.processAudioWithTranslations(String.valueOf(audioPath), transcriptionResult);
            VideoProcessingService videoProcessingService = new VideoProcessingService();
            Path finalVideoPath = videoProcessingService.createTranslatedVideo(videoPath, finalAudioPath);
            InputFile inputFile = new InputFile(new java.io.File(finalVideoPath.toString()),"tts_video.mp4","video/mp4");
            telegramService.getBot().execute(new com.pengrad.telegrambot.request.SendVideo(chatId, inputFile.getFile()));
            logger.info("Final video file sent to chat ID: {}", chatId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error handling video for chat ID: " + chatId, e);
            telegramService.sendMessage(chatId, "Произошла ошибка при обработке видео.");

        }
    }
}
