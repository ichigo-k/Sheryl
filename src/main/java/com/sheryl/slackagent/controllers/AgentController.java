package com.sheryl.slackagent.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sheryl.slackagent.services.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sheryl")
public class AgentController {

    @Autowired
    private AgentService sheryl;


    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("")
    public String ask(@RequestBody String prompt){
        return sheryl.ask(prompt);
    }

    @PostMapping("/whatsapp-listen")
    public void receiveMessage(@RequestParam("From") String from,
                                                 @RequestParam("Body") String body) {
        System.out.println("Message from: " + from + " -> " + body);
        sheryl.ask("Message from: " + from + " -> " + body);
    }

}
