/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.ECG_PIC;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.androidplot.Plot.BorderStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.example.android.ECG1.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

/**
 * The actual activity where the plotting takes place
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;


    // declare for save and email
    static String saveAdd;
    
    // Declare variables for graph
    static int mIntGraph;
    static int changeIntValue;
    static Number mByteGraph = 0;
    static String mStringGraph;
    static String mStringByteGraph;
    static String myInt;
    static float mFloatGraph = 1;
    // Declare Plotting Data Values and variables
    private static final int HISTORY_SIZE = 30;            // asdfnumber of points to plot in history
	private XYPlot mySimpleXYPlot;
	private SimpleXYSeries sensorHistorySeries = null;
		private LinkedList<Integer> sensorHistory;
		private LineAndPointFormatter colorbmpSeries;   // check code purpose
		float v = 0;
		
	 {
	        sensorHistory = new LinkedList<Integer>();
	        sensorHistorySeries = new SimpleXYSeries("ECG");
	        sensorHistorySeries.useImplicitXVals();
	    }
	 
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
 
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up the window layout
        setContentView(R.layout.main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

            // Drawing the Graph.
     		// can be used to add shades inside the plot
     		// -------------------------------------- // 
     		mySimpleXYPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
            mySimpleXYPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);
            mySimpleXYPlot.getGraphWidget().getBackgroundPaint().setColor(Color.BLACK);
    		mySimpleXYPlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.BLACK);
    		mySimpleXYPlot.getGraphWidget().getRangeGridLinePaint().setColor(Color.TRANSPARENT);
    		mySimpleXYPlot.getGraphWidget().getDomainGridLinePaint().setColor(Color.TRANSPARENT);
    		mySimpleXYPlot.setPlotMargins(0, 0, 0, 0);
            mySimpleXYPlot.setPlotPadding(0, 0, 0, 0);
            mySimpleXYPlot.setBorderStyle(BorderStyle.SQUARE, null, null);
    		//---------------------------------------------------------------------------------------//
     		mySimpleXYPlot.setRangeBoundaries(-150, 150, BoundaryMode.FIXED);
     		mySimpleXYPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
     		mySimpleXYPlot.addSeries(sensorHistorySeries,
                    new LineAndPointFormatter(
                            Color.rgb(100, 100, 200), null, null, null));
     		mySimpleXYPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
     		mySimpleXYPlot.setDomainStepValue(HISTORY_SIZE/10);
     		mySimpleXYPlot.setRangeStepValue(1.5f);
     		mySimpleXYPlot.setTicksPerRangeLabel(3);
     		mySimpleXYPlot.setDomainLabel("HR");
     		mySimpleXYPlot.getDomainLabelWidget().pack();
     		mySimpleXYPlot.setRangeLabel("R to R");
     		mySimpleXYPlot.getRangeLabelWidget().pack();
     		mySimpleXYPlot.setRangeValueFormat(new DecimalFormat("#"));
     		mySimpleXYPlot.setDomainValueFormat(new DecimalFormat("#"));
   
             
     		//---------------------------- Screen Shot -------------------//
             Button but = (Button) findViewById(R.id.button1);
             but.setOnClickListener(new OnClickListener() {
                 
                 @Override
                 public void onClick(View v) {
                 	 Bitmap bitmap = takeScreenshot();
                      saveBitmap(bitmap);
                 }
             });
             //----------------------------------End-------------------------//
    }

    /* After new screen(Activity) is launched bluetooth connection needs to be reInitialized
     * So this is being done here */
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();   
            }
        }  
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
       
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
  

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

  
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
     //   if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
      //  if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }



    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                mStringByteGraph = bytes2String(readBuf, 1);
        
                // construct a string from the valid bytes in the buffer
                mStringGraph = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.clear();
                mConversationArrayAdapter.add("ECG Device"+":  " + mStringGraph);  
                
                try {
                	 mIntGraph = Integer.parseInt(mStringByteGraph, 10);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					mIntGraph = 0; // your default value
				}
                
                
                   try {		   
					if (sensorHistory.size() > HISTORY_SIZE) {
					    
					    //sensorHistory.removeAll(sensorHistory);
					    sensorHistory.removeFirst();
					    sensorHistory.removeFirst();
					}
					if(mIntGraph <= 120)
					{
						changeIntValue = -(mIntGraph);
						sensorHistory.addLast(changeIntValue);
					}else {
						changeIntValue = mIntGraph-120;
						sensorHistory.addLast(changeIntValue);
					}
					sensorHistorySeries.setModel(sensorHistory, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
					mySimpleXYPlot.redraw();
				} catch (Exception e) {
					e.printStackTrace();
					mChatService.stop();
			        if(D) Log.e(TAG, "--- Crashed while Plotting ---");
				}
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    /* Method is called as a callback mechanism, it is called after the bluetooth connection is
     * successful or unsuccessful and control returns back to the activity */
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /* Method which makes actual connection */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address 20:13:07:18:34:67 ::ME
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }
 // Convert Byte Arrary to Hex String
   
    public static String bytes2String(byte[] b, int count) {
        String ret =  "";
        //String str ="";
        for (int i = 0; i < count; i++) {
            myInt = Integer.toString((int)(b[i] & 0xFF));                                          
        }
        return myInt;
    }

    /* Method responsible for creating actionbar menu (Buttons in actionbar)*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    /* Method handles touch events of buttons present in the action bar */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

    /* Method copies the root layout and converts it into Bitmap
     * Thus acting as a pseudo screenshot */
    public Bitmap takeScreenshot() {
  	   View rootView = findViewById(android.R.id.content).getRootView();
  	   rootView.setDrawingCacheEnabled(true);
  	   return rootView.getDrawingCache();
  	}

    /* Saving bitmap to SD Card */
	 public void saveBitmap(Bitmap bitmap) {
 		 
	 		// date format for filename
				SimpleDateFormat formatter1 = new SimpleDateFormat("ddMMMyy");
				// get date
				Date now = new Date();
				// get/make file directory
				File dir = new File("/sdcard/ECGimages/");
				dir.mkdirs();
				// check to see if file exists
				// if doesnt exist create file,
				// if does exist increment filename couter
				// filename in format ddMMMyyii.txt
				File file = new File("default.png");
				for (int i = 0; i < 100; i++) {
					String count = String.format("%02d", i);
					String filename = formatter1.format(now) + count + ".png";
					saveAdd = filename;
					file = new File(dir, filename);
					if (!file.exists())
						break;
				}
	 	    //File imagePath = new File(Environment.getExternalStorageDirectory() + "/screenshot.png");
	 	    FileOutputStream fos;
	 	    try {
	 	        fos = new FileOutputStream(file);
	 	        bitmap.compress(CompressFormat.JPEG, 100, fos);
	 	        fos.flush();
	 	        fos.close();
	 	    } catch (FileNotFoundException e) {
	 	        Log.e("GREC", e.getMessage(), e);
	 	    } catch (IOException e) {
	 	        Log.e("GREC", e.getMessage(), e);
	 	    }
				sendEmail("file:///sdcard/ECGimages/"+saveAdd);

	 	}

        /* Calling Implicit intent to send email */
	 	 private void sendEmail(String imagePath){
	  	    
	  	    Intent emailIntent = new Intent(Intent.ACTION_SEND); 
	          Uri U=Uri.parse(imagePath);
	          emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {  });
	          emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, " from ..");
	          emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "from Student ");
	          emailIntent.setType("image/png");
//	        emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,U);
	          emailIntent.putExtra(Intent.EXTRA_STREAM, U);
	          startActivity(Intent.createChooser(emailIntent, ""));  
	 		 
	 	 }

}
