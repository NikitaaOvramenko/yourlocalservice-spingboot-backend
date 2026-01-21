package com.nikita_ovramenko.sping_all_purpose_server.file;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PutMapping("/upload/{name}")
    public ResponseEntity<String> upload(@PathVariable String name, @RequestBody String entity) {

        String url = fileService.createPresignedUrl(name, null);

        return ResponseEntity.ok(url);
    }

}
