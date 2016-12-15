package com.jason.speechcmu;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;


public class MainActivity extends Activity implements RecognitionListener{

    private TextView mTextView;
    private SpeechRecognizer recognizer;
    private DonutProgress progressView;

    private String NATURAL_SEARCH = "natural";
    private static final String KWS_SEARCH = "wakeup";
    private static final String KEYPHRASE = "wakeup";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Start loading progress in red color.

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        progressView = (DonutProgress) findViewById(R.id.donut_progress);
        runRecognizerSetup();
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        progressView.setProgress(0);
        progressView.setFinishedStrokeColor(Color.rgb(255, 50, 50));

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
                    progressView.setProgress(0);
                    progressView.setFinishedStrokeColor(R.color.circular_button_normal);
                }
            }

            protected void setupRecognizer(File assetsDir) throws IOException {
                // The recognizer can be configured to perform multiple searches
                // of different kind and switch between them

                recognizer = SpeechRecognizerSetup.defaultSetup()
                        .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                        .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                        .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                        .getRecognizer();
                recognizer.addListener(MainActivity.this);
                publishProgress(10);

                /** In your application you might not need to add all those searches.
                 * They are added here for demonstration. You can leave just one.
                 */
                recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
                publishProgress(20);

                // Create keyword-activation search.
                File languageModel = new File(assetsDir, "en-70k-0.2-pruned.lm");
                recognizer.addNgramSearch(NATURAL_SEARCH, languageModel);
                publishProgress(100);
            }

        }.execute();
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(NATURAL_SEARCH);
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            sendDataToMQTT(text);
        }
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 20000);
    }

    private void sendDataToMQTT(Object data) {
        // data type is string for now
        String text = (String)data;
        //TODO: send text to EMQTT
    }
}
