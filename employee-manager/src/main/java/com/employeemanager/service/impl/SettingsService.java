package com.employeemanager.service.impl;

import com.employeemanager.service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class SettingsService {
    private static final String SETTINGS_FILE = "application.properties";
    private final Properties properties;

    @Value("${application.version}")
    private String applicationVersion;

    @Value("${application.title}")
    private String applicationTitle;
    public SettingsService() {
        this.properties = new Properties();
        loadSettings();
    }

    private void loadSettings() {
        Path settingsPath = Paths.get(SETTINGS_FILE);
        if (Files.exists(settingsPath)) {
            try (InputStream input = Files.newInputStream(settingsPath)) {
                properties.load(input);
            } catch (IOException e) {
                log.error("Error loading settings", e);
                throw new ServiceException("Failed to load settings", e);
            }
        }
    }

    public void saveSettings() {
        try (OutputStream output = Files.newOutputStream(Paths.get(SETTINGS_FILE))) {
            properties.store(output, "Application Settings");
        } catch (IOException e) {
            log.error("Error saving settings", e);
            throw new ServiceException("Failed to save settings", e);
        }
    }

    // Getters
    public String getApplicationVersion() {
        return applicationVersion;
    }

    public String getApplicationTitle() {
        return applicationTitle;
    }

    public String getApplicationAuthor() {
        return properties.getProperty("application.author", "Your Company");
    }

    // Firebase specifikus beállítások
    public String getFirebaseConfigPath() {
        return properties.getProperty("firebase.service-account.path");
    }

    public void setFirebaseConfigPath(String path) {
        properties.setProperty("firebase.service-account.path", path);
        saveSettings();
    }

    public String getDatabaseUrl() {
        return properties.getProperty("spring.datasource.url");
    }

    public String getDatabaseUsername() {
        return properties.getProperty("spring.datasource.username");
    }

    public String getDatabasePassword() {
        return properties.getProperty("spring.datasource.password");
    }

    public void setDatabaseUrl(String url) {
        properties.setProperty("spring.datasource.url", url);
        saveSettings();
    }

    public void setDatabaseUsername(String username) {
        properties.setProperty("spring.datasource.username", username);
        saveSettings();
    }

    public void setDatabasePassword(String password) {
        properties.setProperty("spring.datasource.password", password);
        saveSettings();
    }
}