package com.chatbot.tele;

import com.chatbot.tele.service.AuthService;
import com.pengrad.telegrambot.UpdatesListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
@Component
public class BotRunner implements CommandLineRunner {

    private final TelegramService telegramService;
    private final AudioMessageHandler audioMessageHandler;
    private final AuthService authService;
    private static final Logger logger = LoggerFactory.getLogger(BotRunner.class);
    @Autowired
    public BotRunner(TelegramService telegramService, AudioMessageHandler audioMessageHandler, AuthService authService) {
        this.telegramService = telegramService;
        this.audioMessageHandler = audioMessageHandler;
        this.authService = authService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Bot is starting");
        /* String broadcastMessage = "Уважаемые пользователи! Мы хотим поделиться важными новостями о развитии нашего сервиса. Для оптимизации затрат и обеспечения долгосрочной устойчивости нашего проекта мы приняли решение о переходе на локальную версию технологии распознавания речи Whisper.\n" +
                "\n" +
                "Этот шаг позволит нам снизить операционные расходы, что важно для поддержания и развития сервиса. В связи с этими изменениями, наш бот будет временно недоступен во время модернизации. Мы ожидаем, что процесс займет значительное время, и искренне приносим извинения за любые неудобства.\n" +
                "\n" +
                "Ваше терпение и поддержка в этот период крайне важны для нас. Мы стремимся сделать наш сервис более эффективным и доступным, и благодарим вас за понимание и верность нашему проекту.\n" +
                "\n" +
                "Пожалуйста, следите за обновлениями, и мы обязательно уведомим вас о возобновлении работы сервиса. Спасибо за ваше терпение и поддержку!";
        broadcastService.broadcastMessage(broadcastMessage);*/

        telegramService.getBot().setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.message() != null && update.message().chat() != null) {
                    long chatId = update.message().chat().id();
                    Long telegramUserId = update.message().from().id();

                    // Проверка наличия текста в сообщении для избежания NullPointerException
                    String text = update.message().text();
                    if (text != null) {
                        switch (text) {
                            case "/start":
                                if (!authService.authenticate(telegramUserId)) {
                                    telegramService.sendMessage(chatId, "Привет! Пожалуйста, зарегистрируйтесь с помощью команды /register.");
                                } else {
                                    telegramService.sendMessage(chatId, "Вы уже зарегистрированы. Добро пожаловать обратно!");
                                }
                                break;
                            case "/register":
                                authService.registerOrUpdateUser(telegramUserId, true);
                                telegramService.sendMessage(chatId, "Вы успешно зарегистрированы. Теперь вы можете использовать все функции бота.");
                                break;
                            // Добавьте обработку других команд здесь, если необходимо.
                        }
                    }

                    // Проверка аутентификации перед обработкой медиа
                    if (authService.authenticate(telegramUserId)) {
                        if (update.message().voice() != null) {
                            audioMessageHandler.handleAudio(update.message().voice().fileId(), chatId);
                        } else if (update.message().audio() != null) {
                            audioMessageHandler.handleAudio(update.message().audio().fileId(), chatId);
                        } else if (update.message().video() != null) {
                            audioMessageHandler.handleVideo(update.message().video().fileId(), chatId);
                        }
                    } else {
                        telegramService.sendMessage(chatId, "Пожалуйста, зарегистрируйтесь с помощью команды /register для доступа к функциям.");
                    }
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;

        });
    }
}
