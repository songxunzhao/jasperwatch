package com.artemis.speechcmu.services.falldetect;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import static com.google.android.gms.wearable.DataMap.TAG;

public class PostureDetectionService implements SensorEventListener{
    private double ax,ay,az;
    public double a_norm;
    static int BUFF_SIZE=50;
    static private double[] window = new double[BUFF_SIZE];
    double sigma=0.5,th=10,th1=5,th2=2, th4 = 18, diff;
    private SensorManager sensorManager;
    public static String curr_state,prev_state;

    Context context;
    FallDetectListener listener;

    public PostureDetectionService(Context p_context) {
        context = p_context;
        sensorManager=(SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        initialize();

    }

    private void initialize() {
        // TODO Auto-generated method stub
        for(int i=0;i<BUFF_SIZE;i++){
            window[i]=0;
        }
        prev_state="none";
        curr_state="none";

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            ax=event.values[0];
            ay=event.values[1];
            az=event.values[2];
            addData(ax,ay,az);
            posture_recognition(window, ay);
            systemState(curr_state,prev_state);
            if(!prev_state.equalsIgnoreCase(curr_state)){
                prev_state=curr_state;
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setFallDetectListener(FallDetectListener p_listener) {
        listener = p_listener;
    }

    private void addData(double ax2, double ay2, double az2) {
        // TODO Auto-generated method stub
        a_norm=Math.sqrt(ax*ax+ay*ay+az*az);
        for(int i=0;i<=BUFF_SIZE-2;i++){
            window[i]=window[i+1];
        }
        window[BUFF_SIZE-1]=a_norm;

    }
    private int computeZrc(double[] window2) {
        // TODO Auto-generated method stub
        int count=0;
        for(int i=1;i<=BUFF_SIZE-1;i++){

            if((window2[i]-th)<sigma && (window2[i-1]-th)>sigma){
                count=count+1;
            }
        }
        return count;
    }
    private boolean fallDetect(double[] window2) {
        diff = 0;
        for(int i = BUFF_SIZE - 2; i >= 0; i--) {
            if(window2[i] > 0 && a_norm - window2[i] > diff ) {
                diff = a_norm - window2[i];
            }
            else {
                break;
            }
        }
        return (diff >= th4);
    }
    private void posture_recognition(double[] window2,double ay2) {
        // TODO Auto-generated method stub
        int zrc = computeZrc(window2);
        if (zrc == 0) {

            if (Math.abs(ay2) < th1) {
                curr_state = "sitting";
            } else {
                curr_state = "standing";
            }

        } else {

            if (zrc > th2) {
                curr_state = "walking";
            } else {
                curr_state = "none";
            }

        }
    }
    private void systemState(String curr_state1, String prev_state1) {
        // TODO Auto-generated method stub
        if(fallDetect(window) && listener != null)
        {
            //Fall !!
             listener.onFallDetected();
        }

        if(!prev_state1.equalsIgnoreCase(curr_state1)){

            if (curr_state1.equalsIgnoreCase("none")) {
            }
            if (curr_state1.equalsIgnoreCase("sitting")) {
            }
            if (curr_state1.equalsIgnoreCase("standing")) {
            }
            if (curr_state1.equalsIgnoreCase("walking")) {
            }
        }
    }
}
