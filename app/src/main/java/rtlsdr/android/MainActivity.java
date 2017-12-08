package rtlsdr.android;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.*;
import android.view.View.OnClickListener;
import android.location.Location;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    RtlSdrCommunicator usbService;
    private UsbManager mUsbManager;

    Thread myNativeThread = null;
    private TextView statusText;
    private EditText bandwidth;
    private EditText frequency1;
    private EditText frequency2;
    private EditText stepSize;
    private EditText numSamples;
    private EditText gain;
    private EditText locationId;
    private EditText fileN;
    private EditText powerS;
    public static TextView time_indi;
    //private TextView gps_indi;
    private Button applyButton;
    static LocationListener clocationListener, flocationListener;
    static LocationManager lm;
    static FileWriter fw;
    public static BufferedWriter bw;
    public static double rss_show;
    public static List<Double> rss_list = new ArrayList<Double>();

    //add orientation sensor
    private SensorManager mSensorManager;
    private Sensor mOrientation;


    static {
        try {
            System.loadLibrary("usb");
            System.loadLibrary("rtlsdr");
            System.loadLibrary("rtltest");
            System.loadLibrary("rtltcp");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load native library:" + t.getMessage(), t);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        final Context ct_context = this.getApplicationContext();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        statusText = (TextView)findViewById(R.id.statusText);
        bandwidth = (EditText)findViewById(R.id.bandwidth);
        frequency1 = (EditText)findViewById(R.id.frequency1);
        frequency2 = (EditText)findViewById(R.id.frequency2);
        stepSize = (EditText)findViewById(R.id.stepSize);
        numSamples = (EditText)findViewById(R.id.numSamples);
        gain = (EditText)findViewById(R.id.gain);
        fileN = (EditText)findViewById(R.id.fileN);
        //gps_indi = (TextView)findViewById(R.id.gps_indicate);
        powerS = (EditText)findViewById(R.id.powerS);
        //gainMode = (Switch)findViewById(R.id.gainMode);
        locationId = (EditText)findViewById(R.id.exp);
        applyButton = (Button)findViewById(R.id.apply);
        //time_indi = (TextView)findViewById(R.id.mTimer);

        //time_indi.setTextSize(24);

        //sensor
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        clocationListener = new CoarseLocationListener();
        flocationListener = new FineLocationListener();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Initialize fine criteria for location providers
        Criteria fine = new Criteria();
        fine.setAccuracy(Criteria.ACCURACY_FINE);
        fine.setAltitudeRequired(false);
        fine.setBearingRequired(false);
        fine.setSpeedRequired(true);
        fine.setCostAllowed(true);

        // Initialize coarse criteria for location providers.
        Criteria coarse = new Criteria();
        coarse.setAccuracy(Criteria.ACCURACY_COARSE);
        String provider = lm.getBestProvider(coarse, true);
        Location location = lm.getLastKnownLocation(provider); // initial location

        lm.requestLocationUpdates(lm.getBestProvider(coarse, true), 0, 0, clocationListener);
        lm.requestLocationUpdates(lm.getBestProvider(fine, true), 0, 0, flocationListener);


        this.applyButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {

                // GPS Reader
                new run_sdr().start();


                int sr  =  (Math.round(Float.parseFloat(bandwidth.getText().toString()) * 1000 * 1000));
                int freq1 =  Math.round(Float.parseFloat(frequency1.getText().toString()) * 1000 * 1000);
                int freq2 =  Math.round(Float.parseFloat(frequency2.getText().toString()) * 1000 * 1000);
                int step = Math.round(Float.parseFloat(stepSize.getText().toString()) * 1000 * 1000);
                long numS = Long.parseLong(numSamples.getText().toString()) * 2;
                int gainS = Math.round(Float.parseFloat(gain.getText().toString()) * 10);
                String fileName  = frequency1.getText().toString()+ "M" + "_gain" + gainS + "_exp" + locationId.getText() + "_time" + System.currentTimeMillis();
                int powerSample = Integer.parseInt(powerS.getText().toString());

                //Create the file to store the data
                try {
                    File file = new File(Environment.getExternalStorageDirectory().getPath(), fileName + ".txt");
                    fw = new FileWriter(file.getAbsoluteFile());
                    bw = new BufferedWriter(fw);
                } catch (Exception e){
                    e.printStackTrace();
                }

                //new CountDownTimer(40000,1000){
                //    public void onTick(long millisUntilFinished){
                //        time_indi.setText("Remaining(s): " + millisUntilFinished/1000);
                //    }

                //    public void onFinish(){
                //        time_indi.setText("DONE!");
                //    }
                //}.start();

                usbService = new RtlSdrCommunicator(ct_context,freq1,freq2,sr,step,numS,gainS,fileName,powerSample);
            }
        });

        //UI updating thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(700);
                    }
                    catch (InterruptedException e) {
                    }
                    if(usbService==null)
                        continue;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText(String.format("Data rate = %.2f KB/s",usbService.getDataRate()/1000.0));
                        }
                    });
                }
            }
        }).start();
    }

    // Continuously grab GPS data
    final class FineLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location locFromGps) {

            //long currTime = System.currentTimeMillis();
            //gps_indi.setText("GPS: On");
            String data_string = System.currentTimeMillis() + " GPS_fine " + locFromGps.getLatitude() + " " + locFromGps.getLongitude()+ " " + locFromGps.getProvider() + " " + locFromGps.getAccuracy() + " " + locFromGps.getBearing();
            try{
                if (bw!= null){
                    bw.write(data_string + "\n");
                    bw.flush();}

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // called when the GPS provider is turned off (user turning off
            // the GPS on the phone)
        }

        @Override
        public void onProviderEnabled(String provider) {
            // called when the GPS provider is turned on (user turning on
            // the GPS on the phone)
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }


    final class CoarseLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location locFromGps) {

            //long currTime = System.currentTimeMillis();
            //gps_indi.setText("GPS: On");
            String data_string = System.currentTimeMillis() + " GPS_coarse " + locFromGps.getLatitude() + " " + locFromGps.getLongitude()+ " " + locFromGps.getProvider() + " " + locFromGps.getAccuracy() + " " + locFromGps.getBearing();
            try{
                if (bw!= null){
                    bw.write(data_string + "\n");
                    bw.flush();}


            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // called when the GPS provider is turned off (user turning off
            // the GPS on the phone)
        }

        @Override
        public void onProviderEnabled(String provider) {
            // called when the GPS provider is turned on (user turning on
            // the GPS on the phone)
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }


    public class run_sdr extends Thread {

        @Override
        public void run() {

            Log.d(TAG, "Starting nativeMain");
            //This starts the TCP server and blocks until something goes wrong or it quits
            int retval = nativeRtlSdrTcp();
            Log.e(TAG, "Native main returned " + retval);
        }
    }

    private native int nativeRtlSdrTcp();
    
    
    /*    public class UsbService extends Thread {
     int sr_rate;
     int freq;
     Context c;
     
     public UsbService(int sr_rate, int freq, Context c_temp) {
     this.sr_rate = sr_rate;
     this.freq = freq;
     this.c = c_temp;
     }
     
     @Override
     public void run() {
     try{
     Thread.sleep(1000);
     }
     catch (InterruptedException e) {
     }
     RtlSdrCommunicator usbService = new RtlSdrCommunicator(c);
     usbService.setSampleRate(sr_rate);
     usbService.setFreq(freq);
     }
     }*/

    /*
     * Method called by the native code to get a device handle
     */
    public UsbDeviceConnection open(String device_name) {
        Log.d(TAG, "Open called " + device_name);
        UsbDevice usbDevice;
        usbDevice = mUsbManager.getDeviceList().get(device_name);
        if (usbDevice != null) {
            if (mUsbManager.hasPermission(usbDevice)) {
                return mUsbManager.openDevice(usbDevice);
            } else {
                Log.d(TAG, "Missing permissions to open device\n");
            }

        }
        return null;
    }




    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(mOrientationSensorEventListener, mOrientation, SensorManager.SENSOR_DELAY_GAME);

        clocationListener = new CoarseLocationListener();
        flocationListener = new FineLocationListener();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Initialize fine criteria for location providers
        Criteria fine = new Criteria();
        fine.setAccuracy(Criteria.ACCURACY_FINE);
        fine.setAltitudeRequired(false);
        fine.setBearingRequired(false);
        fine.setSpeedRequired(true);
        fine.setCostAllowed(true);

        // Initialize coarse criteria for location providers.
        Criteria coarse = new Criteria();
        coarse.setAccuracy(Criteria.ACCURACY_COARSE);
        String provider = lm.getBestProvider(coarse, true);
        Location location = lm.getLastKnownLocation(provider); // initial location

        lm.requestLocationUpdates(lm.getBestProvider(coarse, true), 0, 0, clocationListener);
        lm.requestLocationUpdates(lm.getBestProvider(fine, true), 0, 0, flocationListener);
    }

    @Override
    public void onPause() {

        super.onPause();
        mSensorManager.unregisterListener(mOrientationSensorEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (lm != null){
            lm.removeUpdates(clocationListener);
            lm.removeUpdates(flocationListener);}

    }


    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    //Sensor
    private SensorEventListener mOrientationSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            float azimuth_angle = Math.round(event.values[0]);
            float pitch_angle = Math.round(event.values[1]);
            float roll_angle = Math.round(event.values[2]);

            String data_string = System.currentTimeMillis() + " ORI " + azimuth_angle + " " + pitch_angle + " " + roll_angle ;

            try {
                if (bw != null){
                    //bw.write(data_string + "\n");
                    //bw.flush();
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
    };


}

