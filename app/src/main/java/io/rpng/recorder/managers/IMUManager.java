package io.rpng.recorder.managers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

import io.rpng.recorder.activities.MainActivity;

public class IMUManager implements SensorEventListener {

    // Activity
    Activity activity;

    // Sensor listeners
    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mGyro;

    // Data storage (linear)
    long linear_time;
    int linear_acc;
    float[] linear_data;

    // Data storage (angular)
    long angular_time;
    int angular_acc;
    float[] angular_data;

    private HandlerThread mSensorThread;
    private Handler mSensorHandler;



    public IMUManager(Activity activity) {
        // Set activity
        this.activity = activity;
        // Create the sensor objects
        mSensorManager = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accelerometer reading
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            linear_acc = accuracy;
        }
        // Handle a gyro reading
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            angular_acc = accuracy;
        }
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        if(!MainActivity.is_recording) return;
            // Set event timestamp to current time in milliseconds
        // http://stackoverflow.com/a/9333605
        event.timestamp = (new Date()).getTime() + (event.timestamp - System.nanoTime()) / 1000000L;

        // TODO: Figure out better way, for now just use the total time
        // https://code.google.com/p/android/issues/detail?id=56561
        event.timestamp = new Date().getTime();


        // Handle accelerometer reading
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            linear_time = event.timestamp;
            linear_data = event.values;

            // Write the data to file if we are recording
//            Log.w("pangfumin"," IMU gyro :"+ linear_time );
            // Create folder name
            String filename = "data_gyro.txt";
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/dataset_recorder/" + MainActivity.folder_name + "/";

            // Create export file
            new File(path).mkdirs();
            File dest = new File(path + filename);

            try {
                // If the file does not exist yet, create it
                if(!dest.exists())
                    dest.createNewFile();

                // The true will append the new data
                BufferedWriter writer = new BufferedWriter(new FileWriter(dest, true));

                // Master string of information
                String data = linear_time
                        + "," + linear_data[0] + "," + linear_data[1] + "," + linear_data[2];

                // Appends the string to the file and closes
                writer.write(data + "\n");
                writer.flush();
                writer.close();
            }
            // Ran into a problem writing to file
            catch(IOException ioe) {
                System.err.println("IOException: " + ioe.getMessage());
            }


            // Reset timestamps
            linear_time = 0;
        }
        // Handle a gyro reading
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            angular_time = event.timestamp;
            angular_data = event.values;

            // Write the data to file if we are recording
//            Log.w("pangfumin"," IMU accl: " + angular_time );
            // Create folder name
            String filename = "data_accel.txt";
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/dataset_recorder/" + MainActivity.folder_name + "/";

            // Create export file
            new File(path).mkdirs();
            File dest = new File(path + filename);

            try {
                // If the file does not exist yet, create it
                if(!dest.exists())
                    dest.createNewFile();

                // The true will append the new data
                BufferedWriter writer = new BufferedWriter(new FileWriter(dest, true));

                // Master string of information
                String data = angular_time
                        + "," + angular_data[0] + "," + angular_data[1] + "," + angular_data[2];

                // Appends the string to the file and closes
                writer.write(data + "\n");
                writer.flush();
                writer.close();
            }
            // Ran into a problem writing to file
            catch(IOException ioe) {
                System.err.println("IOException: " + ioe.getMessage());
            }


            // Reset timestamps
            angular_time = 0;

        }


    }

    /**
     * This will register all IMU listeners
     */
    public void register() {
        // Get the freq we should get messages at (default is SensorManager.SENSOR_DELAY_GAME)
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String imuFreq = sharedPreferences.getString("perfImuFreq", "1");
        // Register the IMUs
//        mSensorManager.registerListener(this, mAccel, Integer.parseInt(imuFreq));
//        mSensorManager.registerListener(this, mGyro, Integer.parseInt(imuFreq));

        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick
        mSensorManager.registerListener(this, mAccel, Integer.parseInt(imuFreq), mSensorHandler);
        mSensorManager.registerListener(this, mGyro, Integer.parseInt(imuFreq), mSensorHandler);
    }

    /**
     * This will unregister all IMU listeners
     */
    public void unregister() {
        mSensorManager.unregisterListener(this, mAccel);
        mSensorManager.unregisterListener(this, mAccel);
        mSensorManager.unregisterListener(this);

        mSensorThread.quitSafely();
    }
}
