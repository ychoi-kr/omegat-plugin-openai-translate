package kr.ychoi.otplugin;

import java.util.*;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.util.Language;
import org.omegat.util.WikiGet;
import org.json.*;
import org.omegat.gui.glossary.GlossaryEntry;
import org.omegat.gui.glossary.GlossaryManager;
import org.omegat.core.Core;
import org.omegat.tokenizer.ITokenizer;
import org.omegat.util.Token;

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

    private static final String SYSTEM_PROMPT_WITH_GLOSSARY = 
	    "You are a translation tool integrated in a CAT (Computer-Assisted Translation) tool. Translate the following text from %s to %s using the provided glossary. Preserve the tags in the text and keep any segmentations intact.\n\n" +
	    "Translate the following text exactly as it is, even if it seems incomplete or segmented. Handle line breaks appropriately and do not change the structure of the text. Do not repeat the original text. Ensure all tags are preserved and correctly placed in the translation. Do not ask for additional text or clarification.\n\n" +
	    "Glossary:\n%s\n";

	private static final String SYSTEM_PROMPT_WITHOUT_GLOSSARY = 
	    "You are a translation tool integrated in a CAT (Computer-Assisted Translation) tool. Translate the following text from %s to %s. Preserve the tags in the text and keep any segmentations intact.\n\n" +
	    "Translate the following text exactly as it is, even if it seems incomplete or segmented. Handle line breaks appropriately and do not change the structure of the text. Do not repeat the original text. Ensure all tags are preserved and correctly placed in the translation. Do not ask for additional text or clarification.\n";

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
        
     // 텍스트 토큰화
        Set<String> wordsInText = tokenizeText(text);

        // 용어 검색
        List<GlossaryEntry> glossaryEntries = searchGlossary(wordsInText);

        // 시스템 프롬프트 및 사용자 프롬프트 작성
        String systemPrompt = createSystemPrompt(sLang, tLang, glossaryEntries);
        System.out.println(systemPrompt);
        String userPrompt = text;
        System.out.println(userPrompt);

        // OpenAI API 요청
        String translatedText = requestTranslation(systemPrompt, userPrompt);

        // 캐시에 저장
        putToCache(sLang, tLang, text, translatedText);

        return translatedText;
    }
    
    private Set<String> tokenizeText(String text) throws Exception {
        ITokenizer tokenizer = Core.getProject().getSourceTokenizer();
        Token[] tokens = tokenizer.tokenizeWords(text, ITokenizer.StemmingMode.NONE);
        Set<String> words = new HashSet<>();
        for (Token token : tokens) {
            words.add(token.getTextFromString(text).toLowerCase());
        }
        return words;
    }

    private List<GlossaryEntry> searchGlossary(Set<String> wordsInText) throws Exception {
        GlossaryManager glossaryManager = Core.getGlossaryManager();
        List<GlossaryEntry> glossaryEntries = glossaryManager.getGlossaryEntries(String.join(" ", wordsInText));
        List<GlossaryEntry> relevantEntries = new ArrayList<>();
        for (GlossaryEntry entry : glossaryEntries) {
            if (wordsInText.contains(entry.getSrcText().toLowerCase())) {
                relevantEntries.add(entry);
            }
        }
        return relevantEntries;
    }

    private String createSystemPrompt(Language sLang, Language tLang, List<GlossaryEntry> glossaryEntries) {
        if (glossaryEntries.isEmpty()) {
            return String.format(SYSTEM_PROMPT_WITHOUT_GLOSSARY, sLang.getLanguage(), tLang.getLanguage());
        } else {
            StringBuilder glossaryBuilder = new StringBuilder();
            for (GlossaryEntry entry : glossaryEntries) {
                String[] locTerms = entry.getLocTerms(false);
                String locTerm = locTerms.length > 0 ? locTerms[0] : "";
                glossaryBuilder.append(entry.getSrcText()).append("\t").append(locTerm).append("\n");
            }
            return String.format(SYSTEM_PROMPT_WITH_GLOSSARY, sLang.getLanguage(), tLang.getLanguage(), glossaryBuilder.toString());
        }
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
