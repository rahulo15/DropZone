package com.dropzone.api.controller;

import org.springframework.stereotype.Controller; // Note: NOT @RestController
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller // This returns HTML pages (Views)
public class ViewController {

    @GetMapping("/f/{id}") // Short URL for "File"
    public String showDownloadPage(@PathVariable String id) {
        return "forward:/download.html";
    }
}