package com.chatbot.tele;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class    TelegramService {
    private final TelegramBot bot;


    public TelegramService(@Value("${telegram.bot.token}") String botToken) {
        this.bot = new TelegramBot(botToken);
    }
    public TelegramBot getBot() {
        return bot;
    }

    public void sendMessage(long chatId, String message) {
        bot.execute(new SendMessage(chatId, message));
    }

    public File getFile(String fileId) {
        GetFileResponse fileResponse = bot.execute(new GetFile(fileId));
        return fileResponse.file();
    }

    public String getFullFilePath(String filePath) {
        return "https://api.telegram.org/file/bot" + bot.getToken() + "/" + filePath;
    }
}
