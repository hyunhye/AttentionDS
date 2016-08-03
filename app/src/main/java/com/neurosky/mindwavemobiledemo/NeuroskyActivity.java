package com.neurosky.mindwavemobiledemo;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.EEGPower;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This activity demonstrates how to use the constructor:
 * public TgStreamReader(BluetoothDevice mBluetoothDevice,TgStreamHandler tgStreamHandler)
 * and related functions:
 * (1) changeBluetoothDevice
 * (2) Demo of drawing ECG
 * (3) Demo of getting Bluetooth device dynamically
 * (4) setTgStreamHandler
 */
public class NeuroskyActivity extends Activity {
	private static final String TAG = NeuroskyActivity.class.getSimpleName();
	private TgStreamReader tgStreamReader;
	
	// TODO connection sdk
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private String address = null;

	private String meditation, attention, delta, theta, lowAlpha, highAlpha, lowBeta, highBeta, lowGamma, middleGamma;
	private String strPrevNow = "first";

	// getdata2
	String myJSON;
	private static final String TAG_RESULTS = "result";
	private static final String TAG_PERSON_CONDITION_ID = "person_conditionid";
	private static final String TAG_PERSON_ID = "personid";
	private static final String TAG_START_TIME = "start_time";
	private static final String TAG_CONDITION_ID = "conditionid";
	JSONArray std_place = null;

	int real_personid;
	int real_conditionid;
	int real_person_conditionid;
	String real_start_time;


	// gsr
	public final String ACTION_USB_PERMISSION = "com.neurosky.mindwavemobiledemo.USB_PERMISSION";
	UsbManager usbManager;
	UsbDevice device;
	UsbSerialDevice serialPort;
	UsbDeviceConnection connection;

