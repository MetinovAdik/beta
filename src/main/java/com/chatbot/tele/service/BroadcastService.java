package com.chatbot.tele.service;

import com.chatbot.tele.TelegramService;
import com.chatbot.tele.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BroadcastService {

    private final UserRepository userRepository;
    private final TelegramService telegramService;

    @Autowired
    public BroadcastService(UserRepository userRepository, TelegramService telegramService) {
        this.userRepository = userRepository;
        this.telegramService = telegramService;
    }

    public void broadcastMessage(String message) {
        userRepository.findAll().forEach(user -> {
            if (user.getIsAuthenticated()) { // Отправляем сообщения только аутентифицированным пользователям
                try {
                    telegramService.sendMessage(user.getTelegramId(), message);
                } catch (Exception e) {
                    System.err.println("Ошибка при отправке сообщения пользователю с ID: " + user.getTelegramId());
                    e.printStackTrace();
                }
            }
        });
    }
}

