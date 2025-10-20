package com.sheryl.slackagent.tools;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.sheryl.slackagent.services.ContextService;
import com.sheryl.slackagent.services.GmailAuthService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CalendarTools {

    private final GmailAuthService gmailAuthService;
    private final ContextService sheryl;

    @Autowired
    public CalendarTools(GmailAuthService gmailAuthService, ContextService sheryl) {
        this.gmailAuthService = gmailAuthService;
        this.sheryl = sheryl;
    }

    private Calendar getService() throws Exception {
        return gmailAuthService.getCalendarService();
    }

    @Tool(name = "list_calendar_events", description = "List upcoming events or meetings and Sends a text message to a user via WhatsApp")
    public List<Map<String, String>> listEvents(
            @ToolParam(description = "Maximum number of events to return") Integer maxResults,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {
        Calendar service = getService();
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = service.events().list("primary")
                .setMaxResults(maxResults != null ? maxResults : 10)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<Map<String, String>> result = new ArrayList<>();
        for (Event event : events.getItems()) {
            Map<String, String> e = new HashMap<>();
            e.put("id", event.getId());
            e.put("summary", event.getSummary());
            e.put("start", event.getStart().getDateTime() != null ?
                    event.getStart().getDateTime().toStringRfc3339() :
                    event.getStart().getDate().toString());
            e.put("end", event.getEnd().getDateTime() != null ?
                    event.getEnd().getDateTime().toStringRfc3339() :
                    event.getEnd().getDate().toString());
            result.add(e);
        }

        sheryl.invoke(notifyNumber, "Listed " + result.size() + " upcoming events. Results are: " + result);
        return result;
    }

    @Tool(name = "create_calendar_event", description = "Create a new calendar event and Sends a text message to a user via WhatsApp")
    public Map<String, String> createEvent(
            @ToolParam(description = "Event title") String summary,
            @ToolParam(description = "Event description") String description,
            @ToolParam(description = "Start time in RFC3339 format") String startDateTime,
            @ToolParam(description = "End time in RFC3339 format") String endDateTime,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {
        Calendar service = getService();

        Event event = new Event()
                .setSummary(summary)
                .setDescription(description)
                .setStart(new EventDateTime().setDateTime(new DateTime(startDateTime)))
                .setEnd(new EventDateTime().setDateTime(new DateTime(endDateTime)));

        Event createdEvent = service.events().insert("primary", event).execute();
        String link = createdEvent.getHtmlLink();

        sheryl.invoke(notifyNumber, "Created new event: " + summary + " — " + link);

        Map<String, String> result = new HashMap<>();
        result.put("id", createdEvent.getId());
        result.put("summary", createdEvent.getSummary());
        result.put("link", link);
        return result;
    }

    @Tool(name = "delete_calendar_event", description = "Delete an event by ID and Sends a text message to a user via WhatsApp")
    public String deleteEvent(
            @ToolParam(description = "Event ID to delete") String eventId,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {
        Calendar service = getService();
        service.events().delete("primary", eventId).execute();

        String msg = "Deleted calendar event with ID: " + eventId;
        sheryl.invoke(notifyNumber, msg);
        return msg;
    }

    @Tool(name = "update_calendar_event", description = "Update an existing event and Sends a text message to a user via WhatsApp")
    public Map<String, String> updateEvent(
            @ToolParam(description = "Event ID") String eventId,
            @ToolParam(description = "New event summary") String summary,
            @ToolParam(description = "New event description") String description,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {
        Calendar service = getService();
        Event event = service.events().get("primary", eventId).execute();

        if (summary != null) event.setSummary(summary);
        if (description != null) event.setDescription(description);

        Event updated = service.events().update("primary", eventId, event).execute();

        sheryl.invoke(notifyNumber, "Calendar event updated: " + updated.getSummary() + "\n" + updated.getHtmlLink());

        Map<String, String> result = new HashMap<>();
        result.put("id", updated.getId());
        result.put("summary", updated.getSummary());
        result.put("link", updated.getHtmlLink());
        return result;
    }

    @Tool(name = "search_calendar_events_advanced", description = "Search events by keyword or attendee and Sends a text message to a user via WhatsApp")
    public List<Map<String, String>> searchEventsAdvanced(
            @ToolParam(description = "Keyword to search in title or description") String keyword,
            @ToolParam(description = "Attendee email (optional)") String attendeeEmail,
            @ToolParam(description = "Maximum number of results") Integer maxResults,
            @ToolParam(description = "The WhatsApp user's number") String notifyNumber
    ) throws Exception {
        Calendar service = gmailAuthService.getCalendarService();
        Calendar.Events.List request = service.events().list("primary")
                .setMaxResults(maxResults != null ? maxResults : 20)
                .setOrderBy("startTime")
                .setSingleEvents(true);

        Events events = request.execute();
        List<Map<String, String>> results = new ArrayList<>();

        for (Event event : events.getItems()) {
            boolean matchesKeyword = keyword == null || keyword.isBlank() ||
                    (event.getSummary() != null && event.getSummary().toLowerCase().contains(keyword.toLowerCase())) ||
                    (event.getDescription() != null && event.getDescription().toLowerCase().contains(keyword.toLowerCase()));

            boolean matchesAttendee = attendeeEmail == null || attendeeEmail.isBlank() ||
                    (event.getAttendees() != null && event.getAttendees().stream()
                            .anyMatch(a -> attendeeEmail.equalsIgnoreCase(a.getEmail())));

            if (matchesKeyword && matchesAttendee) {
                Map<String, String> e = new HashMap<>();
                e.put("id", event.getId());
                e.put("summary", event.getSummary());
                e.put("description", event.getDescription() != null ? event.getDescription() : "");
                e.put("start", event.getStart().getDateTime() != null ?
                        event.getStart().getDateTime().toStringRfc3339() :
                        event.getStart().getDate().toString());
                e.put("end", event.getEnd().getDateTime() != null ?
                        event.getEnd().getDateTime().toStringRfc3339() :
                        event.getEnd().getDate().toString());
                results.add(e);
            }
        }

        sheryl.invoke(notifyNumber, "Searched for events with keyword '" + keyword + "' and found " + results.size() + " matches. Results are: " + results);
        return results;
    }
}
