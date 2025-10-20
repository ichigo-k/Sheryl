package com.sheryl.slackagent.controllers;

import com.sheryl.slackagent.services.GmailAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private final GmailAuthService gmailAuthService;

    public GmailController(GmailAuthService gmailAuthService) {
        this.gmailAuthService = gmailAuthService;
    }

    @GetMapping("/callback")
    public String gmailCallback(@RequestParam("code") String code) {
        try {
            gmailAuthService.handleOAuthCallback(code);
            return "✅ Gmail authorized successfully! You can now close this tab.";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error authorizing Gmail: " + e.getMessage();
        }
    }
}

