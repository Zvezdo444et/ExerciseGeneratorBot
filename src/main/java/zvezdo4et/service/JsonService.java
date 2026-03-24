package zvezdo4et.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import zvezdo4et.model.Exercises;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class JsonService {
    private final String FILE_PATH = "exercises.json";
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // Получить всё
    public List<Exercises> loadAll() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return new ArrayList<>();

        try {
            return mapper.readValue(file, new TypeReference<List<Exercises>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Получить по id
    public Exercises getById(String id) {
        List<Exercises> all = loadAll();

        return all.stream()
                .filter(ex -> ex.getExerciseId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Получить строку по id
    public String getByIdAsString(String id) {
        Exercises ex = getById(id);
        if (ex == null) {
            return "Упражнение с ID " + id + " не найдено.";
        }

        return String.format("%s) %s — %s",
                ex.getExerciseId(),
                ex.getExerciseName(),
                ex.getExerciseDescription());
    }

    public String getRandomIdAsString(String id) {
        Exercises ex = getById(id);
        if (ex == null) {
            return "Упражнение с ID " + id + " не найдено.";
        }

        return String.format("%s — %s",
                ex.getExerciseName(),
                ex.getExerciseDescription());
    }

    // Получить максимальный id
    public int getMaxId() {
        List<Exercises> list = loadAll();
        return list.stream()
                .mapToInt(ex -> {
                    try {
                        return Integer.parseInt(ex.getExerciseId());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0); // Если список пуст, начнем с 0
    }

    // Добавить
    public void addExercise(Exercises newEx) {
        List<Exercises> list = loadAll();

        String nextId = String.valueOf(getMaxId() + 1);
        newEx.setExerciseId(nextId);

        list.add(newEx);
        saveToFile(list);
    }

    // Редактировать описание по ID
    public void updateDescription(String id, String newDesc) {
        List<Exercises> list = loadAll();
        for (Exercises ex : list) {
            if (ex.getExerciseId().equals(id)) {
                ex.setExerciseDescription(newDesc);
                break;
            }
        }
        saveToFile(list);
    }

    // Редактировать название по ID
    public void updateName(String id, String newName) {
        List<Exercises> list = loadAll();
        for (Exercises ex : list) {
            if (ex.getExerciseId().equals(id)) {
                ex.setExerciseName(newName);
                break;
            }
        }
        saveToFile(list);
    }

    // Удалить
    public boolean deleteById(String id) {
        List<Exercises> list = loadAll();

        // Метод removeIf возвращает true, если элемент найден и удален.
        boolean removed = list.removeIf(ex -> ex.getExerciseId().equals(id));

        if (removed) {
            reindexAll(list);
            saveToFile(list);
        }

        return removed;
    }

    // Сохранить
    private void saveToFile(List<Exercises> list) {
        try {
            mapper.writeValue(new File(FILE_PATH), list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Есть ли такое id
    public boolean exists(String id) {
        List<Exercises> all = loadAll();
        return all.stream().anyMatch(ex -> ex.getExerciseId().trim().equals(id.trim()));
    }

    private void reindexAll(List<Exercises> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setExerciseId(String.valueOf(i + 1));
        }
    }
}
