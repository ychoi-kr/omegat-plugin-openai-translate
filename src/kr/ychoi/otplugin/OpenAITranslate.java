package kr.ychoi.otplugin;

import java.util.*;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.util.Language;
import org.omegat.util.WikiGet;
import org.json.*;
import org.omegat.gui.glossary.GlossaryEntry;
import org.omegat.gui.glossary.GlossarySearcher;
import org.omegat.core.Core;
import org.omegat.core.data.SourceTextEntry;

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
    private static final String CUSTOM_PROMPT = System.getProperty("custom.prompt", "");

    private static final String BASE_PROMPT = 
            "You are a translation tool integrated in a CAT (Computer-Assisted Translation) tool. Translate the following text from %s to %s. Preserve the tags in the text and keep any segmentations intact.\n\n";

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
        // 프로젝트에서 SourceTextEntry를 찾음
        List<SourceTextEntry> entries = Core.getProject().getAllEntries();
        SourceTextEntry matchingEntry = null;

        for (SourceTextEntry entry : entries) {
            if (entry.getSrcText().equals(text)) {
                matchingEntry = entry;
                break;
            }
        }

        List<GlossaryEntry> glossaryEntries = new ArrayList<>();
        if (matchingEntry != null) {
            // GlossarySearcher를 사용하여 용어집 검색 수행
            GlossarySearcher glossarySearcher = new GlossarySearcher(Core.getProject().getSourceTokenizer(), sLang, true);
            glossaryEntries = glossarySearcher.searchSourceMatches(matchingEntry, Core.getGlossaryManager().getGlossaryEntries(text));
        }

        // 시스템 프롬프트 및 사용자 프롬프트 작성
        String systemPrompt = createSystemPrompt(sLang, tLang, glossaryEntries);
        System.out.println(systemPrompt);
        String userPrompt = text;
        System.out.println(userPrompt);

        // OpenAI API 요청
        return requestTranslation(systemPrompt, userPrompt);
    }


    private String createSystemPrompt(Language sLang, Language tLang, List<GlossaryEntry> glossaryEntries) {
        StringBuilder promptBuilder = new StringBuilder();

        // 기본 지침 추가
        promptBuilder.append(String.format(BASE_PROMPT, sLang.getLanguage(), tLang.getLanguage()));

        // Glossary가 있을 경우 추가
        if (!glossaryEntries.isEmpty()) {
            promptBuilder.append("Glossary:\n");
            for (GlossaryEntry entry : glossaryEntries) {
                String[] locTerms = entry.getLocTerms(false);
                String locTerm = locTerms.length > 0 ? locTerms[0] : "";
                promptBuilder.append(entry.getSrcText()).append("\t").append(locTerm).append("\n");
            }
        }

        // 사용자 정의 프롬프트 추가
        if (!CUSTOM_PROMPT.isEmpty()) {
            promptBuilder.append("\n").append(CUSTOM_PROMPT).append("\n");
        }

        return promptBuilder.toString();
    }

    private String requestTranslation(String systemPrompt, String userPrompt) throws Exception {
        JSONArray messages = new JSONArray();
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

        try {
            String response = WikiGet.postJSON(API_URL, body, headers);
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                if (choice.has("message")) {
                    JSONObject message = choice.getJSONObject("message");
                    return message.getString("content").trim();
                }
            }
            return "Translation failed";
        } catch (Exception e) {
            return "Error contacting OpenAI API: " + e.getMessage();
        }
    }
}
