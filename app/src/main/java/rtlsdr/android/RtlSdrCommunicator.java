package rtlsdr.android;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by sciencectn on 4/13/14.
 */

//The API starts up its stuff as a TCP server
//This connects to it and allows us to interact with it

//Command structure the internal code uses to change parameters
//Param is network byte order
//struct command{
//        unsigned char cmd;
//        unsigned int param;
//        }__attribute__((packed));

public class RtlSdrCommunicator {
    private Socket socket;
    private OutputStream socketOut;
    private InputStream socketIn;

    private Thread commThread;
    private TextView time_indi;

    //private byte[] buf = new byte[500000];

    private static String TAG = "netstuff";

    //Let commands queue up while we wait for the network to behave
    private ConcurrentLinkedQueue<ByteBuffer> commandQueue = new ConcurrentLinkedQueue<ByteBuffer>();

    private long bytesReceived;
    private long lastMillis;     //Since the last time we asked for the data rate

    static byte SET_FREQ = 1;
    static byte SET_SAMPLE_RATE = 2;
    static byte SET_GAIN_MODE = 3;
    static byte SET_GAIN = 4;
    static byte SET_FREQ_CORRECTION = 5;
    static byte SET_END = 6;

    private int BYTE_LEN = (16 * 32 * 512);
    //private int BYTE_LEN = (4 * 512);

    BufferedWriter bw;


       FileOutputStream sampleFile;
//    FileOutputStream sampleFile1;

