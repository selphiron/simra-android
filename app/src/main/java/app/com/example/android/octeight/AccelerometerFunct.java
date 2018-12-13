package app.com.example.android.octeight;

import android.app.usage.UsageEvents;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class AccelerometerFunct implements SensorEventListener {

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Properties
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Context ctx;

    public Sensor myAccSensor;

    public SensorManager accSensorManager;

    public static ExecutorService myEx;

    // the time at which recording begins (in millis) will be handed over from MainActivity
    // as soon as startRecording-button is pressed.
    public long myRecordingTimeVar = 0L;

    boolean recording;

    public ArrayList<Float> xList = new ArrayList<>();
    public ArrayList<Float> yList = new ArrayList<>();
    public ArrayList<Float> zList = new ArrayList<>();

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Methods
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public void onSensorChanged(SensorEvent event) {

        myEx.execute(() -> recordData(event));


    }

    public synchronized void recordData(SensorEvent event) {

        while(recording) {

            // The accelerometer returns 3 values, one for each axis.
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Check if the determined recording time interval has elapsed, if so, write data
            // into lists & reset the variable
            if(System.currentTimeMillis() == (myRecordingTimeVar + Constants.ACC_FREQUENCY)) {

                // Add the accelerometer data to the respective ArrayLists.
                xList.add(x);
                yList.add(y);
                zList.add(z);

                // Reset the time variable
                myRecordingTimeVar = System.currentTimeMillis();

            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public AccelerometerFunct(Context ctx, ExecutorService exec) {
        this.ctx = ctx;
        this.accSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        this.myAccSensor = this.accSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        myEx = exec;
    }

    public void register(){
        accSensorManager.registerListener(this, myAccSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregister(){
        accSensorManager.unregisterListener(this);
    }

    public synchronized void saveRouteData() {

        FileOutputStream fos = null;

        /**String xString = myAccService.xList.toString();
         create(this, "x_accelerometer.csv", xString);

         String yString = myAccService.yList.toString();
         create(this, "y_accelerometer.csv", yString);

         String zString = myAccService.zList.toString();
         create(this, "z_accelerometer.csv", zString);*/

        try {

            fos = ctx.openFileOutput("accData.csv", Context.MODE_PRIVATE);

        } catch (FileNotFoundException fnfe) {

            fnfe.printStackTrace();

        }

        try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            CSVPrinter csvPrinter = new CSVPrinter(osw, CSVFormat.DEFAULT.withHeader("x-axis", "y-axis",
                    "z-axis"));
            for(int i = 0; i < xList.size(); i++) {
                csvPrinter.printRecord(xList.get(i),
                                        yList.get(i),
                                        zList.get(i));

            }
            csvPrinter.flush();
            csvPrinter.close();

        } catch (IOException ioe) {

            ioe.printStackTrace();
        }
    }

}
