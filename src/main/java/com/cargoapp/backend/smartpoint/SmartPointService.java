package com.cargoapp.backend.smartpoint;

import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SmartPointService {

    private final SmartPointProperties properties;
    private final UserRepository userRepository;
    private final RestClient restClient;

    public SmartPointService(SmartPointProperties properties, UserRepository userRepository) {
        this.properties = properties;
        this.userRepository = userRepository;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public void tryUpsertClient(UserEntity user) {
        if (properties.apiToken() == null || properties.apiToken().isBlank()) {
            log.warn("SmartPoint API token not configured, skipping upsert for user {}", user.getId());
            return;
        }
        try {
            Map<String, String> body = Map.of(
                    "client_code", user.getPersonalCode(),
                    "phone", user.getPhone(),
                    "first_name", user.getFirstName(),
                    "last_name", user.getLastName()
            );
            restClient.post()
                    .uri(properties.apiUrl() + "/clients/upsert")
                    .header("Authorization", "Bearer " + properties.apiToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            user.setSmartpointSyncedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("SmartPoint upsert success: userId={} personalCode={}", user.getId(), user.getPersonalCode());
        } catch (Exception e) {
            log.warn("SmartPoint upsert failed: userId={} personalCode={} error={}",
                    user.getId(), user.getPersonalCode(), e.getMessage());
        }
    }

    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional
    public void retrySyncPendingUsers() {
        List<UserEntity> users = userRepository.findAllConfirmedAndNotSynced();
        if (users.isEmpty()) return;
        log.info("SmartPoint retry cron: {} users pending sync", users.size());
        for (UserEntity user : users) {
            tryUpsertClient(user);
        }
    }
}