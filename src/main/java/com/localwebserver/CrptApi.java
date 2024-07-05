package com.localwebserver;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrptApi {
    private final AtomicInteger availableTokens;
    private final int maxTokens;
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String API_URL = "http://localhost:8080/documents/create";
    private static final Logger logger = LoggerFactory.getLogger(CrptApi.class);

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Request limit must be positive");
        }
        this.maxTokens = requestLimit;
        this.availableTokens = new AtomicInteger(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        long period = timeUnit.toMillis(1);
        scheduler.scheduleAtFixedRate(this::refillTokens, period, period, TimeUnit.MILLISECONDS);
    }

    private void refillTokens() {
        availableTokens.set(maxTokens);
    }

    public void createDocument(Document document, String signature) throws Exception {
        while (true) {
            if (availableTokens.get() > 0 && availableTokens.decrementAndGet() >= 0) {
                try {
                    logger.info("Creating document with ID: {}", document.doc_id);
                    String jsonBody = objectMapper.writeValueAsString(document);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(API_URL))
                            .header("Content-Type", "application/json")
                            .header("Signature", signature)
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Response status code: " + response.statusCode());
                    System.out.println("Response body: " + response.body());
                    return;
                } catch (Exception e) {
                    logger.error("Error creating document", e);
                    throw e;
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for available tokens", e);
                }
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Document {
        public Description description;
        @EqualsAndHashCode.Include
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;
    }

    @Getter
    @Setter
    @ToString
    public static class Description {
        public String participantInn;
    }

    @Getter
    @Setter
    @ToString
    public static class Product {
        public String certificate_document;
        public String certificate_document_date;
        public String certificate_document_number;
        public String owner_inn;
        public String producer_inn;
        public String production_date;
        public String tnved_code;
        public String uit_code;
        public String uitu_code;
    }
}