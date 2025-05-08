package com.example.znsk_shortener_url.repository;

import com.example.znsk_shortener_url.domain.url.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, String> {
    List<UrlMapping> findAllByExpiresAtBefore(LocalDateTime time);
}
