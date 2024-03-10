package com.chatbot.tele.resultbox;

public class TranscriptionSegment {
    private final String startTime;
    private final String endTime;
    private final String text;
    private String audioFilePath; // Путь к аудиофайлу сегмента
    private Double duration; // Длительность аудиофрагмента в секундах

    // Конструктор
    public TranscriptionSegment(String startTime, String endTime, String text) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
    }

    // Геттеры и сеттеры
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getText() { return text; }
    public String getAudioFilePath() { return audioFilePath; }
    public void setAudioFilePath(String audioFilePath) { this.audioFilePath = audioFilePath; }
    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }
}