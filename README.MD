# Spring Boot + Ollama (Qwen) LLM 연동 예제

Spring Boot에서 **Spring AI**와 **Ollama**를 사용해
로컬 LLM(Qwen 2.5 / Qwen3 계열)을 호출하는 간단한 REST API 예제입니다.

---

## 개발 환경

- Java 17+
- Spring Boot 3.2+
- Spring AI 1.0.0-M2
- Ollama (Local LLM)
- Model 예시
  - `qwen2.5:1.5b`
  - `qwen3-0.6b`

---

## application.yml 예시

```yaml
spring:
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
    cache: false   # 개발 시 필수
  ai:
    openai:
      api-key: 개인 API 키 입력
      chat:
        options:
          model: gpt-4.1-mini
          temperature: 0.7
          max-tokens: 800
```
