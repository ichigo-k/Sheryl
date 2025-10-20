package com.sheryl.slackagent.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class GmailAuthService {

    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = List.of(
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_MODIFY,
            CalendarScopes.CALENDAR,
            CalendarScopes.CALENDAR_EVENTS
    );

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    @Value("${gmail.redirect.uri}")
    private String redirectUri;

    // Gmail service
    public Gmail getGmailService() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(httpTransport);
        return new Gmail.Builder(httpTransport, GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("SlackAgent")
                .build();
    }

    // Calendar service
    public Calendar getCalendarService() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(httpTransport);

        return new Calendar.Builder(httpTransport, GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("SlackAgent")
                .build();
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws Exception {
        InputStream in = GmailAuthService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(), new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, GsonFactory.getDefaultInstance(),
                clientSecrets, new ArrayList<>(SCOPES))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        Credential credential = flow.loadCredential("user");
        if (credential == null || credential.getAccessToken() == null) {
            String authorizationUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .build();
            throw new IllegalStateException(
                    "⚠️ No credentials found. Send user to authorize at: " + authorizationUrl
            );
        }

        return credential;
    }

    // Handle OAuth callback
    public void handleOAuthCallback(String code) throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        InputStream in = GmailAuthService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(), new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, GsonFactory.getDefaultInstance(),
                clientSecrets, new ArrayList<>(SCOPES))
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        flow.createAndStoreCredential(tokenResponse, "user");
        System.out.println("✅ Gmail account authorized successfully.");
    }
}
