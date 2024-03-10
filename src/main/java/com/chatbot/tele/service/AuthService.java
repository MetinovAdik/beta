package com.chatbot.tele.service;

import com.chatbot.tele.model.User;
import com.chatbot.tele.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean authenticate(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .map(User::getIsAuthenticated)
                .orElse(false);
    }

    public void registerOrUpdateUser(Long telegramId, boolean isAuthenticated) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElse(new User());
        user.setTelegramId(telegramId);
        user.setIsAuthenticated(isAuthenticated);
        userRepository.save(user);
    }
}
