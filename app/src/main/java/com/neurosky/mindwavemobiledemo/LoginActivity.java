package com.neurosky.mindwavemobiledemo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends Activity {

    EditText login_identification_et;
    CheckBox login_checkBox;
    Button login_btn,login_sign_up_btn;
    public Boolean loginChecked = false;

    // getdata
    String myJSON;
    private static final String TAG_RESULTS = "result";
    private static final String TAG_IDENTIFICATION = "identification";
    private static final String TAG_ID = "studentid";
    private static final String TAG_NAME = "name";
    JSONArray myprofile = null;

    public static Activity LoginActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.pink_color_dark));
        }
        setContentView(R.layout.activity_login);

        LoginActivity = LoginActivity.this;

        login_identification_et = (EditText) findViewById(R.id.login_identification_et);
        login_checkBox = (CheckBox) findViewById(R.id.login_checkBox);
        login_btn = (Button) findViewById(R.id.login_btn);
        login_sign_up_btn = (Button) findViewById(R.id.login_sign_up_btn);

        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        try{
            Log.d("here",pref.getBoolean("autoLogin",false)+"");
            if(pref.getBoolean("autoLogin",false)){
                LoginActivity = LoginActivity.this;
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }catch (NullPointerException e){
            LoginActivity = LoginActivity.this;
        }
       login_btn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               getData("http://14.63.214.221/student_get.php");
           }
       });

        login_sign_up_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);

                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
        login_checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    loginChecked = true;
                } else {
                    // if unChecked, removeAll
                    loginChecked = false;
                }
            }
        });
    }

    private void checkLogin(){
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            myprofile = jsonObj.getJSONArray(TAG_RESULTS);

            int i = 0;
            for (i = 0; i < myprofile.length(); i++) {
                JSONObject c = myprofile.getJSONObject(i);
                int studentid = c.getInt(TAG_ID);
                int identification = c.getInt(TAG_IDENTIFICATION);
                String name = c.getString(TAG_NAME);

                if(identification == Integer.parseInt(login_identification_et.getText().toString())){
                    savePreferences(studentid, identification, name);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);

                    break;
                }
            }
            if(i == myprofile.length()){
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage("로그인에 실패했습니다.")
                        .setPositiveButton(R.string.popup_yes, null)
                        .show();
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
                checkLogin();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }

    private void savePreferences(int id, int identification, String name){
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("studentid", id);
        editor.putInt("identification", identification);
        editor.putString("name", name);
        editor.putBoolean("autoLogin", loginChecked);
        editor.commit();
    }
}