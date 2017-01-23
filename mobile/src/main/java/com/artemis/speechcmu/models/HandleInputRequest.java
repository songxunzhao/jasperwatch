package com.artemis.speechcmu.models;

/**
 * Created by song on 1/20/2017.
 */

public class HandleInputRequest extends HubRequest{
    private String text;
    public void setText(String p_str) {
        text = p_str;
    }
}