	// gsr_textview
	private void tvAppend(TextView tv, CharSequence text) {
		final TextView ftv = tv;
		final CharSequence ftext = text;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ftv.append(ftext);
			}
		});
	}



	UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
		@Override
		public void onReceivedData(byte[] arg0) {
			String data = null;
			try {
				data = new String(arg0, "UTF-8");

				// 현재 시간 알아내기
				long now = System.currentTimeMillis();
				Date date = new Date(now);
				SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				String s = sdfNow.format(date);

				GSRinsertToDatabase(Integer.toString(real_person_conditionid), s, data);


				data.concat("/n");
				//tvAppend(gsr_textView,Integer.toString(real_person_conditionid));data.concat("/n");
				tvAppend(gsr_textView, data);
				//tvAppend(gsr_textView, strNow);data.concat("/n");

			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	};

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
				boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
				if (granted) {
					connection = usbManager.openDevice(device);
					serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
					if (serialPort != null) {
						if (serialPort.open()) { //Set Serial Connection Parameters.
							setUiEnabled(true);
							serialPort.setBaudRate(9600);
							serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
							serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
							serialPort.setParity(UsbSerialInterface.PARITY_NONE);
							serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
							serialPort.read(mCallback);
							tvAppend(gsr_textView,"Serial Connection Opened!\n");

						} else {
							Log.d("SERIAL", "PORT NOT OPEN");
						}
					} else {
						Log.d("SERIAL", "PORT IS NULL");
					}
				} else {
					Log.d("SERIAL", "PERM NOT GRANTED");
				}
			} else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
				//onClickStart(btn_start); // 기계 끼우면 바로 시작
			} else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) { // 기게빼면 바로 중지
				onClickStop(btn_stop);

			}
		};
	};

	public void onClickStart(View view) {

		HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
		if (!usbDevices.isEmpty()) {
			boolean keep = true;
			for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
				device = entry.getValue();
				int deviceVID = device.getVendorId();
				if (deviceVID == 0x2341)//Arduino Vendor ID
				{
					PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
					usbManager.requestPermission(device, pi);
					keep = false;
				} else {
					connection = null;
					device = null;
				}

				if (!keep)
					break;
			}
		}
	}

	public void onClickStop(View view) {
		setUiEnabled(false);
		serialPort.close();
		tvAppend(gsr_textView,"\nSerial Connection Closed! \n");

	}

	public void setUiEnabled(boolean bool) {
		btn_start.setEnabled(!bool);
		btn_stop.setEnabled(bool);
		gsr_textView.setEnabled(bool);

	}
	// gsr method end

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.bluetoothdevice_view);

		initView();
		setUpDrawWaveView();

		// gsr_permission
		usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(broadcastReceiver, filter);
		// gsr end

		try {
			// TODO	
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
				Toast.makeText(
						this,
						"Please enable your Bluetooth and re-run this program !",
						Toast.LENGTH_LONG).show();
				finish();
//				return;
			}  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.i(TAG, "error:" + e.getMessage());
			return;
		}
	}

	private TextView tv_ps = null;
	private TextView tv_attention = null;
	private TextView tv_meditation = null;
	private TextView tv_delta = null;
	private TextView tv_theta = null;
	private TextView tv_lowalpha = null;
	
	private TextView  tv_highalpha = null;
	private TextView  tv_lowbeta = null;
	private TextView  tv_highbeta = null;
	
	private TextView  tv_lowgamma = null;
	private TextView  tv_middlegamma  = null;
	private TextView  tv_badpacket = null;
	
	private Button btn_start = null;
	private Button btn_stop = null;
	private Button btn_selectdevice = null;
	private LinearLayout wave_layout;

	private TextView gsr_textView;

	private int badPacketCount = 0;

	private void initView() {
		tv_ps = (TextView) findViewById(R.id.tv_ps);
		tv_attention = (TextView) findViewById(R.id.tv_attention);
		tv_meditation = (TextView) findViewById(R.id.tv_meditation);
		tv_delta = (TextView) findViewById(R.id.tv_delta);
		tv_theta = (TextView) findViewById(R.id.tv_theta);
		tv_lowalpha = (TextView) findViewById(R.id.tv_lowalpha);
		
		tv_highalpha = (TextView) findViewById(R.id.tv_highalpha);
		tv_lowbeta= (TextView) findViewById(R.id.tv_lowbeta);
		tv_highbeta= (TextView) findViewById(R.id.tv_highbeta);
		
		tv_lowgamma = (TextView) findViewById(R.id.tv_lowgamma);
		tv_middlegamma= (TextView) findViewById(R.id.tv_middlegamma);
		tv_badpacket = (TextView) findViewById(R.id.tv_badpacket);
		
		
		btn_start = (Button) findViewById(R.id.btn_start);
		btn_stop = (Button) findViewById(R.id.btn_stop);
		wave_layout = (LinearLayout) findViewById(R.id.wave_layout);

		gsr_textView = (TextView) findViewById(R.id.gsr_textView);

		btn_start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				// neuro start
				SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
				real_personid = pref.getInt("personid",0);
				real_start_time = pref.getString("now","");
				real_conditionid = pref.getInt("conditionid",0);

				getData("http://14.63.214.221/person_condition_get.php");

				badPacketCount = 0;
				showToast("connecting ...",Toast.LENGTH_SHORT);
				start();

				// gsr start
				HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();

				if (!usbDevices.isEmpty()) {
					boolean keep = true;
					for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
						device = entry.getValue();
						int deviceVID = device.getVendorId();
						if (deviceVID == 0x2341)//Arduino Vendor ID
						{
							PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
							usbManager.requestPermission(device, pi);
							keep = false;
						} else {
							connection = null;
							device = null;
						}

						if (!keep)
							break;
					}
				}

			}
		});

		btn_stop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(tgStreamReader != null){
					tgStreamReader.stop();
				}
				// gsr stop and clear
				onClickStop(btn_stop);
				gsr_textView.setText(" ");
			}

		});
		
		btn_selectdevice =  (Button) findViewById(R.id.btn_selectdevice);
		
		btn_selectdevice.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				scanDevice();
			}

		});
	}
	
	private void start(){
		if(address != null){
			BluetoothDevice bd = mBluetoothAdapter.getRemoteDevice(address);
			createStreamReader(bd);

			tgStreamReader.connectAndStart();
		}else{
			showToast("Please select device first!", Toast.LENGTH_SHORT);
		}
	}

	public void stop() {
		if(tgStreamReader != null){
			tgStreamReader.stop();
			tgStreamReader.close();//if there is not stop cmd, please call close() or the data will accumulate 
			tgStreamReader = null;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if(tgStreamReader != null){
			tgStreamReader.close();
			tgStreamReader = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		stop();
	}

	// TODO view
	DrawWaveView waveView = null;
	// (2) demo of drawing ECG, set up of view
	public void setUpDrawWaveView() {
		
		waveView = new DrawWaveView(getApplicationContext());
		wave_layout.addView(waveView, new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		waveView.setValue(2048, 2048, -2048);
	}
	// (2) demo of drawing ECG, update view
	public void updateWaveView(int data) {
		if (waveView != null) {
			waveView.updateData(data);
		}
	}
	private int currentState = 0;
	private TgStreamHandler callback = new TgStreamHandler() {

		@Override
		public void onStatesChanged(int connectionStates) {
			// TODO Auto-generated method stub
			Log.d(TAG, "connectionStates change to: " + connectionStates);
			currentState  = connectionStates;
			switch (connectionStates) {
			case ConnectionStates.STATE_CONNECTED:
				//sensor.start();
				showToast("Connected", Toast.LENGTH_SHORT);
				break;
			case ConnectionStates.STATE_WORKING:
				//byte[] cmd = new byte[1];
				//cmd[0] = 's';
				//tgStreamReader.sendCommandtoDevice(cmd);
				
				break;
			case ConnectionStates.STATE_GET_DATA_TIME_OUT:
				//get data time out
				break;
			case ConnectionStates.STATE_COMPLETE:
				//read file complete
				break;
			case ConnectionStates.STATE_STOPPED:
				break;
			case ConnectionStates.STATE_DISCONNECTED:
				break;
			case ConnectionStates.STATE_ERROR:
				Log.d(TAG,"Connect error, Please try again!");
				break;
			case ConnectionStates.STATE_FAILED:
				Log.d(TAG,"Connect failed, Please try again!");
				break;
			}
			Message msg = LinkDetectedHandler.obtainMessage();
			msg.what = MSG_UPDATE_STATE;
			msg.arg1 = connectionStates;
			LinkDetectedHandler.sendMessage(msg);
			

		}

		@Override
		public void onRecordFail(int a) {
			// TODO Auto-generated method stub
			Log.e(TAG,"onRecordFail: " +a);

		}

		@Override
		public void onChecksumFail(byte[] payload, int length, int checksum) {
			// TODO Auto-generated method stub
			
			badPacketCount ++;
			Message msg = LinkDetectedHandler.obtainMessage();
			msg.what = MSG_UPDATE_BAD_PACKET;
			msg.arg1 = badPacketCount;
			LinkDetectedHandler.sendMessage(msg);

		}

		@Override
		public void onDataReceived(int datatype, int data, Object obj) {
			// TODO Auto-generated method stub
			Message msg = LinkDetectedHandler.obtainMessage();
			msg.what = datatype;
			msg.arg1 = data;
			msg.obj = obj;
			LinkDetectedHandler.sendMessage(msg);
			//Log.i(TAG,"onDataReceived");
		}

	};

	private boolean isPressing = false;
	private static final int MSG_UPDATE_BAD_PACKET = 1001;
	private static final int MSG_UPDATE_STATE = 1002;
	private static final int MSG_CONNECT = 1003;


	int raw;
	private Handler LinkDetectedHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// String meditation, attention, delta, theta, lowAlpha, highAlpha, lowBeta, highBeta, lowGamma, middleGamma;
			switch (msg.what) {
			case MindDataType.CODE_RAW:
					updateWaveView(msg.arg1);
				break;
			case MindDataType.CODE_MEDITATION:
				Log.d(TAG, "HeadDataType.CODE_MEDITATION " + msg.arg1);
				tv_meditation.setText("" +msg.arg1 );
				meditation = Integer.toString(msg.arg1);
				break;
			case MindDataType.CODE_ATTENTION:
				Log.d(TAG, "CODE_ATTENTION " + msg.arg1);
				tv_attention.setText("" +msg.arg1);
				attention = Integer.toString(msg.arg1);
				break;
			case MindDataType.CODE_EEGPOWER:
				EEGPower power = (EEGPower)msg.obj;
				if(power.isValidate()){
					tv_delta.setText("" +power.delta);
					tv_theta.setText("" +power.theta);
					tv_lowalpha.setText("" +power.lowAlpha);
					tv_highalpha.setText("" +power.highAlpha);
					tv_lowbeta.setText("" +power.lowBeta);
					tv_highbeta.setText("" +power.highBeta);
					tv_lowgamma.setText("" +power.lowGamma);
					tv_middlegamma.setText("" +power.middleGamma);

					delta = Integer.toString(power.delta);
					theta = Integer.toString(power.theta);
					lowAlpha = Integer.toString(power.lowAlpha);
					highAlpha = Integer.toString(power.highAlpha);
					lowBeta = Integer.toString(power.lowBeta);
					highBeta = Integer.toString(power.highBeta);
					lowGamma = Integer.toString(power.lowGamma);
					middleGamma = Integer.toString(power.middleGamma);
				}
				break;
			case MindDataType.CODE_POOR_SIGNAL://
				int poorSignal = msg.arg1;
				Log.d(TAG, "poorSignal:" + poorSignal);
				tv_ps.setText(""+msg.arg1);

				break;
			case MSG_UPDATE_BAD_PACKET:
				tv_badpacket.setText("" + msg.arg1);
				
				break;
			default:
				break;
			}

			long now = System.currentTimeMillis();
			Date date = new Date(now);
			SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			String strNow = sdfNow.format(date);
			if(!strPrevNow.equals(strNow)) {
				Log.d("hyunhye",Integer.toString(real_person_conditionid)+":"+strNow);
				insertToDatabase(Integer.toString(real_person_conditionid), strNow, meditation, attention, delta, theta, lowAlpha, highAlpha, lowBeta, highBeta, lowGamma, middleGamma);
				strPrevNow = strNow;
			}
			super.handleMessage(msg);
		}
	};
	
	
	public void showToast(final String msg,final int timeStyle){
		NeuroskyActivity.this.runOnUiThread(new Runnable()
        {    
            public void run()    
            {    
            	Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }    
    
        });  
	}
	
	//show device list while scanning
	private ListView list_select;
	private BTDeviceListAdapter deviceListApapter = null;
	private Dialog selectDialog;
	
	// (3) Demo of getting Bluetooth device dynamically
    public void scanDevice(){

    	if(mBluetoothAdapter.isDiscovering()){
    		mBluetoothAdapter.cancelDiscovery();
    	}
    	
    	setUpDeviceListView();
    	//register the receiver for scanning
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);
    	
    	mBluetoothAdapter.startDiscovery();
    }
    
 private void setUpDeviceListView(){
    	
    	LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_select_device, null);
		list_select = (ListView) view.findViewById(R.id.list_select);
		selectDialog = new Dialog(this, R.style.dialog1);
		selectDialog.setContentView(view);
    	//List device dialog

    	deviceListApapter = new BTDeviceListAdapter(this);
    	list_select.setAdapter(deviceListApapter);
    	list_select.setOnItemClickListener(selectDeviceItemClickListener);
    	
    	selectDialog.setOnCancelListener(new OnCancelListener(){

			@Override
			public void onCancel(DialogInterface arg0) {
				// TODO Auto-generated method stub
				Log.e(TAG,"onCancel called!");
				NeuroskyActivity.this.unregisterReceiver(mReceiver);
			}
    		
    	});
    	
    	selectDialog.show();
    	
    	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    	for(BluetoothDevice device: pairedDevices){
    		deviceListApapter.addDevice(device);
    	}
		deviceListApapter.notifyDataSetChanged();
    }
 
 //Select device operation
 private OnItemClickListener selectDeviceItemClickListener = new OnItemClickListener(){
	 
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1,int arg2, long arg3) {
			// TODO Auto-generated method stub
			Log.d(TAG, "Rico ####  list_select onItemClick     ");
	    	if(mBluetoothAdapter.isDiscovering()){
	    		mBluetoothAdapter.cancelDiscovery();
	    	}
	    	//unregister receiver
	    	NeuroskyActivity.this.unregisterReceiver(mReceiver);

	    	mBluetoothDevice =deviceListApapter.getDevice(arg2);
	    	selectDialog.dismiss();
	    	selectDialog = null;
	    	
			Log.d(TAG,"onItemClick name: "+mBluetoothDevice.getName() + " , address: " + mBluetoothDevice.getAddress() );
			address = mBluetoothDevice.getAddress().toString();
			
			//ger remote device
			BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mBluetoothDevice.getAddress().toString());
         
			//bind and connect
			//bindToDevice(remoteDevice); // create bond works unstable on Samsung S5
			//showToast("pairing ...",Toast.LENGTH_SHORT);

			tgStreamReader = createStreamReader(remoteDevice); 
			tgStreamReader.connectAndStart();
		
		}
	
 };
 
 /**
	 * If the TgStreamReader is created, just change the bluetooth
	 * else create TgStreamReader, set data receiver, TgStreamHandler and parser
	 * @param bd
	 * @return TgStreamReader
	 */
	public TgStreamReader createStreamReader(BluetoothDevice bd){

		if(tgStreamReader == null){
			// Example of constructor public TgStreamReader(BluetoothDevice mBluetoothDevice,TgStreamHandler tgStreamHandler)
			tgStreamReader = new TgStreamReader(bd,callback);
			tgStreamReader.startLog();
		}else{
			// (1) Demo of changeBluetoothDevice
			tgStreamReader.changeBluetoothDevice(bd);
			
			// (4) Demo of setTgStreamHandler, you can change the data handler by this function
			tgStreamReader.setTgStreamHandler(callback);
		}
		return tgStreamReader;
	}
 
 /**
  * Check whether the given device is bonded, if not, bond it 
  * @param bd
  */
 public void bindToDevice(BluetoothDevice bd){
 	    int ispaired = 0;
		if(bd.getBondState() != BluetoothDevice.BOND_BONDED){
			//ispaired = remoteDevice.createBond();
			try {
				//Set pin
				if(Utils.autoBond(bd.getClass(), bd, "0000")){
					ispaired += 1;
				}
				//bind to device
				if(Utils.createBond(bd.getClass(), bd)){
					ispaired += 2;
				}
				Method createCancelMethod=BluetoothDevice.class.getMethod("cancelBondProcess");
                boolean bool=(Boolean)createCancelMethod.invoke(bd);
                Log.d(TAG,"bool="+bool);
					
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.d(TAG, " paire device Exception:    " + e.toString());	
			}
		}
		Log.d(TAG, " ispaired:    " + ispaired);	

 }
 
