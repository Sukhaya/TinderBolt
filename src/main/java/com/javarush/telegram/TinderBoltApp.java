package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "sukharikAiBot"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = "7808129788:AAGJAWW2WUnR1RrGCOUdqaPZFS5GP8TKGLk"; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "gpt:4dws6NYyD0BDK2ufp71ZJFkblB3TCC3tppbmX6OYmhSFydbM"; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGPTService = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();
    private UserInfo me;
    private UserInfo companion;
    private int questionCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();

        //command MAIN
        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");
            sendTextMessage(text);
            sendTextMessage("☝\uFE0F Жми на интересующую тебя команду");

            showMainMenu("главное меню бота", "/start",
                    "генерация Tinder-профля \uD83D\uDE0E", "/profile",
                    "сообщение для знакомства \uD83E\uDD70", "/opener ",
                    "переписка от вашего имени \uD83D\uDE08", "/message",
                    "переписка со звездами \uD83D\uDD25", "/date",
                    "задать вопрос чату GPT \uD83E\uDDE0", "/gpt");
            return;
        }

        //command GPT
        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }

        if (currentMode == DialogMode.GPT && !isMessageCommand()) {
            String prompt = loadMessage("gpt");
            Message messageGPT = sendTextMessage("Буквально несколько секунд. ChatGPT уже думает...");
            String answerGPT = chatGPTService.sendMessage(prompt, message);
            updateTextMessage(messageGPT, answerGPT);
            return;
        }

        //command DATE
        if (message.equals("/date")) {
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райан Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }

        if (currentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage("У Вас хороший вкус! \nТвоя задача пригласить собеседника на свидание за 5 сообщений " +
                        "\uD83D\uDC69\u200D❤\uFE0F\u200D\uD83D\uDC68");

                String prompt = loadPrompt(query);
                chatGPTService.setPrompt(prompt);
                return;
            }
            Message messageGPT = sendTextMessage("Ваш собеседник печатает...");
            String answerGPT = chatGPTService.addMessage(message);
            updateTextMessage(messageGPT, answerGPT);
            return;
        }

        //command MESSAGE
        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите в чат ашу переписку",
                    "Следующее сообщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }

        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);
                Message messageGPT = sendTextMessage("Буквально несколько секунд. ChatGPT уже думает...");
                String answerGPT = chatGPTService.sendMessage(prompt, userChatHistory);
                updateTextMessage(messageGPT, answerGPT);
                return;
            }
            list.add(message);
            return;
        }

        //command PROFILE
        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");

            me = new UserInfo();
            questionCount = 1;
            sendTextMessage("Сколько Вам лет?");
            return;
        }

        if (currentMode == DialogMode.PROFILE && !isMessageCommand()) {
            switch (questionCount) {
                case 1:
                    me.age = message;
                    questionCount = 2;
                    sendTextMessage("Кем Вы работаете?");
                    return;
                case 2:
                    me.occupation = message;
                    questionCount = 3;
                    sendTextMessage("У Вас есть хобби?");
                    return;
                case 3:
                    me.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Что Вам НЕ нравится в людях?");
                    return;
                case 4:
                    me.annoys = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    me.goals = message;

                    String aboutMyself = me.toString();
                    String prompt = loadPrompt("profile");
                    Message messageGPT = sendTextMessage("Буквально несколько секунд. ChatGPT уже думает...");
                    String answerGPT = chatGPTService.sendMessage(prompt, aboutMyself);
                    updateTextMessage(messageGPT, answerGPT);
                    return;
            }
            return;
        }

        //command OPENER
        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");

            companion = new UserInfo();
            questionCount = 1;
            sendTextMessage("Как зовут Вашего собеседника?");
            return;
        }

        if (currentMode == DialogMode.OPENER && !isMessageCommand()) {
            switch (questionCount) {
                case 1:
                    companion.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько ему/ей лет?");
                    return;
                case 2:
                    companion.age = message;
                    questionCount = 3;
                    sendTextMessage("Есть ли у него/неё хобби?");
                    return;
                case 3:
                    companion.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Кем она/он работает?");
                    return;
                case 4:
                    companion.occupation = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакоства?");
                    return;
                case 5:
                    companion.goals = message;

                    String aboutСompanion = message;
                    String prompt = loadPrompt("opener");
                    Message messageGPT = sendTextMessage("Буквально несколько секунд. ChatGPT уже думает...");
                    String answerGPT = chatGPTService.sendMessage(prompt, aboutСompanion);
                    updateTextMessage(messageGPT, answerGPT);
                    return;
            }
            return;
        }

        sendTextMessage("*Привет!* \n_Рады приветствовать тебя в нашем телеграмм-боте_");
        sendTextButtonsMessage("Выберите режим работы: ",
                "Старт", "start",
                "Стоп", "stop");
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
