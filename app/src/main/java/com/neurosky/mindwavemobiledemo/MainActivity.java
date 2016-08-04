package com.neurosky.mindwavemobiledemo;


import com.neurosky.connection.TgStreamReader;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * This activity is the man entry of this app. It demonstrates the usage of 
 * (1) TgStreamReader.redirectConsoleLogToDocumentFolder()
 * (2) TgStreamReader.stopConsoleLog()
 * (3) demo of getVersion
 */
public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private Button btn_device, main_graph_btn;
	//private Button main_place_ok_btn;
	private TextView main_identification_tv, main_condition_tv;
	private Spinner main_bpm_spinner, main_color_spinner;

	int real_identification;
	int real_personid;
	int real_conditionid;
	String strNow;
	String name;

	private String bpm, color;

	// getdata
	String myJSON;
	private static final String TAG_RESULTS = "result";
	private static final String TAG_BPM = "bpm";
	private static final String TAG_COLOR = "color";
	private static final String TAG_CONDITION_ID = "conditionid";
	JSONArray conditions = null;

	private static final int UART_PROFILE_CONNECTED = 20;
	private static final int UART_PROFILE_DISCONNECTED = 21;
	private int mState = UART_PROFILE_DISCONNECTED;

	private String[] navItems = {"Logout"};
	private ListView lvNavList;
	private FrameLayout flContainer;

	private DrawerLayout dlDrawer;
	private ActionBarDrawerToggle dtToggle;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.profile_actionbar));
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setIcon(
				new ColorDrawable(getResources().getColor(android.R.color.transparent)));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			getWindow().setStatusBarColor(getResources().getColor(R.color.pink_color_dark));
		}

		setContentView(R.layout.activity_main);


		initView();

		SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
		name = pref.getString("name","NONE");
		real_personid = pref.getInt("personid",0);
		main_identification_tv.setText(name);


		/*
		main_place_ok_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getData("http://14.63.214.221/place_get.php");
			}
		});
		*/

		ArrayAdapter adapter_bpm = ArrayAdapter.createFromResource(this, R.array.bpm, android.R.layout.simple_spinner_item);
		adapter_bpm.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		main_bpm_spinner.setAdapter(adapter_bpm);

		ArrayAdapter adapter_color = ArrayAdapter.createFromResource(this, R.array.color, android.R.layout.simple_spinner_item);
		adapter_color.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		main_color_spinner.setAdapter(adapter_color);

		main_bpm_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switch(position) {
					case 0:
						bpm = "90-110";
						main_condition_tv.setText(bpm + "  :  " + color);
						break;
					case 1:
						bpm = "110-130";
						main_condition_tv.setText(bpm + "  :  " + color);
						break;
					case 2:
						bpm = "130-150";
						main_condition_tv.setText(bpm + "  :  " + color);
						break;
					case 3:
						bpm = "150-170";
						main_condition_tv.setText(bpm + "  :  " + color);
						break;
				}
				getData("http://14.63.214.221/condition_get.php");
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		main_color_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switch(position){
					case 0:
						color = "white";
						main_condition_tv.setText(bpm +"  :  "+color);
						break;
					case 1:
						color = "black";
						main_condition_tv.setText(bpm +"  :  "+color);
						break;
					case 2:
						color = "blue";
						main_condition_tv.setText(bpm +"  :  "+color);
						break;
					case 3:
						color = "red";
						main_condition_tv.setText(bpm +"  :  "+color);
						break;

				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});


		btn_device.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				long now = System.currentTimeMillis();
				Date date = new Date(now);
				SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				strNow = sdfNow.format(date);
				bpm = main_bpm_spinner.getSelectedItem().toString();
				color = main_color_spinner.getSelectedItem().toString();

				SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
				SharedPreferences.Editor editor = pref.edit();
				editor.putString("now",strNow);
				editor.putString("bpm",bpm);
				editor.putString("color",color);
				editor.commit();

				insertToDatabase(Integer.toString(real_personid),Integer.toString(real_conditionid),strNow);

				Intent intent = new Intent(MainActivity.this,NeuroskyActivity.class);
				startActivity(intent);
			}
		});

		main_graph_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, GraphActivity.class);
				startActivity(intent);
			}
		});

		TgStreamReader.redirectConsoleLogToDocumentFolder();

		lvNavList = (ListView)findViewById(R.id.lv_activity_main_nav_list);
		flContainer = (FrameLayout)findViewById(R.id.fl_activity_main_container);

		lvNavList.setAdapter(
				new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, navItems));
		lvNavList.setOnItemClickListener(new DrawerItemClickListener());

		dlDrawer = (DrawerLayout)findViewById(R.id.dl_activity_main_drawer);
		dtToggle = new ActionBarDrawerToggle(this, dlDrawer,
				R.drawable.drawer, R.string.open_drawer, R.string.close_drawer){

			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
			}

		};
		dlDrawer.setDrawerListener(dtToggle);
	}

	private void initView() {
		btn_device = (Button) findViewById(R.id.btn_device);
		main_identification_tv = (TextView) findViewById(R.id.main_identification_tv);
		main_bpm_spinner = (Spinner) findViewById(R.id.main_bpm_spinner);
		main_color_spinner = (Spinner) findViewById(R.id.main_color_spinner);
		//main_place_ok_btn = (Button) findViewById(R.id.main_place_ok_btn);
		main_graph_btn = (Button) findViewById(R.id.main_graph_btn);
		main_condition_tv = (TextView)findViewById(R.id.main_condition_tv);
	}

	@Override
	protected void onDestroy() {
		TgStreamReader.stopConsoleLog();
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
	}


	// 서버에 저장하는 함수
	private void insertToDatabase(String personid, String conditionid, String time){
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
					String personid = (String)params[0];
					String conditionid = (String)params[1];
					String time = (String)params[2];


					String link="http://14.63.214.221/person_condition_insert.php";
					String data  = URLEncoder.encode("personid", "UTF-8") + "=" + URLEncoder.encode(personid, "UTF-8");
					data += "&" + URLEncoder.encode("conditionid", "UTF-8") + "=" + URLEncoder.encode(conditionid, "UTF-8");
					data += "&" + URLEncoder.encode("time", "UTF-8") + "=" + URLEncoder.encode(time, "UTF-8");

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
		task.execute(personid, conditionid, time);
	}

	private void checkCondition(){
		try {
			JSONObject jsonObj = new JSONObject(myJSON);
			conditions = jsonObj.getJSONArray(TAG_RESULTS);

			for (int i = 0; i < conditions.length(); i++) {
				JSONObject c = conditions.getJSONObject(i);
				int id = c.getInt(TAG_CONDITION_ID);
				String s_bpm = c.getString(TAG_BPM);
				String s_color = c.getString(TAG_COLOR);

				Log.d("hyunhye_condition",s_bpm+" "+bpm+" "+s_color+" "+color);
				if(s_bpm.equals(bpm) && s_color.equals(color)){
					real_conditionid = id;

					SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
					SharedPreferences.Editor editor = pref.edit();
					editor.putInt("conditionid",real_conditionid);
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
				checkCondition();
			}
		}
		GetDataJSON g = new GetDataJSON();
		g.execute(url);
	}

	@Override
	public void onBackPressed() {
		if (mState == UART_PROFILE_CONNECTED) {
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
			showMessage("nRFUART's running in background.\n             Disconnect to exit");
		}
		else {
			new AlertDialog.Builder(this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.popup_title)
					.setMessage(R.string.popup_message)
					.setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which) {
							LoginActivity loginActivity = (LoginActivity) LoginActivity.LoginActivity;
							loginActivity.finish();
							finish();
						}
					})
					.setNegativeButton(R.string.popup_no, null)
					.show();
		}
	}
	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	protected void onPostCreate(Bundle savedInstanceState){
		super.onPostCreate(savedInstanceState);
		dtToggle.syncState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(dtToggle.onOptionsItemSelected(item)){
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		dtToggle.onConfigurationChanged(newConfig);
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener{

		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position,
								long id) {
			switch(position){
				case 0:
					try{
						SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
						SharedPreferences.Editor editor = pref.edit();
						editor.putBoolean("autoLogin", false);
						editor.commit();
						Intent intent = new Intent(MainActivity.this, LoginActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
					}catch (NullPointerException e){
						SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
						SharedPreferences.Editor editor = pref.edit();
						editor.putBoolean("autoLogin", false);
						editor.commit();
						Intent intent = new Intent(MainActivity.this, LoginActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
					}
					break;
			}
			dlDrawer.closeDrawer(lvNavList);
		}
	}
}