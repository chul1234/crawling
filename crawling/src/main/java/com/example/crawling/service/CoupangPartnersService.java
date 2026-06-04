package com.example.crawling.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
@Slf4j
public class CoupangPartnersService {

    @Value("${coupang.partners.access-key:dummy}")
    private String accessKey;

    @Value("${coupang.partners.secret-key:dummy}")
    private String secretKey;

    private static final String API_URL = "https://api-gateway.coupang.com/v2/providers/affiliate_open_api/apis/openapi/v1/deeplink";

    public String generateDeeplink(String originalUrl) {
        if ("dummy".equals(accessKey) || "dummy".equals(secretKey)) {
            log.info("쿠팡 파트너스 API 키가 설정되지 않아 원본 URL을 반환합니다. (추후 발급 시 자동 변환됨)");
            return originalUrl; // 발급 전이면 그대로 원본 반환
        }

        try {
            String method = "POST";
            String path = "/v2/providers/affiliate_open_api/apis/openapi/v1/deeplink";
            
            // 1. Authorization 헤더 생성
            String datetime = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyMMdd'T'HHmmss'Z'"));
            String message = datetime + method + path;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder(signatureBytes.length * 2);
            for(byte b: signatureBytes) {
                sb.append(String.format("%02x", b));
            }
            String signature = sb.toString();
            
            String authorization = String.format("CEA algorithm=HmacSHA256, access-key=%s, signed-date=%s, signature=%s", accessKey, datetime, signature);

            // 2. API 호출
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorization);
            headers.set("Content-Type", "application/json");

            Map<String, Object> requestBody = Map.of("coupangUrls", new String[]{originalUrl});
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                java.util.List<Map<String, String>> data = (java.util.List<Map<String, String>>) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    return data.get(0).get("shortenUrl");
                }
            }
            
        } catch (Exception e) {
            log.error("쿠팡 파트너스 API 연동 중 오류 발생: {}", e.getMessage());
        }
        
        return originalUrl; // 실패 시 원본 URL 반환
    }
}
