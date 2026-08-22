package kr.ychoi.otplugin;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.Language;
import org.omegat.util.Preferences;
import org.json.*;
import org.omegat.gui.glossary.GlossaryEntry;
import org.omegat.gui.glossary.GlossarySearcher;
import org.omegat.core.Core;
import org.omegat.core.data.SourceTextEntry;

/*
 * OpenAI Translate plugin for OmegaT
 * based on Naver Translate plugin by ParanScreen https://github.com/ParanScreen/omegat-plugin-navertranslate
 * licensed under GNU GPLv2 and modified by ychoi
 *
 * Modified to use the OpenAI Responses API (/v1/responses) instead of the
 * legacy Chat Completions API, and to let the user pick a model from a
 * dropdown populated from the account's available models (GET /v1/models)
 * instead of typing the model name by hand.
 */


public class OpenAITranslate extends BaseCachedTranslate {

    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String MODELS_URL = "https://api.openai.com/v1/models";
    private static final String BASE_PROMPT =
            "You are a translation tool integrated in a CAT (Computer-Assisted Translation) tool. Translate the following text from %s to %s. Preserve the tags in the text and keep any segmentations intact.\n\n";

    private static final String PARAM_API_KEY = "openai.api.key";
    private static final String PARAM_MODEL = "openai.model";
    private static final String PARAM_TEMPERATURE = "openai.temperature";
    private static final String PARAM_CUSTOM_PROMPT = "custom.prompt";

    private static final String PLUGIN_VERSION = "0.5.2-responses-custom";

    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final String DEFAULT_TEMPERATURE = "0";
    private static final String DEFAULT_CUSTOM_PROMPT = "";

    // Model ids that are not useful for text translation (audio, image,
    // embeddings, moderation, etc.) and would just clutter the dropdown.
    private static final String[] MODEL_ID_EXCLUDE_SUBSTRINGS = {
            "embedding", "whisper", "tts", "transcribe", "realtime", "image",
            "moderation", "audio", "dall-e", "davinci", "babbage", "sora"
    };

    private JTextField apiKeyField;
    private JComboBox<String> modelDropdown;
    private JButton refreshModelsButton;
    private JTextField tempField;
    private JTextArea promptField;

    @Override
    protected String getPreferenceName() {
        return "allow_openai_translate";
    }

