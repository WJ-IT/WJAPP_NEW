package wjm.co.kr.wjapp_new;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

public class SwingAPI {
    // Debugging
    private static final String TAG = "dsm362";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "SwingUI";
    public String deviceName = "";
    // Unique UUID for this application
    // SPP UUID: 00001101-0000-1000-8000-00805F9B34FB
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mBTadapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_START = 6;
    public static final int MESSAGE_STOP = 7;
    public static final int MESSAGE_FOUND = 8;
    public static final int MESSAGE_TAG = 9;
    public static final int MESSAGE_BATTERY = 11;
    public static final int MESSAGE_POWER = 12;
    public static final int MESSAGE_POWERRANGE = 13;
    public static final String TOAST = "toast";

    // Command Code
    public static final byte CONTINUOUS_MODE = 0x63;
    public static final byte ALL_DEFAULT = 0x64;
    public static final byte POWER_ATTEN = 0x63;
    public static final byte ACCESS_PW = 0x63;

    public enum InventoryMode { INVENTORY_NORMAL, INVENTORY_WITHUSER, SEARCH_SINGLE, SEARCH_MULTI, SEARCH_WILDCARD };
    private InventoryMode _param_inventory_mode = InventoryMode.INVENTORY_NORMAL;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public SwingAPI(Context context, Handler handler) {
        mBTadapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        try {
            if (D) Log.d("dsm362", "mSwing.start begin");

            // Cancel any thread attempting to make a connection
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

            // Start the thread to listen on a BluetoothServerSocket
            if (mAcceptThread == null && mBTadapter != null) {
                mAcceptThread = new AcceptThread();
                mAcceptThread.start();
            }
            setState(STATE_LISTEN);
        } catch (Exception e) {
            if (D) Log.d("dsm362", "mSwing.start: " + e.getMessage());
        }
        if (D) Log.d("dsm362", "mSwing.start end");
    }

    public synchronized boolean isConnected() {
        if (mConnectedThread != null) return true;
        else return false;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(TOAST, "장치 연결에 실패하였습니다.");
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void manageConnectedSocket(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
//        Bundle bundle = new Bundle();
//        bundle.putString("SCANNER_NAME", device.getName());
//        msg.setData(bundle);
        msg.arg1 = 1;
        String scanner = device.getName();
        msg.obj = scanner;
        mHandler.sendMessage(msg);
        Log.i(TAG, device.getName());
        deviceName = device.getName();

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        try {
            Log.d("dsm362", "mSwing.stop begin");

            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
            if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
            if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

            setState(STATE_NONE);
        } catch (Exception e) {
            Log.e("dsm362", "mSwing.stop: " + e.getMessage());
        }
        Log.d("dsm362", "mSwing.stop end");
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     //     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    private void responseClear() {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.mResponseUpdated = false;
        r.mResponseLength = 0;
        Arrays.fill(r.mResponse, (byte)0);
    }

    private boolean responseUpdated() {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return false;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        return r.mResponseUpdated;
    }

    private byte[] responseObtain() {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return null;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        byte[] resp = new byte[r.mResponseLength];
        System.arraycopy(r.mResponse, 0, resp, 0, r.mResponseLength);
        return resp;
    }

    private void sleep(long i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException during sleep", e);
        }
    }

    private boolean mTimerEnded = false;

    private void startTimer(long timeOut)
    {
        mTimerEnded = false;
        sleep(timeOut);
        mTimerEnded = true;
    }