    public RtlSdrCommunicator(Context c, int tmpfreq1, int tmpfreq2, int tmpRate, int tmpSize, long tmpBytes, int tmpGain, String tmpName, int tmpSample){
        final Context cnxt=c;
        final int freq1 = tmpfreq1;
        final int freq2 = tmpfreq2;
        final int stepSize = tmpSize;
        final int sampleRate = tmpRate;
        final long numBytes = tmpBytes;
        final int gain = tmpGain;
        final String filename = tmpName;
        final int number_samples = tmpSample;

        final Handler handler = new Handler() {

            public void handleMessage(Message msg){
                Toast.makeText(cnxt, "File Closed", Toast.LENGTH_LONG).show();
            }
        };

        /*final Handler new_handler = new Handler() {
            public void handleMessage(Message msg){
                MainActivity.time_indi.setText(String.format("RSS = %.2f dBm", MainActivity.rss_show));
            }
        };*/

        bytesReceived = 0;
        lastMillis = System.currentTimeMillis();

        commThread = new Thread(new Runnable() {
            @Override
            public void run() {

                    for (int j = freq1; j <= freq2; j += stepSize) {
                        try {
                            bw = MainActivity.bw;
                            //added Ana for cyclostationary IQ samples
//                            sampleFile = new FileOutputStream(
//                                    new File(
//                                            Environment.getExternalStorageDirectory().getPath(),
//                                            filename + "_raw.dat"
//                                    )
//                            );

                        } catch (Exception e) {
                        }

                        ByteBuffer cmdStruct = setFreq_SampleRate(j, sampleRate, gain);
                        while (true) {
                            try {

                                connectSocket();
                                socketOut.write(cmdStruct.array());

                                int read_size;
                                int size;
                                long currNumBytes = 0L;
                                int remain_num = 0;
                                byte[] remain_bytes = null;
                                while (currNumBytes < numBytes) {

                                    byte[] read_buf = new byte[BYTE_LEN];
                                    read_size = socketIn.read(read_buf);
    
                                    //added Ana
                                    //sampleFile.write(read_buf,0,read_size);
                                    //sampleFile.flush();


                                    byte[] buf = null;
                                    if (remain_bytes != null) {
                                        buf = combine_bytes(remain_bytes, read_buf);
                                        size = read_size + remain_bytes.length;
                                    } else {
                                        buf = read_buf;
                                        size = read_size;
                                    }


                                    int cal_num = size / (number_samples * 2);
                                    remain_num = size % (number_samples * 2);


                                    if (remain_num == 0) {
                                        remain_bytes = null;
                                    } else if (remain_num > 0) {
                                        remain_bytes = new byte[remain_num];
                                        for (int i = 0; i < remain_num; ++i) {
                                            remain_bytes[i] = buf[size - remain_num + i];
                                        }
                                    }


                                    for (int i = 0; i < cal_num; ++i) {
                                        double sum = 0;
                                        for (int k = 0; k < number_samples * 2; ++k) {
                                            double tmp = buf[i * number_samples * 2 + k];
                                            //int conv = tmp & 0xFF;
                                            if (tmp < 0) {
                                                tmp = tmp + 256;
                                            }
                                            double normalized = (tmp - 128) / 128;
                                            double square = normalized * normalized;
                                            sum += square;
                                        }
                                        double average = sum / number_samples;
                                        double power = Math.log10(average) * 10;
                                        /*MainActivity.rss_list.add(power);
                                        if (MainActivity.rss_list.size() == 1000) {
                                            double sum_list = 0;
                                            for (double item : MainActivity.rss_list) {
                                                sum_list = sum_list + item;
                                            }
                                            double average_list = sum_list / MainActivity.rss_list.size();
                                            MainActivity.rss_show = average_list;
                                            Message msg = new_handler.obtainMessage();
                                            new_handler.sendMessage(msg);
                                            MainActivity.rss_list.clear();
                                        }*/

                                        bw.write(System.currentTimeMillis() + " " + (j / 1000000) + " RSS " + power);
                                        bw.write('\n');
                                    }
                                    bw.flush();

                                    bytesReceived += read_size;
                                    currNumBytes += read_size;

                                }

                                ByteBuffer newStruct = setEnd(0, 0, 0);
                                socketOut.write(newStruct.array());
                                try {
                                    Thread.sleep(50); //check
                                } catch (InterruptedException ie) {
                                }
                                socket.close();

                                break;
                            } catch (IOException e) {
                                Log.w(TAG, "Connection error", e);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ie) {
                                }
                            }
                        }
                    }
                try {

                    bw.close();
                    //sampleFile.close();

                    Message msg = handler.obtainMessage();
                    handler.sendMessage(msg);

                }catch (IOException e) {}
            }
        });
        commThread.start();

    }

    //Get how fast this is reading from the radio, in bytes per second
    public double getDataRate(){
        long now = System.currentTimeMillis();
        double delta = (now - lastMillis) / 1000.0;
        double rate = bytesReceived / delta;
        bytesReceived = 0;
        lastMillis = now;
        return rate;
    }

    public ByteBuffer setFreq_SampleRate(int freq, int rate, int gain){
        ByteBuffer cmdStruct = changeParam(SET_FREQ,freq,rate,gain);
        return cmdStruct;
    }


    public ByteBuffer setEnd(int end, int end1, int end2){

        ByteBuffer cmdStruct = changeParam(SET_END,end,end1, end2);
        return cmdStruct;
    }

    private ByteBuffer changeParam(byte whichParam,int arg,int arg1,int arg2){
        ByteBuffer cmdStruct = ByteBuffer.allocate(13);
        cmdStruct.order(ByteOrder.BIG_ENDIAN);
        cmdStruct.put(whichParam);
        cmdStruct.putInt(arg);
        cmdStruct.putInt(arg1);
        cmdStruct.putInt(arg2);
        //commandQueue.add(cmdStruct);
        return cmdStruct;
    }

    private void connectSocket() throws IOException{
        socket = new Socket("127.0.0.1", 5001);
        socketOut = socket.getOutputStream();
        socketIn = socket.getInputStream();
    }

    private byte[] combine_bytes(byte[] first, byte[] second){
        byte[] result = new byte[first.length + second.length];

        for (int i = 0; i < first.length ; ++i){
            result[i] = first[i];
        }

        for (int j = first.length; j < first.length + second.length; ++j){
            result[j] = second[j - first.length];
        }

        return result;
    }


    //    //Frequency in hertz
//    public void setFreq(int freq){
//
//        changeParam(SET_FREQ,freq);
//    }
//
//    //Sampling rate, (samples/sec)
//    public void setSampleRate(int samplesPerSecond){
//        changeParam(SET_SAMPLE_RATE,samplesPerSecond);
//    }

//    //The gain mode.
//    // This seems to be boolean; for a non-zero mode it means auto-gain
//    public void setGainMode(boolean autoGain){
//        int mode = autoGain ? 1 : 0;
//        changeParam(SET_GAIN_MODE,mode);
//    }
//
//    //The gain, in increments of 1/10 dB
//    public void setGain(int gain){
//
//        changeParam(SET_GAIN,gain);
//    }
//
//    //The frequency correction of the onboard crystal, in parts-per-million (ppm)
//    //I'm guessing this allows a positive or negative value
//    public void setFreqCorrection(int ppm){
//
//        changeParam(SET_FREQ_CORRECTION,ppm);
//    }


}







