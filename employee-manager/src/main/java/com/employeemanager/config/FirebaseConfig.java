package com.employeemanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.text.SimpleDateFormat;

@Configuration
public class FirebaseConfig {
    /*

    @Value("${firebase.service-account.path}")
    private String serviceAccountPath;
    
    @Value("${firebase.database.url}")
    private String databaseUrl;
    
    @Value("${firebase.project.id}")
    private String projectId;
    private final ResourceLoader resourceLoader;
    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    @Bean
    public Firestore firestore() throws IOException {
        try {
            Resource serviceAccount = resourceLoader.getResource(serviceAccountPath);

            if (!serviceAccount.exists()) {
                throw new IOException("Firebase service account file not found: " + serviceAccountPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                .setProjectId(projectId)
                .setDatabaseUrl(databaseUrl)
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            return FirestoreClient.getFirestore();
        } catch (IOException e) {
            throw new IOException("Failed to initialize Firebase: " + e.getMessage(), e);
        }
    }

    */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        return mapper;
    }
}