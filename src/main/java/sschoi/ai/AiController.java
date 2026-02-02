package sschoi.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.*;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    private final ChatClient chatClient;

    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // ---------------------------
    // 질문 → LLM → TEXT/CODE 분리 → JSON 반환
    // ---------------------------
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestBody Map<String, Object> request) {

        // 1. 질문
        String prompt = (String) request.get("prompt");
        if (prompt == null || prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "prompt 값이 필요합니다."
            ));
        }

        // 2. 옵션
        Map<String, Object> options = (Map<String, Object>) request.getOrDefault("options", Map.of());
        String responseFormat = (String) options.getOrDefault("responseFormat", "text");

        // 3. 구조 (TEXT / CODE 안내)
        List<Map<String, Object>> structure = (List<Map<String, Object>>) request.getOrDefault("structure", List.of());

        logger.info("LLM 요청: {}", prompt);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("prompt", prompt);

        try {
            // 4. 프롬프트 보강: 구조 안내
            String fullPrompt = prompt;
            if (!structure.isEmpty()) {
                fullPrompt += "\n출력 형식 안내:\n";
                for (Map<String, Object> s : structure) {
                    fullPrompt += "- type: " + s.get("type") + ", description: " + s.get("description") + "\n";
                }
            }

            // 5. LLM 호출
            String rawResponse = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();

            // 로그 안전하게 출력 (최대 500자)
            logger.info("LLM 원본 응답 (length={}): {}",
                    rawResponse.length(),
                    rawResponse.length() > 500 ? rawResponse.substring(0, 500) + "..." : rawResponse);

            // 6. Markdown 코드 블록 기준 TEXT / CODE 분리
            List<Map<String, Object>> parts = parseMarkdownResponse(rawResponse);

            responseMap.put("success", true);
            responseMap.put("parts", parts);

            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            logger.error("LLM 호출 중 오류", e);
            responseMap.put("success", false);
            responseMap.put("error", "AI 처리 중 오류 발생");
            return ResponseEntity.status(500).body(responseMap);
        }
    }

    // ---------------------------
    // Markdown 코드 블록 파서
    // ---------------------------
    private List<Map<String, Object>> parseMarkdownResponse(String response) {
        List<Map<String, Object>> parts = new ArrayList<>();

        Pattern pattern = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(response);

        int lastIndex = 0;
        while (matcher.find()) {
            // 코드 앞 일반 텍스트
            if (matcher.start() > lastIndex) {
                String text = response.substring(lastIndex, matcher.start()).trim();
                if (!text.isEmpty()) {
                    parts.add(Map.of(
                            "type", "TEXT",
                            "content", text
                    ));
                }
            }

            // 코드 블록
            Map<String, Object> codePart = new HashMap<>();
            codePart.put("type", "CODE");
            codePart.put("language", matcher.group(1)); // java, python 등
            codePart.put("content", matcher.group(2).trim());
            parts.add(codePart);

            lastIndex = matcher.end();
        }

        // 마지막 텍스트
        if (lastIndex < response.length()) {
            String text = response.substring(lastIndex).trim();
            if (!text.isEmpty()) {
                parts.add(Map.of(
                        "type", "TEXT",
                        "content", text
                ));
            }
        }

        return parts;
    }
}