    public String getName() {
        if (Preferences.getPreferenceDefault(PARAM_API_KEY, "").isEmpty()) {
            return "OpenAI Translate v" + PLUGIN_VERSION + " (API Key Required)";
        } else {
            return "OpenAI Translate v" + PLUGIN_VERSION;
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

        String currentModel = Preferences.getPreferenceDefault(PARAM_MODEL, DEFAULT_MODEL);
        modelDropdown = new JComboBox<>();
        modelDropdown.setEditable(true); // allow typing a model id manually as a fallback
        modelDropdown.addItem(currentModel);
        modelDropdown.setSelectedItem(currentModel);

        refreshModelsButton = new JButton("Refresh list");
        refreshModelsButton.addActionListener(e -> refreshAvailableModels());

        JPanel modelPanel = new JPanel(new java.awt.BorderLayout(5, 0));
        modelPanel.add(modelDropdown, java.awt.BorderLayout.CENTER);
        modelPanel.add(refreshModelsButton, java.awt.BorderLayout.EAST);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = uiRow;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        configPanel.add(modelPanel, gridBagConstraints);
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
                    Object selectedModel = modelDropdown.getEditor().getItem();
                    Preferences.setPreference(PARAM_MODEL, (Object) (selectedModel == null ? "" : selectedModel.toString().trim()));
                    Preferences.setPreference(PARAM_TEMPERATURE, (Object) tempField.getText());
                    Preferences.setPreference(PARAM_CUSTOM_PROMPT, (Object) promptField.getText());
                } catch (Exception e) {
                    System.err.println("An error occurred while saving preferences: " + e.getMessage());
                }
            }
        };

        dialog.panel.add(configPanel);

        // Try to populate the dropdown with the account's models right away,
        // as long as an API key is already saved.
        if (!apiKeyField.getText().trim().isEmpty()) {
            refreshAvailableModels();
        }

        dialog.show();
    }

    private void refreshAvailableModels() {
        String apiKey = apiKeyField.getText().trim();
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter your API key first, then click \"Refresh list\".",
                    "OpenAI Translate", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String previousSelection = String.valueOf(modelDropdown.getEditor().getItem());
        refreshModelsButton.setEnabled(false);
        refreshModelsButton.setText("Loading...");

        new Thread(() -> {
            List<String> modelIds = null;
            String errorMessage = null;
            try {
                modelIds = fetchAvailableModels(apiKey);
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }
            final List<String> finalModelIds = modelIds;
            final String finalError = errorMessage;
            SwingUtilities.invokeLater(() -> {
                refreshModelsButton.setEnabled(true);
                refreshModelsButton.setText("Refresh list");
                if (finalModelIds != null) {
                    modelDropdown.removeAllItems();
                    for (String id : finalModelIds) {
                        modelDropdown.addItem(id);
                    }
                    if (finalModelIds.contains(previousSelection)) {
                        modelDropdown.setSelectedItem(previousSelection);
                    } else if (!previousSelection.isEmpty() && !"null".equals(previousSelection)) {
                        // keep whatever the user had typed/selected even if it's not in the list
                        modelDropdown.addItem(previousSelection);
                        modelDropdown.setSelectedItem(previousSelection);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Could not fetch model list: " + finalError,
                            "OpenAI Translate", JOptionPane.ERROR_MESSAGE);
                }
            });
        }, "OpenAITranslate-ModelFetch").start();
    }

    private List<String> fetchAvailableModels(String apiKey) throws Exception {
        URL url = new URL(MODELS_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        int status = conn.getResponseCode();
        java.io.InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
        }

        JSONObject jsonResponse = new JSONObject(responseBuilder.toString());
        if (status < 200 || status >= 300) {
            JSONObject error = jsonResponse.optJSONObject("error");
            String message = error != null ? error.optString("message", "HTTP " + status) : "HTTP " + status;
            throw new Exception(message);
        }

        JSONArray data = jsonResponse.getJSONArray("data");
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            String id = data.getJSONObject(i).getString("id");
            if (isTranslationCapableModel(id)) {
                ids.add(id);
            }
        }
        Collections.sort(ids);
        return ids;
    }

    private boolean isTranslationCapableModel(String modelId) {
        String lower = modelId.toLowerCase(Locale.ROOT);
        for (String excluded : MODEL_ID_EXCLUDE_SUBSTRINGS) {
            if (lower.contains(excluded)) {
                return false;
            }
        }
        return true;
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

    /** Small holder for an HTTP status + parsed JSON body pair. */
    private static class ApiResult {
        final int status;
        final JSONObject json;
        ApiResult(int status, JSONObject json) {
            this.status = status;
            this.json = json;
        }
    }

    private String requestTranslation(String systemPrompt, String userPrompt, String apiKey, String model, float temperature) {
        // Responses API: messages go under "input" using the same
        // role/content shape as Chat Completions.
        JSONArray input = new JSONArray();
        input.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        input.put(new JSONObject().put("role", "user").put("content", userPrompt));

        try {
            ApiResult result = postToResponsesApi(apiKey, model, input, temperature);

            if (result.status < 200 || result.status >= 300) {
                JSONObject error = result.json.optJSONObject("error");
                String param = error != null ? error.optString("param", "") : "";
                String message = error != null ? error.optString("message", "HTTP " + result.status) : "HTTP " + result.status;

                // Some models (typically reasoning-tier models) reject the
                // "temperature" parameter entirely. Retry once without it
                // instead of failing outright.
                if ("temperature".equals(param)) {
                    ApiResult retry = postToResponsesApi(apiKey, model, input, null);
                    if (retry.status < 200 || retry.status >= 300) {
                        JSONObject retryError = retry.json.optJSONObject("error");
                        String retryMessage = retryError != null ? retryError.optString("message", "HTTP " + retry.status) : "HTTP " + retry.status;
                        return "Error contacting OpenAI API: " + retryMessage;
                    }
                    return extractOutputText(retry.json);
                }

                return "Error contacting OpenAI API: " + message;
            }

            return extractOutputText(result.json);
        } catch (Exception e) {
            return "Error contacting OpenAI API: " + e.getMessage();
        }
    }

    private String extractOutputText(JSONObject jsonResponse) {
        JSONArray output = jsonResponse.optJSONArray("output");
        if (output != null) {
            for (int i = 0; i < output.length(); i++) {
                JSONObject item = output.getJSONObject(i);
                if (!"message".equals(item.optString("type"))) {
                    continue;
                }
                JSONArray content = item.optJSONArray("content");
                if (content == null) {
                    continue;
                }
                for (int j = 0; j < content.length(); j++) {
                    JSONObject part = content.getJSONObject(j);
                    if ("output_text".equals(part.optString("type"))) {
                        return part.getString("text").trim();
                    }
                }
            }
        }
        return "Translation failed";
    }

    /**
     * POSTs to the Responses API and returns the HTTP status alongside the
     * parsed JSON body (read from the error stream on failure too), since
     * WikiGet.postJSON discards the response body on non-2xx statuses and
     * only exposes a generic "<code>: <reason phrase>" message.
     */
    private ApiResult postToResponsesApi(String apiKey, String model, JSONArray input, Float temperature) throws Exception {
        JSONObject payload = new JSONObject()
                .put("model", model)
                .put("input", input);
        if (temperature != null) {
            payload.put("temperature", temperature);
        }

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        java.io.InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
        }

        return new ApiResult(status, new JSONObject(responseBuilder.toString()));
    }
}
