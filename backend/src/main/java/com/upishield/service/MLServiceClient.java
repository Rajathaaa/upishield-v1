package com.upishield.service;

import com.upishield.dto.MLRequest;
import com.upishield.dto.MLResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MLServiceClient {

    private final RestClient restClient;

    public MLServiceClient(@Value("${ml.service.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public MLResponse predict(MLRequest request) {
        return restClient.post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(MLResponse.class);
    }
}
