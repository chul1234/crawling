package com.example.crawling.service;

import com.example.crawling.entity.AiSummary;
import com.example.crawling.entity.Product;
import com.example.crawling.repository.AiSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final AiSummaryRepository aiSummaryRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=";

    public void generateAndSaveSummary(List<Product> topProducts) {
        if (topProducts == null || topProducts.isEmpty()) {
            log.warn("요약할 핫딜 상품이 없습니다.");
            return;
        }

        // 프롬프트 구성 (배치 단위 처리로 한도 방어)
        String productInfo = topProducts.stream()
                .map(p -> String.format("- %s (할인율: %d%%, 할인가: %d원)", p.getName(), p.getDiscountRate(), p.getPrice()))
                .collect(Collectors.joining("\n"));

        String prompt = "다음은 오늘 수집된 실시간 특가/중고 핫딜 상품 목록입니다.\n\n"
                + productInfo + "\n\n"
                + "위 상품들을 보고 사용자들의 구매 욕구를 자극할 수 있는 매력적인 브리핑을 3~4줄로 요약해 줘. 친근한 말투를 사용하고, HTML 태그(<strong>, <br> 등)를 적절히 사용해서 꾸며줘.";

        try {
            String url = GEMINI_API_URL + apiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            
            String generatedText = extractTextFromResponse(response);
            
            if (generatedText != null && !generatedText.isEmpty()) {
                AiSummary summary = new AiSummary();
                summary.setContent(generatedText);
                aiSummaryRepository.save(summary);
                log.info("✨ AI 요약이 성공적으로 생성 및 저장되었습니다.");
            }
        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패", e);
        }
        return null;
    }
}
