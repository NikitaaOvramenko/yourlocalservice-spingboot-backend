package com.nikita_ovramenko.sping_all_purpose_server.email;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/email/form")
    public ResponseEntity<EmailDto> postMethodName(@RequestBody FormDto formDto) {

        EmailDto emailDto = emailService.sendFormResponseToClient(formDto);

        return ResponseEntity.ok(emailDto);
    }

}
