package com.neurosky.mindwavemobiledemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;


/**
 * Created by lgpc on 2016-07-20.
 */
public class GraphActivity extends Activity {

    GraphView graph;
    Spinner spinner1;
    Spinner spinner2;
    EditText editText;
    ArrayList<String> placeList = new ArrayList<String>();
    ArrayList<String> timeList = new ArrayList<String>();

    // 현재 사용자 정보
    int subStdID;
    int studentid = 0;
    int placeid = 0;
    String start_time = "";
    int stdplaceid = 0;

    // JSON
    String myPlaceJSON;
    String myTimeJSON;
    String myStdplaceidJSON;
    String myGraphJSON;

    private static final String TAG_RESULTS = "result";
    private static final String TAG_NEUROSKY_ID = "neuroskyid";
    private static final String TAG_STD_PLACEID = "stdplaceid";
    private static final String TAG_TIME = "time";
    private static final String TAG_ATTENTION = "attention";
    private static final String TAG_STUDENTID = "studentid";
    private static final String TAG_PLACEID = "placeid";
    private static final String TAG_START_TIME = "start_time";

    JSONArray placeJArray = null;
    JSONArray timeJArray = null;
    JSONArray StdplaceidJArray = null;
    JSONArray attentionJArray = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_graph);

        // graph (4.X)
        graph = (GraphView) findViewById(R.id.graph1);
        spinner1 = (Spinner) findViewById(R.id.placeSpinner);
        spinner2 = (Spinner) findViewById(R.id.timeSpinner);
        spinner1.setPrompt("자리");
        spinner2.setPrompt("측정 시작시간");
        editText = (EditText) findViewById(R.id.place);

        // graph 설정
        GridLabelRenderer glr = graph.getGridLabelRenderer();
        glr.setPadding(32); // should allow for 3 digits to fit on screen
        // graph x,y axis
        //graph.getViewport().setXAxisBoundsManual(true);
        //graph.getViewport().setMinX(0.0);
        //graph.getViewport().setMaxX(27.0);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0.0);
        graph.getViewport().setMaxY(100.0);

        // 현재 로그인한 사용자 알아내기
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        subStdID = pref.getInt("studentid",0);
        studentid = subStdID;

        // 자리들 알아내기
        getPlaces("http://14.63.214.221/std_place_get.php");

        // places 스피너 리스너
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 스피너에서 자리선택시 해당자리에서 시작시간들을 보여주기
                String choice = placeList.get(position);
                // (전역변수) 자리 설정
                placeid= Integer.parseInt(choice);
                // 측정 시작시간들 알아내기
                getTimes("http://14.63.214.221/std_place_get.php");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // times 스피너 리스너
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 선택한 시간을 바탕으로 이제 그래프를 보여줍시당
                start_time = timeList.get(position);   // start_time 전역
                // Stdplaceid찾아내서 그래프 보여주기
                getResult("http://14.63.214.221/std_place_get.php");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    // 1. places 찾아내기
    public void getPlaces(String url) {
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
                myPlaceJSON = result;
                getPlacesResult();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }

    protected void getPlacesResult(){
        try {
            JSONObject jsonObj = new JSONObject(myPlaceJSON);
            placeJArray = jsonObj.getJSONArray(TAG_RESULTS);

            //placeList.clear();
            ArrayList<String> subList = new ArrayList<String>();
            for (int i = 0; i<placeJArray.length(); i++){
                JSONObject obj = placeJArray.getJSONObject(i);
                int stdid = obj.getInt(TAG_STUDENTID);
                int plcid = obj.getInt(TAG_PLACEID);

                // 로그인된 사용자의 자리들을 스피너에 모두 넣는다
                if(stdid == studentid){
                    subList.add(""+plcid);
                }
            }
            // for문 끝

            // HashSet 중복 제거
            placeList = new ArrayList<String>(new HashSet<String>(subList));
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.place_spinner_item,placeList);
            spinner1.setAdapter(adapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 2. times 찾아내기
    public void getTimes(String url) {
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
                myTimeJSON = result;
                getTimesResult();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }

    protected void getTimesResult(){
        try {
            JSONObject jsonObj = new JSONObject(myTimeJSON);
            timeJArray = jsonObj.getJSONArray(TAG_RESULTS);

            // 배열 비우기
            timeList.clear();
            for (int i = 0; i<timeJArray.length(); i++){
                JSONObject obj = timeJArray.getJSONObject(i);
                int stdid = obj.getInt(TAG_STUDENTID);
                int plcid = obj.getInt(TAG_PLACEID);
                String time = obj.getString(TAG_START_TIME);

                // 로그인된 사용자의 시간들을 스피너에 모두 넣는다
                if(stdid == studentid && plcid==placeid){
                    timeList.add(""+time);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.place_spinner_item,timeList);
            spinner2.setAdapter(adapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 3. Student Place ID 구하기
    public void getResult(String url) {
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
                myStdplaceidJSON = result;
                getStdplaceid();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }

    protected void getStdplaceid(){
        try {
            JSONObject jsonObj = new JSONObject(myStdplaceidJSON);
            StdplaceidJArray = jsonObj.getJSONArray(TAG_RESULTS);

            for (int i = 0; i<StdplaceidJArray.length(); i++){
                JSONObject obj = StdplaceidJArray.getJSONObject(i);
                int stdid = obj.getInt(TAG_STUDENTID);
                int plcid = obj.getInt(TAG_PLACEID);
                String time = obj.getString(TAG_START_TIME);
                int std_plc_ID = obj.getInt(TAG_STD_PLACEID);

                // stdid, plcid, time이 모두 일치하는 std_plc_ID를 찾는다
                if(stdid== studentid && plcid == placeid && time.equalsIgnoreCase(start_time)){
                    // 나중에 지우기
                    editText.setText("테스트용: "+std_plc_ID+"/"+ subStdID);
                    stdplaceid = std_plc_ID;
                }
                // for graph
                getData("http://14.63.214.221/neurosky_get.php");

            }
        } catch (JSONException e) {
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
                myGraphJSON = result;
                makeGraph();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }

    protected void makeGraph() {
        try {
            JSONObject jsonObj = new JSONObject(myGraphJSON);
            attentionJArray = jsonObj.getJSONArray(TAG_RESULTS);

            // 필요한 attention들
            ArrayList<Integer> attentionList = new ArrayList<Integer>();

            // x축 크기 알아내기
            for (int i =0; i<attentionJArray.length(); i++){
                JSONObject c = attentionJArray.getJSONObject(i);
                int nowStdplaceid = c.getInt(TAG_STD_PLACEID);
                int attention = c.getInt(TAG_ATTENTION);
                if(nowStdplaceid == stdplaceid){
                    // 배열에 넣는다.
                    attentionList.add(attention);
                }
            }
            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0.0);
            graph.getViewport().setMaxX(attentionList.size());

            // graph 그리기
            DataPoint[] dp = new DataPoint[attentionList.size()];
            for (int i = 0; i < attentionList.size(); i++) {
                // 나중에 x값 더 구체적으로
                dp[i] = new DataPoint(i, attentionList.get(i));
            }
            LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dp);
            graph.addSeries(series);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
