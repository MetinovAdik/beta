package com.chatbot.tele.resultbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TranscriptionResult {
    private final List<TranscriptionSegment> segments;

    public TranscriptionResult() {
        this.segments = new ArrayList<>();
    }

    public void addSegment(TranscriptionSegment segment) {
        segments.add(segment);
    }

    public List<TranscriptionSegment> getSegments() {
        return segments;
    }

    // Метод для расчета общей длительности всех сегментов
    public Double getTotalDuration() {
        return segments.stream()
                .map(TranscriptionSegment::getDuration)
                .filter(Objects::nonNull)
                .reduce(0.0, Double::sum);
    }
}