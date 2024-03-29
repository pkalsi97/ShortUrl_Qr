package com.pk.MyShortUrl.controller;

import com.pk.MyShortUrl.dto.CustomUrlRequest;
import com.pk.MyShortUrl.dto.DisableUrlRequest;
import com.pk.MyShortUrl.dto.ShortURLDto;
import com.pk.MyShortUrl.dto.ShortUrlRequest;
import com.pk.MyShortUrl.model.ShortURL;
import com.pk.MyShortUrl.service.ShortURLService;
import com.pk.MyShortUrl.service.webriskSearchUri;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/url")
public class RestfulController {

    private final ShortURLService shortURLService;
    private final webriskSearchUri webRiskSearchUri;

    @Autowired
    public RestfulController(ShortURLService shortURLService, webriskSearchUri webRiskSearchUri) {
        this.shortURLService = shortURLService;
        this.webRiskSearchUri = webRiskSearchUri;
    }
    // Get request to display all active urls for user
    @GetMapping("/active")
    public ResponseEntity<List<ShortURLDto>> getActiveURLsForUser(Principal principal) {

        String userId = principal.getName();
        List<ShortURL> activeURLs = shortURLService.getActiveShortURLsByUser(userId);
        // Convert each ShortURL to a ShortURLDto
        List<ShortURLDto> activeUrlsDto = activeURLs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(activeUrlsDto);
    }

    // Get request to display all inactive urls for user
    @GetMapping("/inactive")
    public ResponseEntity<List<ShortURLDto>> getInactiveURLsForUser(Principal principal) {
        String userId = principal.getName();
        List<ShortURL> allURLs = shortURLService.getAllShortURLsByUser(userId);

        List<ShortURLDto> inactiveUrlsDto = allURLs.stream()
                .filter(shortURL -> !shortURL.isActive()) // Filter for inactive URLs
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(inactiveUrlsDto);
    }
    // Post request to generate a random url
    @PostMapping("/generateRandom")
    public ResponseEntity<ShortURLDto> generateRandomShortUrl(@RequestBody ShortUrlRequest request, Principal principal) {
        String originalUrl = request.getOriginalUrl();
        String userId = principal.getName();
        String backHalf = shortURLService.generateUniqueBackHalf();
        ShortURL shortURL = shortURLService.createShortURL(request.getOriginalUrl(), backHalf, userId);

        if (shortURL != null) {
            return ResponseEntity.ok(convertToDto(shortURL));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    // Post request to generate a custom Short url
    @PostMapping("/generateCustom")
    public ResponseEntity<?> generateCustomShortUrl(@RequestBody CustomUrlRequest request, Principal principal) {
        String userId = principal.getName();
        try {
            ShortURL shortURL = shortURLService.createShortURL(request.getOriginalUrl(), request.getBackHalf(), userId);
            return ResponseEntity.ok(convertToDto(shortURL));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating the short URL.");
        }
    }

    // Get request to check the safety of an Original URL
    @GetMapping("/checkSafety")
    public ResponseEntity<String> checkUrlSafety(@RequestParam String url) {
        try {
            boolean isSafe = webriskSearchUri.searchUri(url);
            if (isSafe) {
                return ResponseEntity.ok("URL is safe.");
            } else {
                return ResponseEntity.ok("URL is not safe.");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred while checking URL safety.");
        }
    }

    // Post request  to disable an active url
    @PostMapping("/disableUrl")
    public ResponseEntity<?> disableUrl(@RequestBody DisableUrlRequest request, Principal principal) {
        String userId = principal.getName();

        boolean isDisabled = shortURLService.deactivateUrlByShortLink(request.getShortUrl(), userId);
        if (isDisabled) {
            return ResponseEntity.ok(Map.of("success", true, "message", "URL successfully disabled"));
        } else {
            return ResponseEntity.ok(Map.of("success", false, "message", "URL not found or already inactive"));
        }
    }


    // used to get data safely for Get Request made by the user
    private ShortURLDto convertToDto(ShortURL shortURL) {
        ShortURLDto dto = new ShortURLDto();
        dto.setOriginalUrl(shortURL.getOriginalUrl());
        dto.setShortLink(shortURL.getShortLink());
        dto.setCreationDate(shortURL.getCreationDate().toString());
        dto.setExpirationDate(shortURL.getExpirationDate().toString());
        dto.setActive(shortURL.isActive());
        dto.setUserId(shortURL.getUserId());
        dto.setClickCount(shortURL.getClickCount());

        return dto;
    }
}
