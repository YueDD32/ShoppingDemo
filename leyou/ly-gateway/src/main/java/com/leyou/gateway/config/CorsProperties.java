package com.leyou.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "ly.cors")
public class CorsProperties {
    private List<String> allowedOrigins;
    private Boolean allowCredentials;
    private List<String> allowedMethods;
    private Long maxAge;
}
