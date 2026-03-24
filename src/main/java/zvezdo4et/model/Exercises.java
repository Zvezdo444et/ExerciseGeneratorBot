package zvezdo4et.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Exercises {
    @JsonProperty("exerciseId")
    private String exerciseId;

    @JsonProperty("exerciseName")
    private String exerciseName;

    @JsonProperty("exerciseDescription")
    private String exerciseDescription;


    public Exercises() {
    }

    public Exercises(String exerciseId, String exerciseName, String exerciseDescription) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.exerciseDescription = exerciseDescription;

    }

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public String getExerciseDescription() {
        return exerciseDescription;
    }

    public void setExerciseDescription(String exerciseDescription) {
        this.exerciseDescription = exerciseDescription;
    }
}
