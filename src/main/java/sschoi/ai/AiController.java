package sschoi.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    private final ChatClient chatClient;

    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "prompt is required"
            ));
        }

        logger.info("LLM request: {}", prompt);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("prompt", prompt);

        try {
            String fullPrompt = """
                    You are a helpful assistant.
                    Respond in a natural, conversational GPT style.
                    Use clear and concise Korean by default.
                    Only provide code blocks when the user explicitly asks for code.

                    User question:
                    """ + prompt;

            String rawResponse = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();

            logger.info("LLM response (length={}): {}",
                    rawResponse.length(),
                    rawResponse.length() > 500 ? rawResponse.substring(0, 500) + "..." : rawResponse);

            responseMap.put("success", true);
            responseMap.put("answer", rawResponse);
            responseMap.put("parts", List.of(Map.of(
                    "type", "TEXT",
                    "content", rawResponse
            )));

            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            logger.error("Error while calling LLM", e);
            responseMap.put("success", false);
            responseMap.put("error", "Error while processing AI response");
            return ResponseEntity.status(500).body(responseMap);
        }
    }
}
