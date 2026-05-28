package com.englishplatform.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves index.html for any /voice-rooms/** path that does not resolve to
 * a static asset. This enables Vue Router HTML5 history mode for the
 * standalone voice-rooms SPA.
 */
@RestController
public class VoiceRoomsSpaController {

    /**
     * Catch-all for /voice-rooms/room/{id} and other deep links.
     * Static assets (JS, CSS) are handled by Spring's static resource resolver
     * BEFORE reaching this controller.
     */
    @GetMapping(value = "/voice-rooms/{path:[^\\.]*}")
    public ResponseEntity<Resource> voiceRoomsFallback() {
        Resource index = new ClassPathResource("static/voice-rooms/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(index);
    }
}
