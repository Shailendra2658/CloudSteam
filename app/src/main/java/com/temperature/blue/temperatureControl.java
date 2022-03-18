package com.temperature.blue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.imageview.ShapeableImageView;
import com.temperature.blue.databinding.ActivityLedControlBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import androidx.databinding.DataBindingUtil;

import static com.temperature.blue.SplashScreen.getCounter;


public class temperatureControl extends Activity {

    private static final String TAG = "temperatureControl";
    // Button btnOn, btnOff, btnDis;
    Button On, Off, Discnt, Abt;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int CONNECTING = 2;
    public static final int CONNECTED = 3;
    public static final int NO_SOCKET_FOUND = 4;
    String bluetooth_message = "00";
    private TextView statusMsg;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    ActivityLedControlBinding binding;
    ShapeableImageView imageLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_led_control);

        //call the widgets
        On = (Button) findViewById(R.id.on_btn);
        Off = (Button) findViewById(R.id.off_btn);
        Discnt = (Button) findViewById(R.id.dis_btn);
        Abt = (Button) findViewById(R.id.abt_btn);
        statusMsg = (TextView) findViewById(R.id.textView2);
        imageLogout = (ShapeableImageView) findViewById(R.id.imageLogout);

        new ConnectBT().execute(); //Call the class to connect

        if (getCounter(temperatureControl.this) > 20)
            finish();

        //commands to be sent to bluetooth
        binding.switchLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                turnOffLed(isChecked ? 0 : 1);      //method to turn on
                binding.switchLight.setText(isChecked ? "Light ON" : "Light OFF");

            }
        });

        binding.switchTemp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //turnOffLed();   //method to turn off
                turnOffLed(isChecked ? 2 : 3);
                //turnOnLed(isChecked?0:1);
                binding.switchTemp.setText(isChecked ? "Steam ON" : "Steam OFF");
            }
        });

        Discnt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect(); //close connection
            }
        });

        imageLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect(); //close connection
                finishAffinity();
            }
        });

        binding.seekBarTemp.setMax(70);
        binding.seekBarTemp.setProgress(25);
        binding.seekBarTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 25) {
                    binding.textSeekTemp.setText(String.valueOf(seekBar.getProgress()) + getString(R.string.degree));
                }else
                    binding.seekBarTemp.setProgress(25);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() >= 24)
                    turnOffLed(seekBar.getProgress());
            }
        });

        binding.seekBarTime.setMax(90);
        binding.seekBarTime.setProgress(10);
        binding.seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 10) {
                    binding.textSeekTime.setText(String.valueOf(seekBar.getProgress()) + " Mins");
                }else
                    binding.seekBarTime.setProgress(10);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() >= 9)
                    turnOffLed(seekBar.getProgress() + 128);
            }
        });

    }

    private void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {
                msg("Error");
            }
        }
        finish(); //return to the first layout

    }

    private void turnOffLed(int i) {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(i);
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void turnOnLed(int i) {
        if (btSocket != null) {
            try {
                /*for (byte b : "Hello".toString().getBytes()) {
                    btSocket.getOutputStream().write(b);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        msg("Error sleep "+e);

                    }
                }*/
                // btSocket.getOutputStream().write(0XA);
                if (i == 0)
                    btSocket.getOutputStream().write(HexCommandtoByte(String.valueOf(i).getBytes()));
                else
                    btSocket.getOutputStream().write(String.valueOf(i).getBytes());
                //char[] chars = Hex.encodeHex(String.valueOf(i).getBytes(StandardCharsets.UTF_8));

//                else
//                    btSocket.getOutputStream().write("31".toString().getBytes());

                statusMsg.setText("Bluetooth Waiting...");
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void about(View v) {
       /* if(v.getId() == R.id.abt)
        {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(temperatureControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                    turnOffLed(9);
                    // mmOutputStream = btSocket.getOutputStream();
                    // mmInputStream = btSocket.getInputStream();
                    // beginListenForData();


                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected.");
                isBtConnected = true;
                statusMsg.setText("Bluetooth Opened");
                // Start the thread to manage the connection and perform transmissions
                ConnectedThread mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();
            }
            progress.dismiss();
        }
    }

    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        return result;

    }

    // Incoming and the outgoing strings are carried out inside this thread read is for reading incoming messages through a socket and write is for sending messages to the remote device
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                // socket not created
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            // receiving message
            try {
                // Read from the InputStream
                //bytes = mmInStream.read(buffer);
                // Keep listening to the InputStream while connected
                while (true) {
                    try {
                        // Read from the InputStream
                        Log.i(TAG, "Read from the InputStream...");
                        bytes = mmInStream.read(buffer);
                        Log.i(TAG, "Read from the InputStream, length is " + bytes);

                        // Send the obtained bytes to the UI Activity
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    } catch (IOException e) {
                        Log.e(TAG, "disconnected", e);
                        //connectionLost();
                        break;
                    }
                }

                // message is in bytes form so reading them to obtain message
//                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
//                        .sendToTarget();
            } catch (Exception e) {
                // connection was lost and start your connection again
                Log.e(TAG, "disconnected", e);
                // break;
            }
        }

        public void write(byte[] buffer) {
            try {
                //   mmOutStream.write(buffer);
                // mHandler is to show send message from device
                // mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer);
                // mHandler.sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);

            switch (msg_type.what) {
                case MESSAGE_READ:

                    byte[] readbuf = (byte[]) msg_type.obj;
                    String string_recieved = new String(readbuf);

                    //do some task based on recieved string
                    //Toast.makeText(getApplicationContext(), "MESSAGE_READING..." + string_recieved, Toast.LENGTH_SHORT).show();
                    if (string_recieved.startsWith("time"))//"time-34 "
                        binding.textMins.setText(string_recieved.substring(4, 7) + " Mins");
                    else if (string_recieved.startsWith("switch")) {
                        try {
                            // msg("After parsing " + string_recieved.substring(6));
                            if (string_recieved.substring(6).contains("false"))
                                binding.switchTemp.setChecked(false);
                            else
                                binding.switchTemp.setChecked(true);
                        } catch (Exception ex) {
                            //msg("Failed to read");
                        }
                    } else if (string_recieved.startsWith("temp"))
                        binding.textDegree.setText(string_recieved.substring(4, 7) + getString(R.string.degree));
                    else if (string_recieved.startsWith("slidet")) {
                        try {
                            // msg("After parsing " + string_recieved.substring(6)); slidet-26
                            int val = Integer.parseInt(string_recieved.substring(7, 9));
                            binding.seekBarTemp.setProgress(val);
                            binding.textSeekTemp.setText(string_recieved.substring(7, 9) + getString(R.string.degree));

                        } catch (Exception ex) {
                            //msg("Failed to read");
                        }
                    }else if (string_recieved.startsWith("mins")) {
                        try {
                            // msg("After parsing " + string_recieved.substring(6)); mins-39
                            int val = Integer.parseInt(string_recieved.substring(5, 7));
                            binding.seekBarTime.setProgress(val);
                            binding.textSeekTime.setText(string_recieved.substring(5, 7) + " Mins");

                        } catch (Exception ex) {
                            //msg("Failed to read");
                        }
                    }else if (string_recieved.startsWith("light")) {
                        try {
                            // msg("After parsing " + string_recieved.substring(6));
                            if (string_recieved.substring(5).contains("false"))
                                binding.switchLight.setChecked(false);
                            else
                                binding.switchLight.setChecked(true);
                        } catch (Exception ex) {
                            //msg("Failed to read");
                        }
                    }

                    break;
                case MESSAGE_WRITE:

                    if (msg_type.obj != null) {
//                        ConnectedThread connectedThread=new ConnectedThread((BluetoothSocket)msg_type.obj);
//                        connectedThread.write(bluetooth_message.getBytes());

                    }
                    break;

                case CONNECTED:
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    break;

                case CONNECTING:
                    Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                    break;

                case NO_SOCKET_FOUND:
                    Toast.makeText(getApplicationContext(), "No socket found", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public static byte[] HexCommandtoByte(byte[] data) {

        if (data == null) {

            return null;

        }

        int nLength = data.length;


        String strTemString = new String(data, 0, nLength);

        String[] strings = strTemString.split(" ");

        nLength = strings.length;

        data = new byte[nLength];

        for (int i = 0; i < nLength; i++) {

            if (strings[i].length() != 2) {

                data[i] = 00;

                continue;

            }

            try {

                data[i] = (byte) Integer.parseInt(strings[i], 16);

            } catch (Exception e) {

                data[i] = 00;

                continue;

            }

        }

        return data;

    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            statusMsg.setText(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (final IOException ex) {
                        stopWorker = true;
                        handler.post(new Runnable() {
                            public void run() {
                                statusMsg.setText(ex.getMessage());
                            }
                        });
                    }
                }
            }
        });

        workerThread.start();
    }

}
