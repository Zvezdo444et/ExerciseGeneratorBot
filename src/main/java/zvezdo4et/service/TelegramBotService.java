package zvezdo4et.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import zvezdo4et.config.BotConfig;
import zvezdo4et.model.Exercises;

import java.util.*;

@Component
public class TelegramBotService extends TelegramLongPollingBot {
    private final BotConfig config;
    @Autowired
    private ExercisesService jsonService;
    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, Exercises> pendingExercises = new HashMap<>();
    private final Map<Long, String> pendingIdExercises = new HashMap<>();
    private final Map<String, Integer> countOfExercises = new HashMap<>();
    private ReplyKeyboardMarkup keyboardMarkup;
    private final LanguageService languageService;
    private final MessageSource messageSource;

    public TelegramBotService(BotConfig config, LanguageService languageService, MessageSource messageSource) {
        this.config = config;
        this.languageService = languageService;
        this.messageSource = messageSource;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (update.hasMessage() && message.hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = message.getText();
            String userLang = languageService.getLang(chatId);
            this.keyboardMarkup = getMainKeyboard(userLang);
            if (userState.containsKey(chatId)) {
                String key = userState.get(chatId);
                switch (key) {
                    case "WAIT_NAME", "WAIT_DESC", "WAIT_ADD_PASS":
                        handleAddExr(chatId, messageText);
                        break;
                    case "WAIT_ID_DEL", "WAIT_DEL_PASS":
                        handleDelExr(chatId, messageText);
                        break;
                    case "WAIT_ID_EDIT", "WAIT_WHAT_EDIT", "WAIT_EDIT_NAME", "WAIT_EDIT_DESC", "WAIT_EDIT_PASS":
                        handleEditExr(chatId, messageText);
                        break;
                    case "WAIT_COUNT":
                        handleGetExr(chatId, messageText);
                        break;
                }
                return;
            }

            switch (messageText) {
                case "/start":
                    helloMessage(chatId, message.getChat().getFirstName());
                    break;
                case "/help":
                    helpMessage(chatId);
                    break;
                case "/getAll", "Библиотека упражнений \uD83D\uDCDA", "Exercise library \uD83D\uDCDA":
                    getAllExercises(chatId);
                    break;
                case "/addExr", "Добавить ➕", "Add ➕":
                    addExr(chatId);
                    break;
                case "/editExr", "Изменить \uD83D\uDCDD", "Change \uD83D\uDCDD":
                    editExr(chatId);
                    break;
                case "/delExr", "Удалить \uD83D\uDDD1\uFE0F", "Delete \uD83D\uDDD1\uFE0F":
                    delExr(chatId);
                    break;
                case "/getExr", "Получить \uD83C\uDFB2", "Get \uD83C\uDFB2":
                    getExr(chatId);
                    break;
                case "/set_en", "Change language to en \uD83C\uDDEC\uD83C\uDDE7":
                    languageService.setLang(chatId, "en");
                    this.keyboardMarkup = getMainKeyboard("en");
                    sendMessage(chatId, getLangMessage("bot.lang_change", chatId));
                    break;
                case "/set_ru", "Изменить язык на ru \uD83C\uDDF7\uD83C\uDDFA":
                    languageService.setLang(chatId, "ru");
                    this.keyboardMarkup = getMainKeyboard("ru");
                    sendMessage(chatId, getLangMessage("bot.lang_change", chatId));
                    break;
                default:
                    sendMessage(chatId, getLangMessage("bot.command_not_found", chatId));
                    break;
            }

        }
    }

    private ReplyKeyboardMarkup getMainKeyboard(String lang) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();
        KeyboardRow row4 = new KeyboardRow();

