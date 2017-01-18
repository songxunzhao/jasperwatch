package com.artemis.speechcmu.models;

public class HubResponse {
    public String text;
    public Boolean result;
    public HubResponse(String p_text, Boolean p_result) {
        text = p_text;
        result = p_result;
    }
}
