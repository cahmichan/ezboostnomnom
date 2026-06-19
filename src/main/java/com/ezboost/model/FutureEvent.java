package com.ezboost.model;

import java.sql.Date;

public class FutureEvent {
    private int eventId;
    private int userId;
    private String eventName;
    private Date eventDate;
    private Date eventEndDate;
    private String eventType;      // PUBLIC_HOLIDAY, SCHOOL_BREAK, CUSTOM
    private String seasonOverride; // PEAK or SUPER_PEAK
    private String source;         // CALENDARIFIC, PRESET, MANUAL
    private boolean active;

    public FutureEvent() {
        this.active = true;
    }

    public FutureEvent(int userId, String eventName, Date eventDate, Date eventEndDate,
                       String eventType, String seasonOverride, String source) {
        this.userId = userId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventEndDate = eventEndDate;
        this.eventType = eventType;
        this.seasonOverride = seasonOverride;
        this.source = source;
        this.active = true;
    }

    // Getters and Setters
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public Date getEventDate() { return eventDate; }
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }

    public Date getEventEndDate() { return eventEndDate; }
    public void setEventEndDate(Date eventEndDate) { this.eventEndDate = eventEndDate; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSeasonOverride() { return seasonOverride; }
    public void setSeasonOverride(String seasonOverride) { this.seasonOverride = seasonOverride; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "FutureEvent{" +
                "eventId=" + eventId +
                ", eventName='" + eventName + '\'' +
                ", eventDate=" + eventDate +
                ", eventEndDate=" + eventEndDate +
                ", eventType='" + eventType + '\'' +
                ", seasonOverride='" + seasonOverride + '\'' +
                ", source='" + source + '\'' +
                ", active=" + active +
                '}';
    }
}