        if ("ru".equals(lang)) {
            row1.add("Библиотека упражнений \uD83D\uDCDA");

            row2.add("Добавить ➕");
            row2.add("Получить \uD83C\uDFB2");

            row3.add("Изменить \uD83D\uDCDD");
            row3.add("Удалить \uD83D\uDDD1\uFE0F");

            row4.add("Change language to en \uD83C\uDDEC\uD83C\uDDE7");
        } else {
            row1.add("Exercise library \uD83D\uDCDA");

            row2.add("Add ➕");
            row2.add("Get \uD83C\uDFB2");

            row3.add("Change \uD83D\uDCDD");
            row3.add("Delete \uD83D\uDDD1\uFE0F");

            row4.add("Изменить язык на ru \uD83C\uDDF7\uD83C\uDDFA");
        }
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }


    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        String currentLang = languageService.getLang(chatId);
        message.setReplyMarkup(getMainKeyboard(currentLang));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void helloMessage(long chatId, String name) {
        sendMessage(chatId, getLangMessage("bot.hello", chatId, name));
    }

    private void helpMessage(long chatId) {
        sendMessage(chatId, getLangMessage("bot.help", chatId));
    }

    private void getAllExercises(long chatId) {
        String lang = languageService.getLang(chatId);
        List<Exercises> list = jsonService.loadAll(lang);
        if (list.isEmpty()) {
            sendMessage(chatId, getLangMessage("ex.empty", chatId));
            return;
        }

        StringBuilder sb = new StringBuilder(getLangMessage("ex.library_header", chatId));

        for (Exercises ex : list) {
            String line = getLangMessage("ex.item_format", chatId,
                    ex.getExerciseId(),
                    ex.getExerciseName(),
                    ex.getExerciseDescription());
            sb.append(line).append("\n");
        }

        sendMessage(chatId, sb.toString());
    }

    private void addExr(long chatId) {
        sendMessage(chatId, getLangMessage("bot.wait_add_pass", chatId));
        userState.put(chatId, "WAIT_ADD_PASS");
        pendingExercises.put(chatId, new Exercises());

    }

    private void sendExerciseById(long chatId, String id) {
        Exercises ex = jsonService.getById(id, languageService.getLang(chatId));

        String answer;
        if (ex == null) {
            answer = getLangMessage("ex.not_found", chatId, id);
        } else {
            answer = getLangMessage("ex.item_format", chatId,
                    ex.getExerciseId(),
                    ex.getExerciseName(),
                    ex.getExerciseDescription());
        }

        sendMessage(chatId, answer);
    }

    private String getRandomExerciseById(long chatId, String id) {
        Exercises ex = jsonService.getById(id, languageService.getLang(chatId));

        String answer;
        if (ex == null) {
            answer = getLangMessage("ex.not_found", chatId, id);
        } else {
            answer = getLangMessage("ex.display_format", chatId,
                    ex.getExerciseName(),
                    ex.getExerciseDescription());
        }

        return answer;
    }

    private void handleAddExr(long chatId, String text) {
        String state = userState.get(chatId);
        Exercises ex = pendingExercises.get(chatId);
        if ("Отменить".equalsIgnoreCase(text) || "Cancel".equalsIgnoreCase(text) || "Отмена".equalsIgnoreCase(text)) {
            userState.remove(chatId);
            pendingExercises.remove(chatId);
            sendMessage(chatId, getLangMessage("bot.process_cancel", chatId));
            return;
        }
        if ("WAIT_ADD_PASS".equals(state)) {
            if (text.equals("passw123")){
                userState.put(chatId, "WAIT_NAME");
                sendMessage(chatId, getLangMessage("bot.add_pass_right", chatId));
            } else {
                sendMessage(chatId, getLangMessage("bot.wrong_pass", chatId));
                userState.remove(chatId);
                pendingExercises.remove(chatId);
            }
        } else if ("WAIT_NAME".equals(state)) {
            ex.setExerciseName(text);
            userState.put(chatId, "WAIT_DESC");
            sendMessage(chatId, getLangMessage("bot.name_accept", chatId));

        } else if ("WAIT_DESC".equals(state)) {
            ex.setExerciseDescription(text);

            jsonService.addExercise(ex, languageService.getLang(chatId));

            userState.remove(chatId);
            pendingExercises.remove(chatId);

            sendMessage(chatId, getLangMessage("bot.exrs_saved", chatId, ex.getExerciseId()));
            sendExerciseById(chatId, ex.getExerciseId());
        }
    }

    private void delExr(long chatId) {
        sendMessage(chatId, getLangMessage("bot.wait_del_pass", chatId));
        userState.put(chatId, "WAIT_DEL_PASS");

    }

    private void handleDelExr(long chatId, String text) {
        String state = userState.get(chatId);
        if ("Отменить".equalsIgnoreCase(text) || "Cancel".equalsIgnoreCase(text) || "Отмена".equalsIgnoreCase(text)) {
            userState.remove(chatId);
            sendMessage(chatId, getLangMessage("bot.process_cancel", chatId));
            return;
        }
        if ("WAIT_DEL_PASS".equals(state)) {
            if (text.equals("passw123")){
                sendMessage(chatId, getLangMessage("bot.del_pass_right", chatId));
                getAllExercises(chatId);
                userState.put(chatId, "WAIT_ID_DEL");
            } else {
                sendMessage(chatId, getLangMessage("bot.wrong_pass", chatId));
                userState.remove(chatId);
            }
        } else if (!jsonService.exists(text, languageService.getLang(chatId))) {
            sendMessage(chatId, getLangMessage("bot.id_not_found", chatId));
            sendMessage(chatId, getLangMessage("bot.id_del_found", chatId));
        } else if ("WAIT_ID_DEL".equals(state)) {
            jsonService.deleteById(text, languageService.getLang(chatId));

            userState.remove(chatId);

            sendMessage(chatId, getLangMessage("bot.exrs_delete", chatId, text));
            getAllExercises(chatId);
        }
    }

    private void editExr(long chatId) {
        userState.put(chatId, "WAIT_EDIT_PASS");
        sendMessage(chatId, getLangMessage("bot.wait_edit_pass", chatId));

    }

    private void handleEditExr(long chatId, String text) {
        String state = userState.get(chatId);
        if ("Отменить".equalsIgnoreCase(text) || "Cancel".equalsIgnoreCase(text) || "Отмена".equalsIgnoreCase(text)) {
            userState.remove(chatId);
            pendingIdExercises.remove(chatId);
            sendMessage(chatId, getLangMessage("bot.process_cancel", chatId));
            return;
        }
        if ("WAIT_EDIT_PASS".equals(state)) {
            if (text.equals("passw123")){
                pendingIdExercises.put(chatId, "");
                sendMessage(chatId, getLangMessage("bot.edit_pass_right", chatId));
                getAllExercises(chatId);
                userState.put(chatId, "WAIT_ID_EDIT");
            } else {
                sendMessage(chatId, getLangMessage("bot.wrong_pass", chatId));
                userState.remove(chatId);
            }
        } else if ("WAIT_ID_EDIT".equals(state)) {
            if (!jsonService.exists(text, languageService.getLang(chatId))) {
                sendMessage(chatId, getLangMessage("bot.id_not_found", chatId));
                sendMessage(chatId, getLangMessage("bot.id_edit_found", chatId));
                return;
            }
            pendingIdExercises.put(chatId, text);
            userState.put(chatId, "WAIT_WHAT_EDIT");
            sendMessage(chatId, getLangMessage("bot.id_edit_accept", chatId));

        } else if ("WAIT_WHAT_EDIT".equals(state)) {
            if (text.equals("1")) {
                userState.put(chatId, "WAIT_EDIT_NAME");
                sendMessage(chatId, getLangMessage("bot.write_new_name", chatId));
            } else if (text.equals("2")) {
                userState.put(chatId, "WAIT_EDIT_DESC");
                sendMessage(chatId, getLangMessage("bot.write_new_desc", chatId));
            } else {
                sendMessage(chatId, getLangMessage("bot.incorrect_number", chatId));
            }
        } else if ("WAIT_EDIT_NAME".equals(state)) {
            jsonService.updateName(pendingIdExercises.get(chatId), text, languageService.getLang(chatId));

            sendMessage(chatId, getLangMessage("bot.name_changed", chatId, pendingIdExercises.get(chatId), text));
            sendExerciseById(chatId, pendingIdExercises.get(chatId));
            userState.remove(chatId);
            pendingIdExercises.remove(chatId);
        } else if ("WAIT_EDIT_DESC".equals(state)) {
            jsonService.updateDescription(pendingIdExercises.get(chatId), text, languageService.getLang(chatId));

            sendMessage(chatId, getLangMessage("bot.desc_changed", chatId, pendingIdExercises.get(chatId), text));
            sendExerciseById(chatId, pendingIdExercises.get(chatId));
            userState.remove(chatId);
            pendingIdExercises.remove(chatId);
        }
    }

    private void getExr(long chatId) {
        userState.put(chatId, "WAIT_COUNT");
        sendMessage(chatId, getLangMessage("bot.write_count_looses", chatId));
    }

    private void handleGetExr(long chatId, String text) {
        String state = userState.get(chatId);
        if ("WAIT_COUNT".equals(state)) {
            int countLoose;
            Integer count;
            try {
                countLoose = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                sendMessage(chatId, getLangMessage("bot.write_not_number", chatId));
                return;
            }
            sendMessage(chatId, getLangMessage("bot.list_of_exrs", chatId));
            countOfExercises.clear();
            Random random = new Random();
            int maxId = jsonService.getMaxId(languageService.getLang(chatId));
            for (int j = 0; j <= maxId; j++) {
                countOfExercises.put(String.valueOf(j), -1);
            }

            for (int i = 0; i < countLoose; i++) {
                int randomId = random.nextInt(maxId) + 1;
                String randId = String.valueOf(randomId);
                count = countOfExercises.get(randId);
                int randomCount = random.nextInt(6, 16);
                if (count == -1) {
                    countOfExercises.put(randId, randomCount);
                } else {
                    countOfExercises.put(randId, count+randomCount);
                }
            }

            for (int i = 0; i < countOfExercises.size(); i++) {
                count = countOfExercises.get(String.valueOf(i));
                if (count == -1) {
                } else {
                    String exercise = getRandomExerciseById(chatId, String.valueOf(i));

                    sendMessage(chatId, getLangMessage("bot.count", chatId, exercise, count));
                }
            }
            userState.remove(chatId);
        }

    }


    private String getLangMessage(String key, long chatId, Object... args) {
        String lang = languageService.getLang(chatId);
        Locale locale = new Locale(lang);
        return messageSource.getMessage(key, args, locale);
    }

}
