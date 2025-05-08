package com.example.znsk_shortener_url.controller;

import com.example.znsk_shortener_url.dtos.RequestDto;
import com.example.znsk_shortener_url.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@CrossOrigin(origins = "http://localhost:5173", methods = {RequestMethod.GET, RequestMethod.POST})
public class UrlController {

    @Autowired
    private UrlService urlService;

    @PostMapping("/shorten")
    public ResponseEntity<String> shorten(@RequestBody RequestDto requestDto) {
        String shortUrl = urlService.shortenUrl(requestDto.originalUrl(), requestDto.expireInHours());
        return ResponseEntity.ok(shortUrl);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {

        String originalUrl = urlService.getOriginalUrl(code);
        boolean isExpired = urlService.isExpired(code);

        if (originalUrl != null) {
            if(isExpired){
                urlService.deleteById(code);
                return ResponseEntity.status(HttpStatus.GONE).build();
            }


            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(originalUrl))
                    .build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
