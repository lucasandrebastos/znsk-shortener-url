package com.example.znsk_shortener_url.service;
import com.example.znsk_shortener_url.repository.UrlMappingRepository;
import com.example.znsk_shortener_url.domain.url.UrlMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class UrlService {

    @Autowired
    private UrlMappingRepository repository;

    private final Random random = new Random();

    public String shortenUrl(String originalUrl, String expires) {
        String code;
        do {
            code = generateCode();
        } while (repository.existsById(code));

        UrlMapping link = new UrlMapping();
        link.setCode(code);
        link.setOriginalUrl(originalUrl);
        link.setCreatedAt(LocalDateTime.now());

        switch (expires){
            case "1h":
                link.setExpiresAt(LocalDateTime.now().plusHours(1));
                break ;
            case "1d":
                link.setExpiresAt(LocalDateTime.now().plusHours(24));
                break;
            case "7d":
                link.setExpiresAt(LocalDateTime.now().plusHours(168));
                break;
            case "30d":
                link.setExpiresAt(LocalDateTime.now().plusHours(720));
                break;
        }


        repository.save(link);

        return "https://znskshortener.up.railway.app/" + code;
    }

    public String getOriginalUrl(String code) {
        return repository.findById(code).map(UrlMapping::getOriginalUrl).orElse(null);
    }

    public boolean isExpired(String code){

        LocalDateTime expiresAt = repository.findById(code).map(UrlMapping::getExpiresAt).orElse(null);
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());

    }

    public void deleteById(String code){
       UrlMapping url = repository.getReferenceById(code);
       repository.delete(url);

    }

    public void deleteExpiredLinks() {
        var expiredLinks = repository.findAllByExpiresAtBefore(LocalDateTime.now());
        repository.deleteAll(expiredLinks);
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder();
        do {
            String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            code.append(BASE62.charAt(random.nextInt(BASE62.length())));
        } while (code.length() < 6);

        return code.toString();
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanExpiredUrls() {
        deleteExpiredLinks();
    }



}
