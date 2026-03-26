package zvezdo4et.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import zvezdo4et.model.Exercises;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExercisesService {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Map<String, List<Exercises>> cache = new HashMap<>();

    public ExercisesService() {
        preloadCache("ru");
        preloadCache("en");
    }

    private void preloadCache(String lang) {
        String path = getFilePath(lang);
        File file = new File(path);
        if (file.exists()) {
            try {
                List<Exercises> data = mapper.readValue(file, new TypeReference<List<Exercises>>() {});
                cache.put(lang, new ArrayList<>(data));
            } catch (IOException e) {
                cache.put(lang, new ArrayList<>());
            }
        } else {
            cache.put(lang, new ArrayList<>());
        }
    }

    private String getFilePath(String lang) {
        return "exercises_" + lang + ".json";
    }

    // Получить всё
    public List<Exercises> loadAll(String lang) {
        return cache.getOrDefault(lang, new ArrayList<>());
    }

    // Получить по id
    public Exercises getById(String id, String lang) {
        return loadAll(lang).stream()
                .filter(ex -> ex.getExerciseId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Получить максимальный id
    public int getMaxId(String lang) {
        return loadAll(lang).size();
    }

    // Добавить
    public void addExercise(Exercises newEx, String lang) {
        List<Exercises> list = loadAll(lang);

        int maxId = list.stream()
                .mapToInt(ex -> {
                    try { return Integer.parseInt(ex.getExerciseId()); }
                    catch (NumberFormatException e) { return 0; }
                })
                .max().orElse(0);

        newEx.setExerciseId(String.valueOf(maxId + 1));
        list.add(newEx);

        saveToFile(lang);
    }

    // Редактировать описание по ID
    public void updateDescription(String id, String newDesc, String lang) {
        List<Exercises> list = loadAll(lang);
        for (Exercises ex : list) {
            if (ex.getExerciseId().equals(id)) {
                ex.setExerciseDescription(newDesc);
                break;
            }
        }
        saveToFile(lang);
    }

    // Редактировать название по ID
    public void updateName(String id, String newName, String lang) {
        List<Exercises> list = loadAll(lang);
        for (Exercises ex : list) {
            if (ex.getExerciseId().equals(id)) {
                ex.setExerciseName(newName);
                break;
            }
        }
        saveToFile(lang);
    }

    // Удалить
    public boolean deleteById(String id, String lang) {
        List<Exercises> list = loadAll(lang);

        // Метод removeIf возвращает true, если элемент найден и удален.
        boolean removed = list.removeIf(ex -> ex.getExerciseId().equals(id));

        if (removed) {
            reindexAll(list);
            saveToFile(lang);
        }

        return removed;
    }

    // Сохранить
    private void saveToFile(String lang) {
        try {
            mapper.writeValue(new File(getFilePath(lang)), cache.get(lang));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Есть ли такое id
    public boolean exists(String id, String lang) {
        return loadAll(lang).stream().anyMatch(ex -> ex.getExerciseId().trim().equals(id.trim()));
    }

    private void reindexAll(List<Exercises> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setExerciseId(String.valueOf(i + 1));
        }
    }
}
