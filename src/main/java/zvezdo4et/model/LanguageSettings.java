package zvezdo4et.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LanguageSettings {
    @JsonProperty("chatId")
    private String chatId;

    @JsonProperty("language")
    private String language;

    public LanguageSettings() {
    }

    public LanguageSettings(String chatId, String language) {
        this.chatId = chatId;
        this.language = language;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }


}

