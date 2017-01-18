package com.artemis.speechcmu.services;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.widget.Toast;

import com.artemis.speechcmu.MainActivity;
import com.artemis.speechcmu.R;

public class PostureDetectionService implements SensorEventListener{
    private double ax,ay,az;
    public double a_norm;
    static int BUFF_SIZE=50;
    static private double[] window = new double[BUFF_SIZE];
    double sigma=0.5,th=10,th1=5,th2=2;
    private SensorManager sensorManager;
    public static String curr_state,prev_state;
    Context context;

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
            TextView view = (TextView) ((MainActivity)context).findViewById(R.id.textView);

            ax=event.values[0];
            ay=event.values[1];
            az=event.values[2];
            view.setText(ax + " " + ay + " " + az);
            AddData(ax,ay,az);
            posture_recognition(window, ay);
            SystemState(curr_state,prev_state);
            if(!prev_state.equalsIgnoreCase(curr_state)){
                prev_state=curr_state;
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void AddData(double ax2, double ay2, double az2) {
        // TODO Auto-generated method stub
        a_norm=Math.sqrt(ax*ax+ay*ay+az*az);
        for(int i=0;i<=BUFF_SIZE-2;i++){
            window[i]=window[i+1];
        }
        window[BUFF_SIZE-1]=a_norm;

    }
    private void posture_recognition(double[] window2,double ay2) {
        // TODO Auto-generated method stub
        int zrc = compute_zrc(window2);
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

    private boolean fall_detection(double[] window2) {
        double diff = 0;
        for(int i = BUFF_SIZE - 2; i > 0; i++) {
            if(a_norm - window2[i] > diff ) {
                diff = a_norm - window2[i];
            }
            else {
                break;
            }
        }
        Toast.makeText(context, "" + diff, Toast.LENGTH_SHORT).show();
        return (diff >= 2*9.8);
    }

    private int compute_zrc(double[] window2) {
        // TODO Auto-generated method stub
        int count=0;
        for(int i=1;i<=BUFF_SIZE-1;i++){

            if((window2[i]-th)<sigma && (window2[i-1]-th)>sigma){
                count=count+1;
            }

        }
        return count;
    }

    private void SystemState(String curr_state1,String prev_state1) {
        // TODO Auto-generated method stub

        //Fall !!
        if(!prev_state1.equalsIgnoreCase(curr_state1)){
            if(fall_detection(window))
            {
                Toast.makeText(context, "fall", Toast.LENGTH_SHORT).show();
            } else {
                if (curr_state1.equalsIgnoreCase("none")) {
                    Toast.makeText(context, "none", Toast.LENGTH_SHORT).show();
                }
                if (curr_state1.equalsIgnoreCase("sitting")) {
                    Toast.makeText(context, "sit", Toast.LENGTH_SHORT).show();
                }
                if (curr_state1.equalsIgnoreCase("standing")) {
                    Toast.makeText(context, "stand", Toast.LENGTH_SHORT).show();
                }
                if (curr_state1.equalsIgnoreCase("walking")) {
                    Toast.makeText(context, "walking", Toast.LENGTH_SHORT).show();

                }
            }
        }
    }
}