    private void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    private void write(byte out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public void swing_setAllTagReport(boolean on_all_tag_report) {
        String cmdString = ">x b ";

        if(on_all_tag_report) {
            cmdString += "1\r";
        } else {
            cmdString += "0\r";
        }

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void swing_setLCDmode(String mode) {
        String cmdString = ">x o ";

        cmdString += mode + "\r";

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void swing_setDisplay(String strFirst, String strSecond, String strThird) {
        String cmdString = ">x x ";

        if(strFirst.length() == 0) {
            cmdString += "1\r";
        } else {
            cmdString += "1 " + strFirst + "\r";
        }

        if(strSecond.length() == 0) {
            cmdString += "2\r";
        } else {
            cmdString += ">x x 2 " + strSecond + "\n";
        }

        if(strThird.length() == 0) {
            cmdString += "3\r";
        } else {
            cmdString += ">x x 3 " + strThird + "\n";
        }

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void swing_setContinuous(boolean on_continuous) {
        String cmdString = ">x c ";

        if(on_continuous) {
            cmdString += "1\r";
        } else {
            cmdString += "0\r";
        }

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public int swing_getContinuous() {
        String cmdString = ">y c\r";
        byte[] pkt = cmdString.getBytes();
        byte[] ack = null;

        responseClear();
        write(pkt);
        startTimer(1000);

        for(;;) {
            sleep(20);
            if(responseUpdated()) {
                ack = responseObtain();
                responseClear();
                break;
            }
            if(mTimerEnded) return (-2); //응답 수신 타임 아웃 발생
        }

        if(!(ack[0] == '>' && ack[1] == 'c')) return (-1); //잘못된 응답 수신
        return (ack[2] - 0x30);
    }

    public int swing_getTagCount() {
        String cmdString = ">y n\r";
        byte[] pkt = cmdString.getBytes();
        byte[] ack = null;

        responseClear();
        write(pkt);
        startTimer(300);

        for(;;) {
            sleep(20);
            if(responseUpdated()) {
                ack = responseObtain();
                responseClear();
                break;
            }
            if(mTimerEnded) return (-2);
        }

        if(!(ack[0] == '>' && ack[1] == 'n')) return (-1);

        return (ack[2] - 0x30);
//        return ack[2];
    }

    public void prisma_setInit() {
        String cmdString = String.format("~wk1\r\n~wb0\r\n");

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void prisma_getBattery() {
        String cmdString = String.format("~re\r\n");

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void prisma_tagDel() {
        String cmdString = String.format("~aD\r\n");

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void prisma_getPower() {
        String cmdString = String.format("~rp\r\n");

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void prisma_setPower(String val) {
        String cmdString = String.format("~wp"+val+"\r\n");

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void prisma_getPowerRange() {
        String cmdString = String.format("~ra\r\n");

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void swing_setBuzzerVolume(int level) {
        if(level == g_volume) return;

        String cmdString = String.format(">x s %d\r", level);

        g_volume = level;

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void swing_setPower(int attenuation) {
        String cmdString = String.format(">x p %d\r", attenuation);
        g_rf_atten = attenuation;

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public int g_threshold = 15;

    public void swing_setThreshold(int th) {
        if(th == g_threshold) return;

        String cmdString = String.format(">x k %d\r", th);
        g_threshold = th;

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public int g_unit = 1;

    public void swing_setUnit(int unit) {
        if(unit == g_unit) return;

        String cmdString = String.format(">x u %d\r", unit);

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public int g_rf_atten = 0;

    public int swing_getPower() {
        String cmdString = ">y p\r";
        byte[] pkt = cmdString.getBytes();
        byte[] ack = null;

        responseClear();
        write(pkt);
        startTimer(300);

        for(;;) {
            sleep(20);
            if(responseUpdated()) {
                ack = responseObtain();
                responseClear();
                break;
            }
            if(mTimerEnded) return (-2); //���� ���� Ÿ�� �ƿ� �߻�
        }

        if(!(ack[0] == '>' && ack[1] == 'p')) return (-1); //�߸�� ���� ����

        int power = 0;
        if(ack.length == 4) {
            power = ack[2] - 0x30;
        } else {
            power = (ack[2] - 0x30)*10 + (ack[3] - 0x30);
        }

        g_rf_atten = power;

        return power;
    }

    public int g_volume = 4;

    public int swing_getBuzzerVolume() {
        String cmdString = ">y s\r";
        byte[] pkt = cmdString.getBytes();
        byte[] ack = null;

        responseClear();
        write(pkt);
        startTimer(1000);

        for(;;) {
            sleep(20);
            if(responseUpdated()) {
                ack = responseObtain();
                responseClear();
                break;
            }
            if(mTimerEnded) return (-2); //���� ���� Ÿ�� �ƿ� �߻�
        }

        if(!(ack[0] == '>' && ack[1] == 's')) return (-1); //�߸�� ���� ����

        int volume = 0;
        if(ack.length >= 4) {
            volume = ack[3] - 0x30;
        } else {
            volume = -1;
        }

        g_volume = volume;

        return volume;
    }

    public int swing_getEPCUserMode() {
        String cmdString = ">y y\r";
        byte[] pkt = cmdString.getBytes();
        byte[] ack = null;

        responseClear();
        write(pkt);
        startTimer(300);

        for(;;) {
            sleep(20);
            if(responseUpdated()) {
                ack = responseObtain();
                responseClear();
                break;
            }
            if(mTimerEnded) return (-2); //���� ���� Ÿ�� �ƿ� �߻�
        }

        if(!(ack[0] == '>' && ack[1] == 'y')) return (-1); //�߸�� ���� ����

        return (ack[2] - 0x30);
    }

    public void swing_setEPCUserMode(boolean on_ecp_user) {
        String cmdString = ">x y ";

        if(on_ecp_user) {
            cmdString += "1\r";
        } else {
            cmdString += "0\r";
        }

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public int swing_getUserMemLength() {
        String cmdString = ">y 7\r";
        byte[] pkt = cmdString.getBytes();
        byte[] ack = null;

        responseClear();
        write(pkt);
        startTimer(300);

        for(;;) {
            sleep(20);
            if(responseUpdated()) {
                ack = responseObtain();
                responseClear();
                break;
            }
            if(mTimerEnded) return (-2); //���� ���� Ÿ�� �ƿ� �߻�
        }

        if(!(ack[0] == '>' && ack[1] == '7')) return (-1); //�߸�� ���� ����

        int power = 0;
        if(ack.length == 4) {
            power = ack[2] - 0x30;
        } else {
            power = (ack[2] - 0x30)*10 + (ack[3] - 0x30);
        }

        return power;
    }

    public void swing_set_search_target(String id) {
        swing_set_multi_search();
        String cmdString = ">x l 1 ";
        cmdString += id;
        cmdString += "\r";

        byte[] pkt = cmdString.getBytes();
//        byte[] tmp = null;
//        for(int i = 0 ; i < pkt.length; i++) {
//            Log.i("aaafdewqvve", String.valueOf(pkt[i]));
//        }
//        cmdString = null;
//        tmp = cmdString.getBytes();
//        pkt[10] = tmp[0];
//        for(int i = 0 ; i < pkt.length; i++) {
//            Log.i("xx", String.valueOf(pkt[i]));
//        }
        write(pkt);
    }

    public void swing_set_multi_search() {
        String cmdString = ">x l c";
        cmdString += "\r";

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void swing_set_inventory_mode(InventoryMode mode) {
        if(_param_inventory_mode == mode) return;

        String cmd = String.format(">x m %d\r", mode.ordinal());
        Log.d("dsm362", cmd);
        byte[] pkt = cmd.getBytes();
        write(pkt);
    }

    public void swing_init() {
        String strinit;
        strinit = ">x l c\n>x m 0\n>x o 0\n";
        byte[] pkt = strinit.getBytes();
        write(pkt);
        swing_setPower(0);
    }

    public void swing_write(String no_tag) {
        String strinit;
        strinit = ">w 01 02 06 ";
        strinit += no_tag + "\n";
        byte[] pkt = strinit.getBytes();
        write(pkt);
    }
/*
    public void swing_set_mode_inventory() {
    	String cmdString = ">x m 0\r";
    	byte[] pkt = cmdString.getBytes();
    	write(pkt);
    }

    public void swing_set_mode_readmem() {
    	String cmdString = ">x m 1\r";
    	byte[] pkt = cmdString.getBytes();
    	write(pkt);
    }

    public void swing_set_mode_search() {
    	String cmdString = ">x m 2\r";
    	byte[] pkt = cmdString.getBytes();
    	write(pkt);
    }
*/

    public void swing_setUserMemLength(int length) {
        if(length < 0 || length > 32) return;

        String cmdString = ">x 7 ";
        cmdString += Integer.toString(length);
        cmdString += "\r";

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void swing_saveParam() {
        String cmdString = ">x a\r";
        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public boolean swing_setAccessPass(String password) {
        int pLength = password.length();

        if(pLength != 8) return false;

        String cmdString = ">x w ";
        cmdString += password;
        cmdString += "\r";

        byte[] pkt = cmdString.getBytes();
        write(pkt);

        return true;
    }

    public void swing_clearAccessPass() {
        String cmdString = ">x w 0\r";

        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public String swing_getAccessPass() {
        String cmdString = ">y w\r";
        byte[] pkt = cmdString.getBytes();
        byte[] ack = null;
        String retValue = "";

        responseClear();
        write(pkt);
        startTimer(300);

        for(;;) {
            sleep(20);
            if(responseUpdated()) {
                ack = responseObtain();
                responseClear();
                break;
            }
            if(mTimerEnded) return "-2";
        }

        if(!(ack[0] == '>' && ack[1] == 'w')) return "-1";

        if(ack.length == 4) {
            retValue = "0";
        } else {
            for (int i = 2; i < 10; i++) {
                retValue += (char)ack[i];
            }
        }

        return retValue;
    }

    public void swing_readStart() {
        String cmdString = ">f\r";
        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void swing_clear_inventory() {
        String cmdString = ">c\r";
        byte[] pkt = cmdString.getBytes();
        write(pkt);
    }

    public void swing_readStop() {
        byte pkt = '3';
        write(pkt);
    }

    public void swing_multiReadStart() {
        swing_setContinuous(true);
        sleep(100);
        swing_readStart();
    }

    public void swing_singleReadStart() {
        swing_setContinuous(false);
        sleep(100);
        swing_readStart();
    }
    boolean on_inventory = false;

    public boolean swing_on_inventory() {
        return on_inventory;
    }

    public int swing_readMemory(byte type, int offset, int wordLength) {
        String cmdString = ">r 00 06\r";

        byte[] pkt = cmdString.getBytes();

        write(pkt);

        return 1;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "장치 연결에 실패하였습니다.");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        SwingAPI.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
//    	mHandler.obtainMessage(MainActivity.MESSAGE_LOST).sendToTarget();

        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "장치 연결이 종료되었습니다.");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        SwingAPI.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private BluetoothServerSocket mmServerSocket = null;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mBTadapter.listenUsingRfcommWithServiceRecord(NAME, SPP_UUID);
                mmServerSocket = tmp;
            } catch (IOException e) {
                Log.e(TAG, "listen() failed: " + e.getMessage());
            }

        }

        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this);
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (mState != STATE_CONNECTED) {
                try {
                    if(mmServerSocket != null) {
                        socket = mmServerSocket.accept();
                        Log.d(TAG, "accept success");
                    } else {
                        sleep(1);
                        continue;
                    }
                } catch (IOException e) {
                    Log.e(TAG, String.format("accept() failed: %s", e.getMessage()));
                    break;
                } catch (InterruptedException e) {
                    Log.e(TAG, String.format("No bluetooth: %s", e.getMessage()));
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (SwingAPI.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                manageConnectedSocket(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                    Log.i(TAG, "mmServerSocket is closed");
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
                try {
                    sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket, because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            // Cancel discovery because it will slow down the connection
            mBTadapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "connect socket failed", e);
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (SwingAPI.this) {
                mConnectThread = null;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte mEtx;

        public boolean mResponseUpdated = false;
        public byte[] mResponse = new byte[256];
        public int mResponseLength = 0;

        public ConnectedThread(BluetoothSocket socket) {
            Log.i(TAG, "create ConnectedThread" + this);

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mEtx = 0x0A;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[256];
            int bytes = 0;
            int offset = 0;
            int pktlength = 0;
            boolean isPkt = false;
            stopThread = false;

            // Keep listening to the InputStream while connected
            while (!stopThread) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes == 0) {
                        sleep(1);
                        continue;
                    }

                    offset += mmInStream.read(buffer, offset, 1);
                    for(int i = 0; i < offset; i++) {
                        if(buffer[i] == mEtx) {
                            pktlength = i;
                            isPkt = true;
                            break;
                        }
                    }

                    if(isPkt) {
                        parse(buffer, pktlength);
                        Arrays.fill(buffer, (byte)0);
                        offset = 0;
                        pktlength = 0;
                        isPkt = false;
                    }

                    sleep(0);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                } catch (InterruptedException ie) {
                    Log.e("dsm362", ie.getMessage());
                    ;
                }
            }
            Log.i(TAG, "Stop mConnectedThread");
        }

        private boolean stopThread = false;

        public void cancel() {
            try {
                stopThread = true;
                mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

        /**
         * Write to the connected OutStream.
         //         * @param buffer  The bytes to write
         */
        public void write(byte[] outbuffer) {
            try {
                mmOutStream.write(outbuffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void write(byte singleByte) {
            try {
                mmOutStream.write(singleByte);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        private void parse(byte[] buffer, int length) {
            String str_ack = new String(buffer, 0, length);
            Log.i("dsm362", String.format("rx packet = %s", str_ack));
            parseSwing(buffer, length);
        }

        private void parseSwing(byte[] buffer, int length) {
            try {


                if (deviceName.contains("RFPrisma")) { // Scanner -> RFPrisma
                    int datalength = length - 3;
                    byte cmd = buffer[1];
                    byte[] data = new byte[datalength];
                    System.arraycopy(buffer, 3, data, 0, datalength);

                    switch (cmd) {
                        case 'e':
                            if (buffer[2] == 'T') {
                                mHandler.obtainMessage(MESSAGE_TAG, datalength, -1, data).sendToTarget();
                            }
                            break;
                        case 'R':
                            if (buffer[2] == 'e') { //Battery Status 0~4(100%)
                                mHandler.obtainMessage(MESSAGE_BATTERY, datalength, -1, data).sendToTarget();
                            }
                            if (buffer[2] == 'p') { //Power Status
                                mHandler.obtainMessage(MESSAGE_POWER, datalength, -1, data).sendToTarget();
                            }
                            if (buffer[2] == 'a') { //Power Range
                                mHandler.obtainMessage(MESSAGE_POWERRANGE, datalength, -1, data).sendToTarget();
                            }
                            break;

                        default:
                            mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, data).sendToTarget();
                            break;
                    }
                } else{ // Scanner -> Swing-U
                    int datalength = length - 3;
                    byte cmd = buffer[1];
                    byte[] data = new byte[datalength];
                    System.arraycopy(buffer, 2, data, 0, datalength);
                    switch (cmd) {
                        case 'a':
                        case 'A':
                            if (buffer[2] == '3') {
                                mHandler.obtainMessage(MESSAGE_STOP).sendToTarget();
                                on_inventory = false;
                            } else if (buffer[2] == '7') {
                                mHandler.obtainMessage(MESSAGE_FOUND).sendToTarget();
                            } else {
                                mHandler.obtainMessage(MESSAGE_START).sendToTarget();
                                on_inventory = true;
                            }
                            break;
                        case 'T':    // 'T', tag report mode
                            mHandler.obtainMessage(MESSAGE_TAG, datalength, -1, data).sendToTarget();
                            break;
                        case 'R':    // 'R', tag memory report mode
//					mHandler.obtainMessage(MainMenu.MESSAGE_READ, datalength, -1, data).sendToTarget();
                            break;
                        case 'c':    // 'c', get continuous mode
//					Log.d("dsm362", String.format("continuous mode = %s", new String(data, 0, datalength)));
//					mHandler.obtainMessage(MainMenu.MESSAGE_CONTINUOUS, datalength, -1, data).sendToTarget();
//					sync_update(buffer, length);
                            break;
                        case 's':    // 's', get buzzer volume
//					if(buffer[2] == 't') break;
//					Log.d("dsm362", String.format("buzzer volume = %s", new String(data, 0, datalength)));
//					mHandler.obtainMessage(MainMenu.MESSAGE_VOLUME, datalength, -1, data).sendToTarget();
                            sync_update(buffer, length);
                            break;
                        case 'b':    // 'b', get report mode, 0: trigger, 1: all tag
//					Log.d("dsm362", String.format("report mode = %s", new String(data, 0, datalength)));
//					mHandler.obtainMessage(MainMenu.MESSAGE_REPORT, datalength, -1, data).sendToTarget();
//					sync_update(buffer, length);
                            break;
                        case 'p':    // 'p', get power, 0, 1, 2 mode
//					Log.d("dsm362", String.format("rf atten = %s", new String(data, 0, datalength)));
//					mHandler.obtainMessage(MainMenu.MESSAGE_RFPOWER, datalength, -1, data).sendToTarget();
//					sync_update(buffer, length);
                            break;
                        case 'k':    // 'p', get power, 0, 1, 2 mode
//					Log.d("dsm362", String.format("rf th = %s", new String(data, 0, datalength)));
//					mHandler.obtainMessage(MainMenu.MESSAGE_THRESHOLD, datalength, -1, data).sendToTarget();
//					sync_update(buffer, length);
                            break;
                        case 'u':    // 'p', get power, 0, 1, 2 mode
//					Log.d("dsm362", String.format("down unit = %s", new String(data, 0, datalength)));
//					mHandler.obtainMessage(MainMenu.MESSAGE_UNIT, datalength, -1, data).sendToTarget();
//					sync_update(buffer, length);
                            break;
                        case 'v':
//					mHandler.obtainMessage(MainMenu.MESSAGE_BATTERY, datalength, -1, data).sendToTarget();
                            break;
                        case 'm':
//					String temp = new String(data, 0, datalength).trim();
//					_param_inventory_mode = InventoryMode.values()[Integer.parseInt(temp)];
//					Log.d("dsm362", String.format("mode = %d", _param_inventory_mode.ordinal()));
                            break;
                        case 'f':
                            String temp2 = new String(data, 0, datalength).trim();
                            if (temp2.contains("3")) {
//						mHandler.obtainMessage(MainMenu.MESSAGE_CLEAR, datalength, -1, data).sendToTarget();
                            } else {

                            }
                        case 'n': // 'n', get count
                            Log.d("dsm362", String.format("rf count = %s", new String(data, 0, datalength)));
//					mHandler.obtainMessage(MainMenu.MESSAGE_RFPOWER, datalength, -1, data).sendToTarget();
                            sync_update(buffer, length);
                            break;
                        default:
                            mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, data).sendToTarget();
                            break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during parseSwing", e);
            }
        }

        private void sync_update(byte[] buffer, int length) {
            System.arraycopy(buffer, 0, mResponse, 0, length);
            mResponseLength = length;
            mResponseUpdated = true;
        }

        /*
        private byte calcByte(byte origin) {
        	byte ret = 0x00;
        	if(origin < 0x0a) {
        		ret = (byte)(origin + 0x30);
        	}
        	else
        	{
        		ret = (byte)(origin + 0x37);
        	}
        	return ret;
        }

        private String byteArrayToHex(byte[] ba) {
            if (ba == null || ba.length == 0) {
                return null;
            }

            StringBuffer sb = new StringBuffer(ba.length * 2);
            String hexNumber;
            for (int x = 0; x < ba.length; x++) {
                hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

                sb.append(hexNumber.substring(hexNumber.length() - 2));
            }
            return sb.toString();
        } */
    }
}
