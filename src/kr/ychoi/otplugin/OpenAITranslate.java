package kr.ychoi.otplugin;

import java.util.TreeMap;
import java.util.Map;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.util.Language;
import org.omegat.util.WikiGet;
import org.json.*;

/*
 * OpenAI Translate plugin for OmegaT
 * based on Naver Translate plugin by ParanScreen https://github.com/ParanScreen/omegat-plugin-navertranslate
 * licensed under GNU GPLv2 and modified by ychoi
 */


public class OpenAITranslate extends BaseCachedTranslate {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = System.getProperty("openai.api.key");
    private static final String MODEL = System.getProperty("openai.model", "gpt-4o");
    private static final float TEMPERATURE = Float.parseFloat(System.getProperty("openai.temperature", "0"));

    @Override
    protected String getPreferenceName() {
        return "allow_openai_translate";
    }

    public String getName() {
        if (API_KEY == null) {
            return "OpenAI Translate (API Key Required)";
        } else {
            return "OpenAI Translate";
        }
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        if (API_KEY == null) {
            return "API key not found";
        }

        String cachedResult = getCachedTranslation(sLang, tLang, text);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        JSONArray messages = new JSONArray();

        String lvSourceLang = sLang.getLanguageCode().substring(0, 2).toUpperCase();
        String lvTargetLang = tLang.getLanguageCode().substring(0, 2).toUpperCase();

        String systemPrompt = String.format("You will be provided with a sentence in %s, and your task is to translate it into %s.", lvSourceLang, lvTargetLang);
        String userPrompt = text;

        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
        
        Map<String, String> headers = new TreeMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + API_KEY);

        String body = new JSONObject()
                .put("model", MODEL)
                .put("messages", messages)
                .put("temperature", TEMPERATURE)
                .toString();
        
        System.out.println(body);
        try {
            String response = WikiGet.postJSON(API_URL, body, headers);
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                if (choice.has("message")) {
                    JSONObject message = choice.getJSONObject("message");
                    String translatedText = message.getString("content").trim();
                    putToCache(sLang, tLang, text, translatedText);  // 캐시에 저장
                    return translatedText;
                }
            }
            return "Translation failed";
        } catch (Exception e) {
            return "Error contacting OpenAI API: " + e.getMessage();
        }
    }
}