//The BroadcastReceiver that listens for discovered devices 
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
				Log.d(TAG, "mReceiver()");
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.d(TAG,"mReceiver found device: " + device.getName());
				
				// update to UI
				deviceListApapter.addDevice(device);
				deviceListApapter.notifyDataSetChanged();

			} 
		}
	};

	// 서버에 저장하는 함수
	private void insertToDatabase(String stdplaceid, String time, String meditation, String attention, String delta, String theta, String lowAlpha, String highAlpha,
								  String lowBeta, String highBeta, String lowGamma, String middleGamma){
		class InsertData extends AsyncTask<String,Void, String> {
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}
			@Override
			protected void onPostExecute(String s) {
				super.onPostExecute(s);
			}
			@Override
			protected String doInBackground(String... params) {
				try{
					String stdplaceid = (String)params[0];
					String time = (String)params[1];
					String meditation = (String)params[2];
					String attention = (String)params[3];
					String delta = (String)params[4];
					String theta = (String)params[5];
					String lowAlpha = (String)params[6];
					String highAlpha = (String)params[7];
					String lowBeta = (String)params[8];
					String highBeta = (String)params[9];
					String lowGamma = (String)params[10];
					String middleGamma = (String)params[11];

					String link="http://14.63.214.221/neurosky_insert.php";
					String data  = URLEncoder.encode("stdplaceid", "UTF-8") + "=" + URLEncoder.encode(stdplaceid, "UTF-8");
					data += "&" + URLEncoder.encode("time", "UTF-8") + "=" + URLEncoder.encode(time, "UTF-8");
					data += "&" + URLEncoder.encode("meditation", "UTF-8") + "=" + URLEncoder.encode(meditation, "UTF-8");
					data += "&" + URLEncoder.encode("attention", "UTF-8") + "=" + URLEncoder.encode(attention, "UTF-8");
					data += "&" + URLEncoder.encode("delta", "UTF-8") + "=" + URLEncoder.encode(delta, "UTF-8");
					data += "&" + URLEncoder.encode("theta", "UTF-8") + "=" + URLEncoder.encode(theta, "UTF-8");
					data += "&" + URLEncoder.encode("lowAlpha", "UTF-8") + "=" + URLEncoder.encode(lowAlpha, "UTF-8");
					data += "&" + URLEncoder.encode("highAlpha", "UTF-8") + "=" + URLEncoder.encode(highAlpha, "UTF-8");
					data += "&" + URLEncoder.encode("lowBeta", "UTF-8") + "=" + URLEncoder.encode(lowBeta, "UTF-8");
					data += "&" + URLEncoder.encode("highBeta", "UTF-8") + "=" + URLEncoder.encode(highBeta, "UTF-8");
					data += "&" + URLEncoder.encode("lowGamma", "UTF-8") + "=" + URLEncoder.encode(lowGamma, "UTF-8");
					data += "&" + URLEncoder.encode("middleGamma", "UTF-8") + "=" + URLEncoder.encode(middleGamma, "UTF-8");

					Log.d("attention",attention);
					URL url = new URL(link);
					URLConnection conn = url.openConnection();
					conn.setDoOutput(true);
					OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

					wr.write(data);
					wr.flush();

					BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

					StringBuilder sb = new StringBuilder();
					String line = null;

					// Read Server Response
					while((line = reader.readLine()) != null)
					{
						sb.append(line);
						break;
					}
					return sb.toString();
				}
				catch(Exception e){
					return new String("Exception: " + e.getMessage());
				}
			}
		}
		InsertData task = new InsertData();
		task.execute(stdplaceid, time, meditation, attention, delta, theta, lowAlpha, highAlpha, lowBeta, highBeta, lowGamma, middleGamma);
	}

	private void checkPersonCondition(){
		try {
			JSONObject jsonObj = new JSONObject(myJSON);
			std_place = jsonObj.getJSONArray(TAG_RESULTS);

			for (int i = 0; i < std_place.length(); i++) {
				JSONObject c = std_place.getJSONObject(i);
				int json_person_condition_id = c.getInt(TAG_PERSON_CONDITION_ID);
				int json_personid = c.getInt(TAG_PERSON_ID);
				int json_conditionid = c.getInt(TAG_CONDITION_ID);
				String json_strNow = c.getString(TAG_START_TIME);

				Log.d("hyunhye",json_personid +","+ real_personid +","+ json_conditionid +","+ real_conditionid +","+ real_start_time +","+ json_strNow);
				if(json_personid == real_personid && json_conditionid == real_conditionid && real_start_time.equals(json_strNow)){
					real_person_conditionid = json_person_condition_id;

					SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
					SharedPreferences.Editor editor = pref.edit();
					editor.putInt("person_condition_id", real_person_conditionid);
					editor.commit();
				}
			}
		}catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void getData(String url) {
		class GetDataJSON extends AsyncTask<String, Void, String> {
			@Override
			protected String doInBackground(String... params) {
				String uri = params[0];

				BufferedReader bufferedReader = null;
				try {
					URL url = new URL(uri);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					StringBuilder sb = new StringBuilder();

					bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

					String json;
					while ((json = bufferedReader.readLine()) != null) {
						sb.append(json + "\n");
					}
					return sb.toString().trim();

				} catch (Exception e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(String result) {
				myJSON = result;
				checkPersonCondition();
			}
		}
		GetDataJSON g = new GetDataJSON();
		g.execute(url);
	}


	// 서버에 저장하는 함수_gsr
	private void GSRinsertToDatabase(String person_conditionid, String time, String gsrdata){
		class InsertData extends AsyncTask<String,Void, String> {
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
			}
			@Override
			protected void onPostExecute(String s) {
				super.onPostExecute(s);
			}
			@Override
			protected String doInBackground(String... params) {
				try{
					String person_conditionid = (String)params[0];
					String time = (String)params[1];
					String gsrdata = (String)params[2];


					String link="http://14.63.214.221/gsr_insert.php";
					String data  = URLEncoder.encode("person_conditionid", "UTF-8") + "=" + URLEncoder.encode(person_conditionid, "UTF-8");
					data += "&" + URLEncoder.encode("time", "UTF-8") + "=" + URLEncoder.encode(time, "UTF-8");
					data += "&" + URLEncoder.encode("gsrdata", "UTF-8") + "=" + URLEncoder.encode(gsrdata, "UTF-8");


					Log.d("attention",attention);
					URL url = new URL(link);
					URLConnection conn = url.openConnection();
					conn.setDoOutput(true);
					OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

					wr.write(data);
					wr.flush();

					BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

					StringBuilder sb = new StringBuilder();
					String line = null;

					// Read Server Response
					while((line = reader.readLine()) != null)
					{
						sb.append(line);
						break;
					}
					return sb.toString();
				}
				catch(Exception e){
					return new String("Exception: " + e.getMessage());
				}
			}
		}
		InsertData task = new InsertData();
		task.execute(person_conditionid, time, gsrdata);
	}

}
