package com.prpa.Shortz.config;

import com.prpa.Shortz.model.shortener.DigestToBase64StringEncoder;
import com.prpa.Shortz.model.shortener.EncodeRandomUrlShortener;
import com.prpa.Shortz.model.shortener.contract.UrlShortener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

@Configuration
public class UrlShortenerConfig {

    @Value("${default.urlshortener.dictionary}")
    private String DICTIONARY;

    @Value("${initial.urlshortener.window.size}")
    private Integer WINDOW_SIZE;

    @Value("${default.urlshortener.supported.protocols}")
    private Set<String> SUPPORTED_PROTOCOLS;

    @Bean
    public UrlShortener urlShortener() {
        return new EncodeRandomUrlShortener(new DigestToBase64StringEncoder(), WINDOW_SIZE, DICTIONARY, new Random(), SUPPORTED_PROTOCOLS);
    }

    public Set<String> getSupportedProtocols() {
        return Collections.unmodifiableSet(SUPPORTED_PROTOCOLS);
    }

}
