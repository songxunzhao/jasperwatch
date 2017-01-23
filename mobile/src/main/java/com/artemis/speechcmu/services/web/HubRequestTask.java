package com.artemis.speechcmu.services.web;

import com.artemis.speechcmu.models.HubRequest;
import com.artemis.speechcmu.models.HubResponse;
import com.google.gson.Gson;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HubRequestTask extends AsyncTask<Object, Object, Object>{
    private OnTaskCompleted listener;
    private String apiPath;
    private final String BASE_URL = "http://artemis-hub.ngrok.io";
    public static final String API_HANDLEINPUT = "/jasper/handleinput";
    public static final String API_NOTIFY      = "/jasper/notify";

    // Constructor
    public HubRequestTask(String p_api, OnTaskCompleted p_listener) {
        apiPath = BASE_URL + p_api;
        listener = p_listener;
    }

    private void writePostBody(HttpURLConnection conn, HubRequest data) throws IOException{
        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        //TODO: replace with data
        Gson gson = new Gson();
        writer.write(gson.toJson(data));
        writer.flush();
        writer.close();
        os.close();
    }

    private Object readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();

        String line = null;
        while ((line = reader.readLine()) != null)
        {
            stringBuilder.append(line + "\n");
        }

        Gson gson = new Gson();
        return gson.fromJson(stringBuilder.toString(), HubResponse.class);
    }

    @Override
    protected Object doInBackground(Object[] params) {
        URL url;
        try{
            url = new URL(apiPath);
        }
        catch (MalformedURLException exception) {
            return null;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            //TODO: replace with data
            writePostBody(conn, (HubRequest)params[0]);
            conn.connect();
            return readResponse(conn);
        }
        catch (IOException exception) {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Object o) {
        listener.onTaskCompleted(o);
    }
}
