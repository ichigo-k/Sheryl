package com.sheryl.slackagent.tools;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.sheryl.slackagent.services.ContextService;
import com.sheryl.slackagent.services.GmailAuthService;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Component
public class GmailTools {

    private final GmailAuthService gmailAuthService;
    private final ContextService sheryl;

    public GmailTools(GmailAuthService gmailAuthService, ContextService sheryl) {
        this.gmailAuthService = gmailAuthService;
        this.sheryl = sheryl;
    }


    @Tool(
            name = "list_gmail_messages",
            description = "List Gmail messages matching a search query (e.g., 'is:unread', 'from:example@gmail.com', 'subject:meeting'). Sends results to user via WhatsApp."
    )
    public List<Map<String, Object>> listMessages(
            @ToolParam(description = "Gmail search query (example: 'is:unread' or 'from:boss@gmail.com')") String query,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {

        Gmail service = gmailAuthService.getGmailService();
        if (query == null || query.isBlank()) query = "is:unread";

        ListMessagesResponse response = service.users().messages()
                .list("me")
                .setQ(query)
                .setMaxResults(10L)
                .execute();

        List<Message> messages = response.getMessages();
        List<Map<String, Object>> result = new ArrayList<>();

        if (messages == null || messages.isEmpty()) {
            String msg = "No messages found for query: " + query;
            sheryl.invoke(notifyNumber, msg);
            return List.of(Map.of("message", msg));
        }

        for (Message msg : messages) {
            Message full = service.users().messages().get("me", msg.getId()).setFormat("full").execute();

            String from = header(full, "From");
            String subject = header(full, "Subject");

            result.add(Map.of(
                    "id", full.getId(),
                    "from", from,
                    "subject", subject,
                    "snippet", full.getSnippet()
            ));
        }

        sheryl.invoke(notifyNumber, "📩 Found " + result.size() + " messages for query: '" + query + "'. Results: " + result);
        return result;
    }


    @Tool(
            name = "send_gmail_message",
            description = "Send an email to someone using Gmail and notify via WhatsApp."
    )
    public String sendEmail(
            @ToolParam(description = "Recipient email address") String to,
            @ToolParam(description = "Email subject") String subject,
            @ToolParam(description = "Email body text") String body,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {

        Gmail service = gmailAuthService.getGmailService();

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress("me"));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(body);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);

        String encodedEmail = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(buffer.toByteArray());

        Message gmailMessage = new Message();
        gmailMessage.setRaw(encodedEmail);

        service.users().messages().send("me", gmailMessage).execute();

        String result = "✅ Email sent to " + to + " with subject: " + subject;
        sheryl.invoke(notifyNumber, result);
        return result;
    }


    @Tool(
            name = "reply_gmail_message",
            description = "Reply to an existing email using its message ID and notify via WhatsApp."
    )
    public String replyToEmail(
            @ToolParam(description = "Message ID to reply to") String messageId,
            @ToolParam(description = "Reply body text") String body,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {

        Gmail service = gmailAuthService.getGmailService();

        Message original = service.users().messages().get("me", messageId).setFormat("full").execute();
        String subject = header(original, "Subject");
        String from = header(original, "From");

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress("me"));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(from));
        email.setSubject("Re: " + subject);
        email.setText(body);
        email.setHeader("In-Reply-To", original.getId());
        email.setHeader("References", original.getId());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        String encodedEmail = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(buffer.toByteArray());

        Message replyMessage = new Message();
        replyMessage.setRaw(encodedEmail);
        replyMessage.setThreadId(original.getThreadId());

        service.users().messages().send("me", replyMessage).execute();

        String result = "↩️ Replied to message with subject: " + subject;
        sheryl.invoke(notifyNumber, result);
        return result;
    }


    @Tool(
            name = "flag_gmail_message",
            description = "Flag a Gmail message (star it) when it requires user attention and notify via WhatsApp."
    )
    public String flagMessage(
            @ToolParam(description = "Message ID to flag") String messageId,
            @ToolParam(description = "Reason for flagging") String reason,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {

        Gmail service = gmailAuthService.getGmailService();
        ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(List.of("STARRED"));
        service.users().messages().modify("me", messageId, mods).execute();

        String result = "🚩 Message " + messageId + " flagged. Reason: " + reason;
        sheryl.invoke(notifyNumber, result);
        return result;
    }


    @Tool(
            name = "delete_gmail_message",
            description = "Delete a Gmail message permanently and notify via WhatsApp."
    )
    public String deleteMessage(
            @ToolParam(description = "Message ID to delete") String messageId,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {

        Gmail service = gmailAuthService.getGmailService();
        service.users().messages().delete("me", messageId).execute();

        String result = "🗑️ Message " + messageId + " deleted successfully.";
        sheryl.invoke(notifyNumber, result);
        return result;
    }


    @Tool(
            name = "mark_gmail_as_read",
            description = "Mark a Gmail message as read and notify via WhatsApp."
    )
    public String markAsRead(
            @ToolParam(description = "Message ID to mark as read") String messageId,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {

        Gmail service = gmailAuthService.getGmailService();
        ModifyMessageRequest mods = new ModifyMessageRequest().setRemoveLabelIds(List.of("UNREAD"));
        service.users().messages().modify("me", messageId, mods).execute();

        String result = "✅ Message " + messageId + " marked as read.";
        sheryl.invoke(notifyNumber, result);
        return result;
    }


    private String header(Message msg, String name) {
        return msg.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(MessagePartHeader::getValue)
                .orElse("Unknown");
    }
}
