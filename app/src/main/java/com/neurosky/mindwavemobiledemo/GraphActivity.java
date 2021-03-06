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
    Spinner spinner1;   // color
    Spinner spinner2;   // bpm
    Spinner spinner3;   // time
    EditText editText;      // test

    // 현재 사용자 정보
    int personid;
    String selectedColor;
    String selectedBpm;
    int conditionid;
    String start_time = "";
    int person_conditionid;

    // JSON
    String myConditionJSON;
    String myTimeJSON;
    String myPersonConidJSON;
    // 그래프용 JSON
    String myNeuroGraphJSON;
    String myGSRGraphJSON;

    JSONArray conditionJArray = null;
    JSONArray timeJArray = null;
    JSONArray PersonConidJArray = null;
    // 그래프용 JSONArray
    JSONArray attentionJArray = null;
    JSONArray gsrJArray = null;

    ArrayList<String> conditionList = new ArrayList<String>();
    ArrayList<String> timeList = new ArrayList<String>();

    // *********************************************************************************************
    // *********************************************************************************************

    ArrayList<String> placeList = new ArrayList<String>();

    int studentid = 0;
    int stdplaceid = 0;

    String myPlaceJSON;


    private static final String TAG_RESULTS = "result";
    private static final String TAG_NEUROSKY_ID = "neuroskyid";
    private static final String TAG_STD_PLACEID = "stdplaceid";
    private static final String TAG_TIME = "time";
    private static final String TAG_ATTENTION = "attention";
    private static final String TAG_STUDENTID = "studentid";
    private static final String TAG_PLACEID = "placeid";
    private static final String TAG_START_TIME = "start_time";
    private static final String TAG_COLOR = "color";
    private static final String TAG_BPM = "bpm";
    private static final String TAG_CONDITION_ID = "conditionid";
    private static final String TAG_PERSON_ID = "personid";
    private static final String TAG_PERSON_CONDITION_ID = "person_conditionid";
    private static final String TAG_GSRDATA = "gsrdata";


    JSONArray placeJArray = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_graph);

        // graph (4.X)
        graph = (GraphView) findViewById(R.id.graph1);
        spinner1 = (Spinner) findViewById(R.id.colorSpinner);
        spinner2 = (Spinner) findViewById(R.id.bpmSpinner);
        spinner3 = (Spinner) findViewById(R.id.timeSpinner);
        spinner1.setPrompt("Color");
        spinner2.setPrompt("BPM");
        spinner3.setPrompt("Time");
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

        // 현재 로그인한 사용자(personid) 알아내기
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        personid = pref.getInt("personid",0);
        // 나중에 지우기!!!!*********************************
        personid = 2;

        // 스피너1,2는 같은 기능 3은 선택되면 바로 그래프 보여줌

        // 스피너 리스너
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 현재 설정된 값으로 전역변수 변경
                selectedColor = "" + spinner1.getSelectedItem();
                selectedBpm = "" + spinner2.getSelectedItem();

                // conditionid 알아내기
                getConditionid("http://14.63.214.221/condition_get.php");

                // 현재 설정된 color, bpm 바탕으로 시간들 리스트 알려주는 메소드 호출
                getTimes("http://14.63.214.221/person_condition_get.php");

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // 스피너 리스너
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 현재 설정된 값으로 전역변수 변경
                selectedColor = "" + spinner1.getSelectedItem();
                selectedBpm = "" + spinner2.getSelectedItem();

                // conditionid 알아내기
                getConditionid("http://14.63.214.221/condition_get.php");

                // 현재 설정된 color, bpm 바탕으로 시간들 리스트 알려주는 메소드 호출
                getTimes("http://14.63.214.221/person_condition_get.php");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // 스피너3 리스너
        spinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 선택한 시간을 바탕으로 이제 그래프를 보여줍시당
                start_time = timeList.get(position);   // start_time 전역
                // person_conditionid 찾아내서 그래프 보여주기
                getResult("http://14.63.214.221/person_condition_get.php");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // *********************************************************************************************
        // *********************************************************************************************


        // 시간들 알아내기(마지막 스피너)
        //getPlaces("http://14.63.214.221/std_place_get.php");

        /*
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
        */
    }

    // conditionID 찾아내기
    public void getConditionid(String url) {
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
                myConditionJSON = result;
                getConditionidResult();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }

    // conditionID 설정
    protected void getConditionidResult(){
        try {
            JSONObject jsonObj = new JSONObject(myConditionJSON);
            conditionJArray = jsonObj.getJSONArray(TAG_RESULTS);

            // 배열 비우기
            conditionList.clear();
            for (int i = 0; i<conditionJArray.length(); i++){
                JSONObject obj = conditionJArray.getJSONObject(i);

                // color, bpm 비교해서 conditionid 찾기
                String color = obj.getString(TAG_COLOR);
                String bpm = obj.getString(TAG_BPM);
                int subConditionid = obj.getInt(TAG_CONDITION_ID);

                // 찾기
                if(selectedColor.equalsIgnoreCase(color) && selectedBpm.equalsIgnoreCase(bpm)){
                    conditionid = subConditionid;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // times 찾아내기
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

                // personid, conditionid 비교해서 time 넣기
                int subPersonid = obj.getInt(TAG_PERSON_ID);
                int subConditionid = obj.getInt(TAG_CONDITION_ID);
                String time = obj.getString(TAG_START_TIME);

                if(personid == subPersonid && subConditionid == conditionid ){
                    timeList.add(""+time);
                }

            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.place_spinner_item,timeList);
            spinner3.setAdapter(adapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // person_condition ID 구하기
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
                myPersonConidJSON = result;
                getPersonConid();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }

    protected void getPersonConid(){
        try {
            JSONObject jsonObj = new JSONObject(myPersonConidJSON);
            PersonConidJArray = jsonObj.getJSONArray(TAG_RESULTS);

            for (int i = 0; i<PersonConidJArray.length(); i++){
                JSONObject obj = PersonConidJArray.getJSONObject(i);

                // start_time, personid, conditionid 비교해서 person_conditionid 찾아내기
                String time = obj.getString(TAG_START_TIME);
                int subPersonid = obj.getInt(TAG_PERSON_ID);
                int subConditionid = obj.getInt(TAG_CONDITION_ID);
                int subPersonCondition_ID = obj.getInt(TAG_PERSON_CONDITION_ID);

                if(time.equalsIgnoreCase(start_time) && subPersonid == personid && subConditionid == conditionid){
                    person_conditionid = subPersonCondition_ID;
                    editText.setText("테스트용person_conditionid: "+person_conditionid);
                }

                // for graph
                getNeuro("http://14.63.214.221/neurosky_get.php");  // 여기서 drawNeuro() 호출
                getGSR("http://14.63.214.221/gsr_get.php");    // 여기서 drawGSR() 호출
                // getData("http://14.63.214.221/neurosky_get.php");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getNeuro(String url) {
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
                myNeuroGraphJSON = result;
                drawNeuro();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }
    protected void drawNeuro() {
        try {
            JSONObject jsonObj = new JSONObject(myNeuroGraphJSON);
            attentionJArray = jsonObj.getJSONArray(TAG_RESULTS);

            // 필요한 attention들
            ArrayList<Integer> attentionList = new ArrayList<Integer>();

            // x축 크기 알아내기
            for (int i =0; i<attentionJArray.length(); i++){
                JSONObject c = attentionJArray.getJSONObject(i);
                int subStdplaceid = c.getInt(TAG_STD_PLACEID);
                int attention = c.getInt(TAG_ATTENTION);
                if(person_conditionid == subStdplaceid){
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

    // BPM 그래프
    public void getGSR(String url) {
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
                myGSRGraphJSON = result;
                drawGSR();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
}
    protected void drawGSR() {
        try {
            JSONObject jsonObj = new JSONObject(myGSRGraphJSON);
            gsrJArray = jsonObj.getJSONArray(TAG_RESULTS);

            // 필요한 gsr값들
            ArrayList<Integer> gsrList = new ArrayList<Integer>();

            // x축 크기 알아내기
            for (int i =0; i<gsrJArray.length(); i++){
                JSONObject c = gsrJArray.getJSONObject(i);
                int id = c.getInt(TAG_PERSON_CONDITION_ID);
                int gsrDT = c.getInt(TAG_GSRDATA);
                if(person_conditionid == id){
                    // 배열에 넣는다.
                    gsrList.add(gsrDT);
                }
            }
            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0.0);
            graph.getViewport().setMaxX(gsrList.size());

            // graph 그리기
            DataPoint[] dp = new DataPoint[gsrList.size()];
            for (int i = 0; i < gsrList.size(); i++) {
                // 나중에 x값 더 구체적으로
                dp[i] = new DataPoint(i, gsrList.get(i));
            }
            LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dp);
            graph.addSeries(series);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
    protected void makeGraph() {
        try {
            JSONObject jsonObj = new JSONObject(myNGraphJSON);
            attentionJArray = jsonObj.getJSONArray(TAG_RESULTS);

            // 필요한 attention들
            ArrayList<Integer> attentionList = new ArrayList<Integer>();

            // x축 크기 알아내기
            for (int i =0; i<attentionJArray.length(); i++){
                JSONObject c = attentionJArray.getJSONObject(i);
                int nowStdplaceid = c.getInt(TAG_STD_PLACEID);
                int attention = c.getInt(TAG_ATTENTION);
                if(nowStdplaceid == nowStdplaceid){
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
    }*/






















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




}
