package kr.ychoi.otplugin;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.*;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.Language;
import org.omegat.util.Preferences;
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
    private static final String BASE_PROMPT = 
            "You are a translation tool integrated in a CAT (Computer-Assisted Translation) tool. Translate the following text from %s to %s. Preserve the tags in the text and keep any segmentations intact.\n\n";
    
    private static final String PARAM_API_KEY = "openai.api.key";
    private static final String PARAM_MODEL = "openai.model";
    private static final String PARAM_TEMPERATURE = "openai.temperature";
    private static final String PARAM_CUSTOM_PROMPT = "custom.prompt";

    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final String DEFAULT_TEMPERATURE = "0";
    private static final String DEFAULT_CUSTOM_PROMPT = "";

    private JTextField apiKeyField;
    private JTextField modelField;
    private JTextField tempField;
    private JTextArea promptField;

    @Override
    protected String getPreferenceName() {
        return "allow_openai_translate";
    }

    public String getName() {
        if (Preferences.getPreferenceDefault(PARAM_API_KEY, "").isEmpty()) {
            return "OpenAI Translate (API Key Required)";
        } else {
            return "OpenAI Translate";
        }
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }
    
    @Override
    public void showConfigurationUI(Window parent) {
        JPanel configPanel = new JPanel(new java.awt.GridBagLayout());
        GridBagConstraints gridBagConstraints;

        int uiRow = 0;

        // API Key
        JLabel apiKeyLabel = new JLabel("API Key:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        configPanel.add(apiKeyLabel, gridBagConstraints);

        apiKeyField = new JTextField(Preferences.getPreferenceDefault(PARAM_API_KEY, ""), 52);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;  // 입력란이 창 크기에 맞춰 늘어남
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        configPanel.add(apiKeyField, gridBagConstraints);
        uiRow++;

        // Model
        JLabel modelLabel = new JLabel("Model:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        configPanel.add(modelLabel, gridBagConstraints);

        modelField = new JTextField(Preferences.getPreferenceDefault(PARAM_MODEL, DEFAULT_MODEL));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        configPanel.add(modelField, gridBagConstraints);
        uiRow++;

        // Temperature
        JLabel tempLabel = new JLabel("Temperature:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        configPanel.add(tempLabel, gridBagConstraints);

        tempField = new JTextField(Preferences.getPreferenceDefault(PARAM_TEMPERATURE, DEFAULT_TEMPERATURE));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        configPanel.add(tempField, gridBagConstraints);
        uiRow++;

        // Custom Prompt
        JLabel promptLabel = new JLabel("Custom Prompt:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 5);
        configPanel.add(promptLabel, gridBagConstraints);

        promptField = new JTextArea(5, 20);
        promptField.setText(Preferences.getPreferenceDefault(PARAM_CUSTOM_PROMPT, DEFAULT_CUSTOM_PROMPT));
        promptField.setLineWrap(true);
        promptField.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(promptField, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        configPanel.add(scrollPane, gridBagConstraints);
        uiRow++;

        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                try {
                    Preferences.setPreference(PARAM_API_KEY, (Object) apiKeyField.getText());
                    Preferences.setPreference(PARAM_MODEL, (Object) modelField.getText());
                    Preferences.setPreference(PARAM_TEMPERATURE, (Object) tempField.getText());
                    Preferences.setPreference(PARAM_CUSTOM_PROMPT, (Object) promptField.getText());
                } catch (Exception e) {
                    System.err.println("An error occurred while saving preferences: " + e.getMessage());
                }
            }
        };

        dialog.panel.add(configPanel);
        dialog.show();
    }
    
    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
    	String apiKey = Preferences.getPreferenceDefault(PARAM_API_KEY, "");
        String model = Preferences.getPreferenceDefault(PARAM_MODEL, DEFAULT_MODEL);
        float temperature = Float.parseFloat(Preferences.getPreferenceDefault(PARAM_TEMPERATURE, DEFAULT_TEMPERATURE));
    	
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
        return requestTranslation(systemPrompt, userPrompt, apiKey, model, temperature);
    }


    private String createSystemPrompt(Language sLang, Language tLang, List<GlossaryEntry> glossaryEntries) {
        String customPrompt = Preferences.getPreferenceDefault(PARAM_CUSTOM_PROMPT, DEFAULT_CUSTOM_PROMPT);

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
        if (!customPrompt.isEmpty()) {
            promptBuilder.append("\n").append(customPrompt).append("\n");
        }

        return promptBuilder.toString();
    }

    private String requestTranslation(String systemPrompt, String userPrompt, String apiKey, String model, float temperature) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userPrompt));

        Map<String, String> headers = new TreeMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + apiKey);

        String body = new JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("temperature", temperature)
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
