package com.neurosky.mindwavemobiledemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

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

public class SignUpActivity extends Activity {
    EditText sign_up_email_et, sign_up_name_et, sign_up_password_et;
    Button sign_up_btn;

    // getdata
    String myJSON;
    private static final String TAG_RESULTS = "result";
    private static final String TAG_ID = "personid";
    private static final String TAG_EMAIL = "email";
    private static final String TAG_PASSWORD = "password";
    private static final String TAG_NAME = "name";
    JSONArray myprofile = null;

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
        setContentView(R.layout.activity_sign_up);

        sign_up_password_et = (EditText) findViewById(R.id.sign_up_password_et);
        sign_up_email_et = (EditText) findViewById(R.id.sign_up_email_et);
        sign_up_name_et = (EditText) findViewById(R.id.sign_up_name_et);
        sign_up_btn = (Button) findViewById(R.id.sign_up_btn);

        sign_up_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getData("http://14.63.214.221/person_get.php");
            }
        });
    }

    // 서버에 저장하는 함수
    private void insertToDatabase(String email, String password, String name){
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
                    String email = (String)params[0];
                    String password = (String)params[1];
                    String name = (String)params[2];

                    String link="http://14.63.214.221/person_insert.php";
                    String data  = URLEncoder.encode("email", "UTF-8") + "=" + URLEncoder.encode(email, "UTF-8");
                    data += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8");
                    data += "&" + URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(name, "UTF-8");

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
        task.execute(email, password, name);
    }

    private void checkLogin(){
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            myprofile = jsonObj.getJSONArray(TAG_RESULTS);

            int i = 0;
            for (i = 0; i < myprofile.length(); i++) {
                JSONObject c = myprofile.getJSONObject(i);
                String email = c.getString(TAG_EMAIL);

                if(email.equals(sign_up_email_et.getText().toString())){
                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage("아이디가 이미 존재 합니다.")
                            .setPositiveButton(R.string.popup_yes, null)
                            .show();
                    break;
                }
            }
            if(i == myprofile.length()){
                insertToDatabase(sign_up_email_et.getText().toString(), sign_up_password_et.getText().toString(),sign_up_name_et.getText().toString());
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
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
}
