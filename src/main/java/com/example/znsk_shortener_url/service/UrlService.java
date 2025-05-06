package com.example.znsk_shortener_url.service;

import com.example.znsk_shortener_url.repository.UrlMappingRepository;
import com.example.znsk_shortener_url.domain.url.UrlMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class UrlService {

    @Autowired
    private UrlMappingRepository repository;

    private final Random random = new Random();


    public String shortenUrl(String originalUrl) {
        String code;
        do {
            code = generateCode();
        } while (repository.existsById(code));

        UrlMapping link = new UrlMapping();
        link.setCode(code);
        link.setOriginalUrl(originalUrl);
        repository.save(link);

        return "http://localhost:8080/" + code;
    }

    public String getOriginalUrl(String code) {
        return repository.findById(code).map(UrlMapping::getOriginalUrl).orElse(null);
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder();
        do {
            String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            code.append(BASE62.charAt(random.nextInt(BASE62.length())));
        } while (code.length() < 6);

        return code.toString();
    }

}
