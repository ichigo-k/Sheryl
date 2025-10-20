package com.sheryl.slackagent.services;


import com.sheryl.slackagent.tools.WhatsAppTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ContextService {

    private final ChatClient chatClient;
    private final WhatsAppTools whatsAppTools;


    public ContextService(ChatClient.Builder builder, WhatsAppTools whatsAppTools) {
        this.chatClient = builder.build();
        this.whatsAppTools = whatsAppTools;

    }

    public void invoke(String notifyNumber, String results) {
        String response = chatClient
                .prompt()
                .user(results)
                .system("""
                You are *Sheryl*, the AI Virtual Assistant. The following actions were executed successfully,
                and you now have their results. Use these results to continue the conversation naturally —
                confirm completion, ask clarifying questions, or follow up with the next logical step.

                Keep tone friendly and conversational (like WhatsApp).
                Use the appropriate tools again if follow-up actions are needed.
                
                The prompt is coming from you so think of it more like your inner thought okay 
                As it is the output of  an operation that a user wnated so do not be like thanks for doing this 
                Just convery the output neatly and nicely basically format the output well for the user 
             
                """)
                .call()
                .content();

        whatsAppTools.sendMessage(notifyNumber, response);
    }

}
