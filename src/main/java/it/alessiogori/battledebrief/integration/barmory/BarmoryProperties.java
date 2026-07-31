package it.alessiogori.battledebrief.integration.barmory;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration.barmory")
public record BarmoryProperties(String baseUrl, String version) {

    public BarmoryProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank()
                ? "https://barmory.net"
                : baseUrl;
        version = version == null || version.isBlank() ? "8.4" : version;
    }
}
