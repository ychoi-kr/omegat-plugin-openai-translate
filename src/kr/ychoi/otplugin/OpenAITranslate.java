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
            return "";
        }

        String lvSourceLang = sLang.getLanguageCode().substring(0, 2).toUpperCase();
        String lvTargetLang = tLang.getLanguageCode().substring(0, 2).toUpperCase();

        String cacheKey = lvSourceLang + '-' + lvTargetLang + text;
        String cachedResult = translationCache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        String model = "text-davinci-003";
        int max_tokens = 4000;
//        Below is commented out because of 404 issue.
//        See https://community.openai.com/t/when-i-try-the-gpt-4-model-chat-completion-in-api-request-i-get-an-error-that-model-does-not-exist/98850
//        if (text.length() > max_tokens * 0.3) {
//        	model = "gpt-4";
//        	max_tokens = 8000;
//        }

        String prompt = String.format("Translate below into %s: %s", lvTargetLang, text);
        String escapedPrompt = prompt.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n");
        
        Map<String, String> headers = new TreeMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + API_KEY);

        String body = String.format("{ \"model\": \"%s\", \"prompt\": \"%s\", \"max_tokens\": %d, \"temperature\": 0 }", model, escapedPrompt, max_tokens);
        System.out.println(body);
        String response = WikiGet.postJSON(API_URL, body, headers);
        JSONObject jsonResponse = new JSONObject(response);
        String translatedText = jsonResponse.getJSONArray("choices").getJSONObject(0).getString("text").trim();

        translationCache.put(cacheKey, translatedText);
        return translatedText;
    }
}

