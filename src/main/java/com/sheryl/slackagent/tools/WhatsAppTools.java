package com.sheryl.slackagent.tools;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppTools {

    @Value("${twilio.phone.from}")
    private String fromPhone;

    @Tool(
            name = "whatsapp-message-tool",
            description = "Sends a text message to a user via WhatsApp"
    )
    public void sendMessage(
            @ToolParam(description = "The WhatsApp user's number") String to,
            @ToolParam(description = "The message text to send") String body
    ) {
        Message.creator(
                new PhoneNumber("whatsapp:" + to),
                new PhoneNumber("whatsapp:" + fromPhone),
                body
        ).create();
    }

    @Tool(
            name = "whatsapp-react-tool",
            description = "React to a message with an emoji"
    )
    public void reactToMessage(
            @ToolParam(description = "The WhatsApp user's number") String to,
            @ToolParam(description = "The emoji reaction") String emoji
    ) {
        Message.creator(
                new PhoneNumber("whatsapp:" + to),
                new PhoneNumber("whatsapp:" + fromPhone),
                emoji
        ).create();
    }

    @Tool(
            name = "whatsapp-send-file-tool",
            description = "Send a file, document, or image to the user via WhatsApp"
    )
    public void sendFile(
            @ToolParam(description = "The WhatsApp user's number") String to,
            @ToolParam(description = "The URL of the file to send") String fileUrl,
            @ToolParam(description = "Optional caption for the file") String caption
    ) {


        Message.creator(
                        new PhoneNumber("whatsapp:" + to),
                        new PhoneNumber("whatsapp:" + fromPhone),
                        caption != null ? caption : ""
                ).setMediaUrl(fileUrl)
                .create();
    }
}
