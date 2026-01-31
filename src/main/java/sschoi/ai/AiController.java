package sschoi.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {
	
	private static final Logger logger = LoggerFactory.getLogger(AiController.class);
	
    private final ChatClient chatClient;

    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    
    @GetMapping("/test")
    public String test(@RequestParam("prompt") String prompt) {
    	logger.info("LLM 요청: {}", prompt);
    	
    	String response;
         try {
             response = chatClient.prompt()
                     .user(prompt)
                     .call()
                     .content();
             logger.info("LLM 응답: {}", response);
         } catch (Exception e) {
             logger.error("LLM 호출 중 오류", e);
             response = "AI 처리 중 오류 발생";
         }

         return response;
    }
}
