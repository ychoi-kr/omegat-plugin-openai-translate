package kr.ychoi.otplugin;

import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.util.Language;
import org.omegat.util.WikiGet;
import org.json.*;

/*
 * OpenAI Translate plugin for OmegaT
 * based on Naver Translate plugin by ParanScreen https://github.com/ParanScreen/omegat-plugin-navertranslate
 * licensed under GNU GPLv2 and modified by ychoi
 */


@SuppressWarnings({"unchecked", "rawtypes"})
public class OpenAITranslate extends BaseTranslate {

    private static final String API_URL = "https://api.openai.com/v1/completions";
    private static final String API_KEY = System.getProperty("openai.api.key");

    private static final Map<String, String> translationCache = new HashMap<>();

    @Override
    protected String getPreferenceName() {
        return "allow_naver_translate";
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
            return "";
        }

        String lvSourceLang = sLang.getLanguageCode().substring(0, 2).toUpperCase();
        String lvTargetLang = tLang.getLanguageCode().substring(0, 2).toUpperCase();

        String cacheKey = lvSourceLang + '-' + lvTargetLang + text;
        String cachedResult = translationCache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        String prompt = String.format("Translate below into %s: %s", lvTargetLang, text);
        
        Map<String, String> headers = new TreeMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + API_KEY);

        String body = String.format("{ \"model\": \"text-davinci-003\", \"prompt\": \"%s\", \"max_tokens\": 500, \"temperature\": 0 }", prompt.replaceAll("\"", "\\\\\""));

        String response = WikiGet.postJSON(API_URL, body, headers);
        JSONObject jsonResponse = new JSONObject(response);
        String translatedText = jsonResponse.getJSONArray("choices").getJSONObject(0).getString("text").trim();

        translationCache.put(cacheKey, translatedText);
        return translatedText;
    }
}

