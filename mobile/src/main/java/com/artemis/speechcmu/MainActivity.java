package com.artemis.speechcmu;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;

import com.artemis.speechcmu.models.HubRequest;
import com.artemis.speechcmu.models.HubResponse;
import com.artemis.speechcmu.services.HandleResultTask;
import com.artemis.speechcmu.services.OnTaskCompleted;
import com.artemis.speechcmu.services.PostureDetectionService;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends AppCompatActivity {

    private static final String KWS_SEARCH = "artemis";
    private static final String KEYPHRASE = "artemis";
    private static final String HOST_ADDRESS = "";

    private edu.cmu.pocketsphinx.SpeechRecognizer sphinxRecognizer;
    private android.speech.SpeechRecognizer googleRecognizer;
    private Intent recognizerIntent;

    TextToSpeech ttsEngine;
    private DonutProgress progressView;
    PostureDetectionService postureService;

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkRecordPermission();

        progressView = (DonutProgress) findViewById(R.id.donut_progress);

        postureService = new PostureDetectionService(this);

        setupRecognizer();
        createGoogleRecognizer();
        createGoogleTTS();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void checkRecordPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }

    private void createGoogleRecognizer() {
        googleRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
        googleRecognizer.setRecognitionListener(new GoogleSpeechListener());
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    private void createGoogleTTS() {
        ttsEngine=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    ttsEngine.setLanguage(Locale.US);
                }
            }
        });
    }

    private void setupRecognizer() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        progressView.setProgress(0);
        progressView.setFinishedStrokeColor(Color.RED);

        new AsyncTask<Void, Integer, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);

                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                progressView.setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {

                } else {
                    progressView.setUnfinishedStrokeColor(Color.RED);
                    progressView.setProgress(0);
                    progressView.setTextColor(Color.TRANSPARENT);
                    MainActivity.this.switchSearch(KWS_SEARCH);
                }
            }

            protected void setupRecognizer(File assetsDir) throws IOException {
                // The recognizer can be configured to perform multiple searches
                // of different kind and switch between them

                sphinxRecognizer = SpeechRecognizerSetup.defaultSetup()
                        .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                        .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                        .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                        .getRecognizer();
                sphinxRecognizer.addListener(new SphinxSpeechListener());
                publishProgress(30);
                delayForSecond();
                /** In your application you might not need to add all those searches.
                 * They are added here for demonstration. You can leave just one.
                 */
                sphinxRecognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
                publishProgress(100);
                delayForSecond();
            }

            protected void delayForSecond() {
                SystemClock.sleep(1000);
            }
        }.execute();
    }

    private void switchSearch(String searchName) {
        sphinxRecognizer.stop();

        sphinxRecognizer.startListening(searchName);
    }

    private void sendTextToJasper(String text) {
        HubRequest model = new HubRequest();
        model.setText(text);
        HandleResultTask task = new HandleResultTask(new OnTaskCompleted() {
            @Override
            public void onTaskCompleted(Object result) {
            speakToUser((HubResponse) result);
            }
        });
        task.execute(model);
    }

    private void speakToUser(HubResponse response) {
        if(response != null && response.result) {
            ttsEngine.speak(response.text, TextToSpeech.QUEUE_FLUSH, null);
        } else {
            Toast.makeText(this, "Communication error with Jasper channel", Toast.LENGTH_SHORT).show();
        }
    }

    private static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case android.speech.SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case android.speech.SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case android.speech.SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case android.speech.SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case android.speech.SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    class SphinxSpeechListener implements edu.cmu.pocketsphinx.RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onEndOfSpeech() {
            switchSearch(KWS_SEARCH);
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
            if (hypothesis == null)
                return;

            String text = hypothesis.getHypstr();
            if (text.equals(KEYPHRASE)) {
                // Start google listening
                Toast.makeText(MainActivity.this, KEYPHRASE, Toast.LENGTH_SHORT).show();

                googleRecognizer.stopListening();
                googleRecognizer.startListening(recognizerIntent);
            }
            Toast.makeText(MainActivity.this, "OnResult: " + KEYPHRASE, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(Exception e) {

        }

        @Override
        public void onTimeout()
        {
            switchSearch(KWS_SEARCH);
        }
    }

    class GoogleSpeechListener implements android.speech.RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            googleRecognizer.stopListening();
        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results
                    .getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
            String text = "";
            if(!matches.isEmpty()) {
                text = matches.get(0);
                // TODO: comment out for production
                sendTextToJasper(text);
//                speakToUser(new HubResponse(text, true));

            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
