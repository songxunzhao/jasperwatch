package com.artemis.speechcmu.models;

public class NotifyRequest extends HubRequest {
    private String event;
    private Location location;
    public void setEvent(String p_event) {
        event = p_event;
    }
    public void setLocation(Location p_loc) {
        location = p_loc;
    }
}
