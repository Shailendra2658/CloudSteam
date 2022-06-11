package com.temperature.red;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.databinding.DataBindingUtil;

import com.temperature.red.databinding.ActivityLedControlBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class temperatureControl extends Activity {
    public static final int CONNECTED = 3;
    public static final int CONNECTING = 2;
    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int NO_SOCKET_FOUND = 4;
    private static final String TAG = "temperatureControl";
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Button Abt;
    Button Discnt;
    Button Off;

    /* renamed from: On */
    Button f45On;
    String address = null;
    ActivityLedControlBinding binding;
    String bluetooth_message = "00";
    BluetoothSocket btSocket = null;
    int counter;
    /* access modifiers changed from: private */
    public boolean isBtConnected = false;
    ImageView logout;
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);
            int i = msg_type.what;
            if (i == 0) {
                temperatureControl.this.readMsg(((String) msg_type.obj).trim());
            } else if (i == 1) {
                Object obj = msg_type.obj;
            } else if (i == 2) {
                Toast.makeText(temperatureControl.this.getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
            } else if (i == 3) {
                Toast.makeText(temperatureControl.this.getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
            } else if (i == 4) {
                Toast.makeText(temperatureControl.this.getApplicationContext(), "No socket found", Toast.LENGTH_SHORT).show();
            }
        }
    };
    InputStream mmInputStream;
    BluetoothAdapter myBluetooth = null;
    /* access modifiers changed from: private */
    public ProgressDialog progress;
    byte[] readBuffer;
    int readBufferPosition;
    Runnable runnable;
    /* access modifiers changed from: private */
    public TextView statusMsg;
    volatile boolean stopWorker;
    StringBuffer strBuf = new StringBuffer();
    Toast toast;
    Thread workerThread;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.address = getIntent().getStringExtra(DeviceList.EXTRA_ADDRESS);
        setContentView(R.layout.activity_led_control);
        this.binding = (ActivityLedControlBinding) DataBindingUtil.setContentView(this, R.layout.activity_led_control);
        this.f45On = (Button) findViewById(R.id.on_btn);
        this.Off = (Button) findViewById(R.id.off_btn);
        this.Discnt = (Button) findViewById(R.id.dis_btn);
        this.Abt = (Button) findViewById(R.id.abt_btn);
        this.statusMsg = (TextView) findViewById(R.id.textView2);
        this.logout = (ImageView) findViewById(R.id.imageLogout);
        new ConnectBT().execute(new Void[0]);
        this.binding.switchLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!temperatureControl.this.binding.switchLight.isClickable()) {
                    temperatureControl.this.manageBlinkEffect(true);
                    return;
                }
                temperatureControl.this.turnOffLed(isChecked ^ true ? 1 : 0);
                temperatureControl.this.binding.switchLight.setText(isChecked ? "Light ON" : "Light OFF");
            }
        });
        this.binding.switchLight.setOnTouchListener(new View.OnTouchListener() {
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return temperatureControl.this.lambda$onCreate$0$temperatureControl(view, motionEvent);
            }
        });
        this.logout.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                temperatureControl.this.lambda$onCreate$1$temperatureControl(view);
            }
        });
        this.binding.switchTemp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!temperatureControl.this.binding.switchTemp.isClickable()) {
                    temperatureControl.this.manageBlinkEffect(true);
                    return;
                }
                temperatureControl.this.turnOffLed(isChecked ? 2 : 3);
                temperatureControl.this.binding.switchTemp.setText(isChecked ? "Steam ON" : "Steam OFF");
            }
        });
        this.binding.switchTemp.setOnTouchListener(new View.OnTouchListener() {
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return temperatureControl.this.lambda$onCreate$2$temperatureControl(view, motionEvent);
            }
        });
        this.Discnt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                temperatureControl.this.Disconnect();
            }
        });
        this.binding.seekBarTemp.setMax(70);
        this.binding.seekBarTemp.setProgress(25);
        final int[] lastTempProgress = {25};
        this.binding.seekBarTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!seekBar.isClickable()) {
                    seekBar.setProgress(lastTempProgress[0]);
                } else if (progress >= 25) {
                    TextView textView = temperatureControl.this.binding.textSeekTemp;
                    textView.setText(String.valueOf(seekBar.getProgress()) + temperatureControl.this.getString(R.string.degree));
                } else {
                    temperatureControl.this.binding.seekBarTemp.setProgress(25);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                lastTempProgress[0] = seekBar.getProgress();
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() >= 24 && seekBar.isClickable()) {
                    temperatureControl.this.turnOffLed(seekBar.getProgress());
                }
                if (!seekBar.isClickable()) {
                    temperatureControl.this.manageBlinkEffect(true);
                }
            }
        });
        this.binding.seekBarTime.setMax(90);
        this.binding.seekBarTime.setProgress(10);
        final int[] lastProgress = {10};
        this.binding.seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!seekBar.isClickable()) {
                    temperatureControl.this.binding.seekBarTime.setProgress(lastProgress[0]);
                } else if (progress >= 10) {
                    TextView textView = temperatureControl.this.binding.textSeekTime;
                    textView.setText(String.valueOf(seekBar.getProgress()) + " Mins");
                } else {
                    temperatureControl.this.binding.seekBarTime.setProgress(10);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                lastProgress[0] = seekBar.getProgress();
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() >= 9 && seekBar.isClickable()) {
                    temperatureControl.this.turnOffLed(seekBar.getProgress() + 128);
                }
                if (!seekBar.isClickable()) {
                    temperatureControl.this.manageBlinkEffect(true);
                }
            }
        });
    }

    public /* synthetic */ boolean lambda$onCreate$0$temperatureControl(View v, MotionEvent event) {
        if (!this.binding.switchLight.isClickable()) {
            manageBlinkEffect(true);
        } else {
            this.binding.switchLight.setChecked(!this.binding.switchLight.isChecked());
            turnOffLed(this.binding.switchLight.isChecked() ^ true ? 1 : 0);
            this.binding.switchLight.setText(this.binding.switchLight.isChecked() ? "Light ON" : "Light OFF");
        }
        return true;
    }

    public /* synthetic */ void lambda$onCreate$1$temperatureControl(View v) {
        turnOffLed(6);
        Disconnect();
        finish();
    }

    public /* synthetic */ boolean lambda$onCreate$2$temperatureControl(View v, MotionEvent event) {
        if (!this.binding.switchTemp.isClickable()) {
            manageBlinkEffect(true);
        } else {
            this.binding.switchTemp.setChecked(!this.binding.switchTemp.isChecked());
            turnOffLed(this.binding.switchTemp.isChecked() ? 2 : 3);
            this.binding.switchTemp.setText(this.binding.switchTemp.isChecked() ? "Steam ON" : "Steam OFF");
        }
        return true;
    }

    private void enableAll(boolean isEnable) {
        this.binding.switchTemp.setClickable(isEnable);
        this.binding.switchLight.setClickable(isEnable);
        this.binding.seekBarTemp.setClickable(isEnable);
        this.binding.seekBarTime.setClickable(isEnable);
    }

    /* access modifiers changed from: private */
    public void Disconnect() {
        BluetoothSocket bluetoothSocket = this.btSocket;
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                msg("Error", 1);
            }
        }
        finish();
    }

    /* access modifiers changed from: private */
    public void turnOffLed(int i) {
        if (this.btSocket != null) {
            try {
                enableAll(false);
                this.btSocket.getOutputStream().write(i);
            } catch (IOException e) {
                msg("Please wait", 0);
            }
        }
    }

   /* private void turnOnLed(int i) {
        BluetoothSocket bluetoothSocket = this.btSocket;
        if (bluetoothSocket != null) {
            if (i == 0) {
                try {
                    bluetoothSocket.getOutputStream().write(HexCommandtoByte(String.valueOf(i).getBytes()));
                } catch (IOException e) {
                    msg("Error", 1);
                    return;
                }
            } else {
                bluetoothSocket.getOutputStream().write(String.valueOf(i).getBytes());
            }
            this.statusMsg.setText("Bluetooth Waiting...");
        }
    }*/

    /* access modifiers changed from: private */
    public void msg(String s, int toastLength) {
        Toast toast2 = this.toast;
        if (toast2 != null) {
            toast2.cancel();
        }
        if (s.length() > 1) {
            Toast makeText = Toast.makeText(getApplicationContext(), s, toastLength);
            this.toast = makeText;
            makeText.show();
        }
    }

    public void about(View v) {
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess;

        private ConnectBT() {
            this.ConnectSuccess = true;
        }

        /* access modifiers changed from: protected */
        public void onPreExecute() {
            temperatureControl temperaturecontrol = temperatureControl.this;
            ProgressDialog unused = temperaturecontrol.progress = ProgressDialog.show(temperaturecontrol, "Connecting...", "Please wait!!!");
        }

        /* access modifiers changed from: protected */
        public Void doInBackground(Void... devices) {
            try {
                if (temperatureControl.this.btSocket != null && temperatureControl.this.isBtConnected) {
                    return null;
                }
                temperatureControl.this.myBluetooth = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice dispositivo = temperatureControl.this.myBluetooth.getRemoteDevice(temperatureControl.this.address);
                temperatureControl.this.btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(temperatureControl.myUUID);
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                temperatureControl.this.btSocket.connect();
                temperatureControl.this.turnOffLed(9);
                return null;
            } catch (IOException e) {
                this.ConnectSuccess = false;
                return null;
            }
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!this.ConnectSuccess) {
                temperatureControl.this.msg("Connection Failed. Is it a SPP Bluetooth? Try again.", 1);
                temperatureControl.this.finish();
            } else {
                temperatureControl.this.msg("Connected.", 1);
                boolean unused = temperatureControl.this.isBtConnected = true;
                temperatureControl.this.statusMsg.setText("Bluetooth Opened");
                temperatureControl temperaturecontrol = temperatureControl.this;
                new ConnectedThread(temperaturecontrol.btSocket).start();
            }
            temperatureControl.this.progress.dismiss();
        }
    }

    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (byte b : bytes) {
            String hexString = Integer.toHexString(b & 255);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result = result + hexString.toUpperCase();
        }
        return result;
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;

        public ConnectedThread(BluetoothSocket socket) {
            this.mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;
        }

        public void run() {
            while (!isInterrupted()) {
                try {
                    try {
                        Log.i(temperatureControl.TAG, "Read from the InputStream...");
                        InputStream inputStream = this.mmInStream;
                        if (inputStream == null) {
                            Log.e(temperatureControl.TAG, "Lost bluetooth connection!");
                            return;
                        } else if (inputStream.available() > 0) {
                            byte[] buffer = new byte[1024];
                            int bytes = this.mmInStream.read(buffer);
                            Log.i(temperatureControl.TAG, "Read from the InputStream, length is " + bytes);
                            String string_recieved = new String(buffer, "ASCII").substring(0, bytes);
                            if (string_recieved.contains("*")) {
                                temperatureControl.this.strBuf.append(string_recieved.substring(0, string_recieved.indexOf("*")));
                                temperatureControl.this.mHandler.obtainMessage(0, bytes, -1, temperatureControl.this.strBuf.toString().replace("*", "").trim()).sendToTarget();
                                String pending = string_recieved.substring(string_recieved.indexOf("*"));
                                temperatureControl.this.strBuf = new StringBuffer();
                                temperatureControl.this.strBuf.append(pending);
                            } else {
                                temperatureControl.this.strBuf.append(string_recieved);
                            }
                        }
                    } catch (IOException e) {
                        Log.e(temperatureControl.TAG, "disconnected", e);
                        return;
                    }
                } catch (Exception e2) {
                    Log.e(temperatureControl.TAG, "disconnected", e2);
                    return;
                }
            }
        }

        public void write(byte[] buffer) {
        }
    }

    /* access modifiers changed from: private */
    public void readMsg(String string_recieved) {
        if (string_recieved.startsWith("time")) {
            TextView textView = this.binding.textMins;
            textView.setText(string_recieved.substring(4, 7) + " Mins");
        } else if (string_recieved.startsWith("switch")) {
            try {
                if (string_recieved.substring(6).contains("false")) {
                    this.binding.switchTemp.setChecked(false);
                } else {
                    this.binding.switchTemp.setChecked(true);
                }
            } catch (Exception e) {
            }
        } else if (string_recieved.startsWith("temp")) {
            TextView textView2 = this.binding.textDegree;
            textView2.setText(string_recieved.substring(4, 7) + getString(R.string.degree));
        } else if (string_recieved.startsWith("slidet")) {
            try {
                this.binding.seekBarTemp.setProgress(Integer.parseInt(string_recieved.substring(7, 9)));
                TextView textView3 = this.binding.textSeekTemp;
                textView3.setText(string_recieved.substring(7, 9) + getString(R.string.degree));
            } catch (Exception e2) {
            }
        } else if (string_recieved.startsWith("mins")) {
            try {
                this.binding.seekBarTime.setProgress(Integer.parseInt(string_recieved.substring(5, 7)));
                TextView textView4 = this.binding.textSeekTime;
                textView4.setText(string_recieved.substring(5, 7) + " Mins");
            } catch (Exception e3) {
            }
        } else if (string_recieved.startsWith("light")) {
            try {
                if (string_recieved.substring(5).contains("false")) {
                    this.binding.switchLight.setChecked(false);
                } else {
                    this.binding.switchLight.setChecked(true);
                }
            } catch (Exception e4) {
            }
        } else if (string_recieved.equalsIgnoreCase("ok")) {
            enableAll(true);
            manageBlinkEffect(false);
        }
    }

    public static byte[] HexCommandtoByte(byte[] data) {
        if (data == null) {
            return null;
        }
        String[] strings = new String(data, 0, data.length).split(" ");
        int nLength = strings.length;
        byte[] data2 = new byte[nLength];
        for (int i = 0; i < nLength; i++) {
            if (strings[i].length() != 2) {
                data2[i] = 0;
            } else {
                try {
                    data2[i] = (byte) Integer.parseInt(strings[i], 16);
                } catch (Exception e) {
                    data2[i] = 0;
                }
            }
        }
        return data2;
    }

    /* access modifiers changed from: private */
    public void blink() {
        final Handler handler = new Handler();
        if (this.runnable == null) {
            this.runnable = new Runnable() {
                public void run() {
                    try {
                        Thread.sleep((long) 1000);
                    } catch (Exception e) {
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            if (temperatureControl.this.binding.textPlzWait.getVisibility() == View.VISIBLE) {
                                temperatureControl.this.binding.textPlzWait.setVisibility(View.INVISIBLE);
                            } else {
                                temperatureControl.this.binding.textPlzWait.setVisibility(View.VISIBLE);
                            }
                            temperatureControl.this.blink();
                        }
                    });
                }
            };
        }
        new Thread(this.runnable).start();
    }

    /* access modifiers changed from: private */
    public void manageBlinkEffect(boolean isShow) {
        this.binding.textPlzWait.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
    }

    /* access modifiers changed from: package-private */
    public void beginListenForData() {
        final Handler handler = new Handler();
        this.stopWorker = false;
        this.readBufferPosition = 0;
        this.readBuffer = new byte[1024];
        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !temperatureControl.this.stopWorker) {
                    try {
                        int bytesAvailable = temperatureControl.this.mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            temperatureControl.this.mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == 10) {
                                    byte[] encodedBytes = new byte[temperatureControl.this.readBufferPosition];
                                    System.arraycopy(temperatureControl.this.readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    temperatureControl.this.readBufferPosition = 0;
                                    handler.post(new Runnable() {
                                        public void run() {
                                            temperatureControl.this.statusMsg.setText(data);
                                        }
                                    });
                                } else {
                                    byte[] bArr = temperatureControl.this.readBuffer;
                                    temperatureControl temperaturecontrol = temperatureControl.this;
                                    int i2 = temperaturecontrol.readBufferPosition;
                                    temperaturecontrol.readBufferPosition = i2 + 1;
                                    bArr[i2] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        temperatureControl.this.stopWorker = true;
                        handler.post(new Runnable() {
                            public void run() {
                                temperatureControl.this.statusMsg.setText(ex.getMessage());
                            }
                        });
                    }
                }
            }
        });
        this.workerThread = thread;
        thread.start();
    }
}
