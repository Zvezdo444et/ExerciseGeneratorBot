package zvezdo4et.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class LanguageService {
    private final String FILE_PATH = "language.json";
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Map<Long, String> userLanguages;

    public LanguageService() {
        File file = new File(FILE_PATH);
        Map<Long, String> loadedData = new HashMap<>();

        if (file.exists()) {
            try {
                loadedData = mapper.readValue(file, new TypeReference<Map<Long, String>>() {});
            } catch (IOException e) {
                System.err.println("Ошибка загрузки языков: " + e.getMessage());
            }
        }
        this.userLanguages = loadedData;
    }

    public String getLang(long chatId) {
        if (!userLanguages.containsKey(chatId)) {
            setLang(chatId, "ru");
        }
        return userLanguages.get(chatId);
    }

    public void setLang(long chatId, String lang) {
        userLanguages.put(chatId, lang);
        saveToFile();
    }

    private void saveToFile() {
        try {
            mapper.writeValue(new File(FILE_PATH), userLanguages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
