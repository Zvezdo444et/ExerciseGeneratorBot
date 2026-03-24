package zvezdo4et.service;

import org.springframework.beans.factory.annotation.Autowired;
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
    private JsonService jsonService;
    private final Map<Long, String> userState = new HashMap<>();
    private final Map<Long, Exercises> pendingExercises = new HashMap<>();
    private final Map<Long, String> pendingIdExercises = new HashMap<>();
    private final Map<String, Integer> countOfExercises = new HashMap<>();
    private ReplyKeyboardMarkup keyboardMarkup;

    public TelegramBotService(BotConfig config) {
        this.config = config;
        this.keyboardMarkup = createMainKeyboard();
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
                case "/getAll", "Библиотека упражнений \uD83D\uDCDA":
                    getAllExercises(chatId);
                    break;
                case "/addExr", "Добавить ➕":
                    addExr(chatId);
                    break;
                case "/editExr", "Изменить \uD83D\uDCDD":
                    editExr(chatId);
                    break;
                case "/delExr", "Удалить \uD83D\uDDD1\uFE0F":
                    delExr(chatId);
                    break;
                case "/getExr", "Получить \uD83C\uDFB2":
                    getExr(chatId);
                    break;
                default:
                    sendMessage(chatId, "Такой команды не найдено! Введите /help для просмотра списка команд");
                    break;
            }

        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Библиотека упражнений \uD83D\uDCDA");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Добавить ➕");
        row2.add("Получить \uD83C\uDFB2");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Изменить \uD83D\uDCDD");
        row3.add("Удалить \uD83D\uDDD1\uFE0F");
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("Ошибка при отправке сообщения в Telegram: " + e.getMessage());
        }
    }

    private void helloMessage(long chatId, String name) {
        String answer = "Привет, " + name + " Я бот выдающий случайное упражнение!\n" +
                "Введи /help - для получения информации";
        sendMessage(chatId, answer);
    }

    private void helpMessage(long chatId) {
        String answer = "Здравствуй! Вот список команд:\n" +
                "1) /getAll - получить все упражнения\n" +
                "2) /addExr - добавить упражнение\n" +
                "3) /editExr - редактировать упражнение\n" +
                "4) /delExr - удалить упражнение\n" +
                "5) /getExr - получить упражнение";
        sendMessage(chatId, answer);
    }

    private void getAllExercises(long chatId) {
        List<Exercises> list = jsonService.loadAll();
        String answer;
        if (list.isEmpty()) {
            answer = "Список упражнений пока пуст.";
            sendMessage(chatId, answer);
            return;
        }

        StringBuilder sb = new StringBuilder("Библиотека упражнений:\n\n");

        for (Exercises ex : list) {
            sb.append(ex.getExerciseId())
                    .append(") ")
                    .append(ex.getExerciseName())
                    .append(" — ")
                    .append(ex.getExerciseDescription())
                    .append("\n");
        }

        answer = sb.toString();
        sendMessage(chatId, answer);
    }

    private void addExr(long chatId) {
        sendMessage(chatId, "Введите пароль для добавления упражнения:");
        userState.put(chatId, "WAIT_ADD_PASS");
        pendingExercises.put(chatId, new Exercises());

    }

    private void handleAddExr(long chatId, String text) {
        String state = userState.get(chatId);
        Exercises ex = pendingExercises.get(chatId);
        if ("Отменить".equalsIgnoreCase(text)) {
            userState.remove(chatId);
            pendingExercises.remove(chatId);
            sendMessage(chatId, "Удаление отменено.");
            return;
        }
        if ("WAIT_ADD_PASS".equals(state)) {
            if (text.equals("passw123")){
                userState.put(chatId, "WAIT_NAME");
                sendMessage(chatId, "Пароль введён верно. Для выхода введите \"Отменить\" на любом из этапов.\nВведите название упражнения:");
            } else {
                sendMessage(chatId, "Вы ввели неверный пароль!");
                userState.remove(chatId);
                pendingExercises.remove(chatId);
            }
        } else if ("WAIT_NAME".equals(state)) {
            ex.setExerciseName(text);
            userState.put(chatId, "WAIT_DESC");
            sendMessage(chatId, "Название принято! Теперь введите описание:");

        } else if ("WAIT_DESC".equals(state)) {
            ex.setExerciseDescription(text);

            jsonService.addExercise(ex);

            userState.remove(chatId);
            pendingExercises.remove(chatId);

            sendMessage(chatId, "✅ Упражнение успешно сохранено под ID: " + ex.getExerciseId());
            sendMessage(chatId, jsonService.getByIdAsString(ex.getExerciseId()));
        }
    }

    private void delExr(long chatId) {
        sendMessage(chatId, "Введите пароль для удаления упражнения:");
        userState.put(chatId, "WAIT_DEL_PASS");

    }

    private void handleDelExr(long chatId, String text) {
        String state = userState.get(chatId);
        if ("Отменить".equalsIgnoreCase(text)) {
            userState.remove(chatId);
            sendMessage(chatId, "Удаление отменено.");
            return;
        }
        if ("WAIT_DEL_PASS".equals(state)) {
            if (text.equals("passw123")){
                getAllExercises(chatId);
                sendMessage(chatId, "Пароль введён верно. Для выхода введите \"Отменить\" на любом из этапов.\nВведите ID для удаления:");
                userState.put(chatId, "WAIT_ID_DEL");
            } else {
                sendMessage(chatId, "Вы ввели неверный пароль!");
                userState.remove(chatId);
            }
        } else if (!jsonService.exists(text)) {
            sendMessage(chatId, "Такого ID нет в списке!");
            sendMessage(chatId, "Введите ID для удаления:");
        } else if ("WAIT_ID_DEL".equals(state)) {
            jsonService.deleteById(text);

            userState.remove(chatId);

            sendMessage(chatId, "✅ Упражнение под ID " + text + " удалено!");
            getAllExercises(chatId);
        }
    }

    private void editExr(long chatId) {
        userState.put(chatId, "WAIT_EDIT_PASS");
        sendMessage(chatId, "Введите пароль для редактирования:");

    }

    private void handleEditExr(long chatId, String text) {
        String state = userState.get(chatId);
        if ("Отменить".equalsIgnoreCase(text)) {
            userState.remove(chatId);
            pendingIdExercises.remove(chatId);
            sendMessage(chatId, "Редактирование отменено.");
            return;
        }
        if ("WAIT_EDIT_PASS".equals(state)) {
            if (text.equals("passw123")){
                getAllExercises(chatId);
                pendingIdExercises.put(chatId, "");
                sendMessage(chatId, "Пароль введён верно. Для выхода введите \"Отменить\" на любом из этапов.\nВведите ID для редактирования:");
                userState.put(chatId, "WAIT_ID_EDIT");
            } else {
                sendMessage(chatId, "Вы ввели неверный пароль!");
                userState.remove(chatId);
            }
        } else if ("WAIT_ID_EDIT".equals(state)) {
            if (!jsonService.exists(text)) {
                sendMessage(chatId, "Такого ID нет в списке!");
                sendMessage(chatId, "Введите ID для редактирования:");
                return;
            }
            pendingIdExercises.put(chatId, text);
            userState.put(chatId, "WAIT_WHAT_EDIT");
            String answer = "ID принят! Теперь выберите что нужно отредактировать.\n" +
                    "1 - Название\n" +
                    "2 - Описание\n" +
                    "Ваш выбор:";
            sendMessage(chatId, answer);

        } else if ("WAIT_WHAT_EDIT".equals(state)) {
            if (text.equals("1")) {
                userState.put(chatId, "WAIT_EDIT_NAME");
                sendMessage(chatId, "Введите новое название:");
            } else if (text.equals("2")) {
                userState.put(chatId, "WAIT_EDIT_DESC");
                sendMessage(chatId, "Введите новое описание:");
            } else {
                sendMessage(chatId, "Вы ввели неверное число. Повторите ввод:");
            }
        } else if ("WAIT_EDIT_NAME".equals(state)) {
            jsonService.updateName(pendingIdExercises.get(chatId), text);

            sendMessage(chatId, "✅ В упражнение под ID " + pendingIdExercises.get(chatId) + " название успешно изменено на: " + text);
            sendMessage(chatId, jsonService.getByIdAsString(pendingIdExercises.get(chatId)));
            userState.remove(chatId);
            pendingIdExercises.remove(chatId);
        } else if ("WAIT_EDIT_DESC".equals(state)) {
            jsonService.updateDescription(pendingIdExercises.get(chatId), text);

            sendMessage(chatId, "✅ В упражнение под ID " + pendingIdExercises.get(chatId) + " описание успешно изменено на: " + text);
            sendMessage(chatId, jsonService.getByIdAsString(pendingIdExercises.get(chatId)));
            userState.remove(chatId);
            pendingIdExercises.remove(chatId);
        }
    }

    private void getExr(long chatId) {
        userState.put(chatId, "WAIT_COUNT");
        sendMessage(chatId, "Введите количество проигранных игр:");
    }

    private void handleGetExr(long chatId, String text) {
        String state = userState.get(chatId);
        if ("WAIT_COUNT".equals(state)) {
            int countLoose;
            Integer count;
            try {
                countLoose = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Вы ввели не число! Введите ещё раз:");
                return;
            }
            sendMessage(chatId, "Ваш список упражнений");
            countOfExercises.clear();
            Random random = new Random();
            int maxId = jsonService.getMaxId();
            for (int j = 0; j <= maxId; j++) {
                countOfExercises.put(String.valueOf(j), -1);
            }

            // В следующий раз использовать
            // countOfExercises.getOrDefault(randIdStr, 0); -> возвращает 0 если числа нет,
            // или число если оно найдено, позволяет не использовать костыли.

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
                    String exercise = jsonService.getRandomIdAsString(String.valueOf(i));

                    sendMessage(chatId, exercise + ".\nКоличество: " + count);
                }
            }
            userState.remove(chatId);
        }

    }


}
