package com.leyou.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class GlobalCorsConfig {
    @Bean
    public CorsFilter corsFilter(CorsProperties prop) {
        //1.添加CORS配置信息
        CorsConfiguration config = new CorsConfiguration();
        //1) 允许的域,不要写*，否则cookie就无法使用了
        for (String origin : prop.getAllowedOrigins()) {
            config.addAllowedOrigin(origin);
        }
        //2) 是否发送Cookie信息
        config.setAllowCredentials(prop.getAllowCredentials());
        //3) 允许的请求方式
        for (String method : prop.getAllowedMethods()) {
            config.addAllowedMethod(method);
        }
        // 4）允许的头信息
        config.addAllowedHeader("*");
        // 5）配置有效期
        config.setMaxAge(prop.getMaxAge());

        //2.添加映射路径，我们拦截一切请求
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);

        //3.返回新的CorsFilter.
        return new CorsFilter(configSource);
    }
}