package com.neurosky.mindwavemobiledemo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

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
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by lgpc on 2016-07-20.
 */
public class GraphActivity extends Activity {

    GraphView graph;

    // getData
    String myJSON;
    private static final String TAG_RESULTS = "result";
    private static final String TAG_NEUROSKY_ID = "neuroskyid";
    private static final String TAG_STD_PLACEID = "stdplaceid";
    private static final String TAG_TIME = "time";
    private static final String TAG_ATTENTION = "attention";
    JSONArray attentions = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_graph);

        // graph (4.X)
        graph = (GraphView) findViewById(R.id.graph1);

        //
        GridLabelRenderer glr = graph.getGridLabelRenderer();
        glr.setPadding(32); // should allow for 3 digits to fit on screen

        // graph x,y axis
        //graph.getViewport().setXAxisBoundsManual(true);
        //graph.getViewport().setMinX(0.0);
        //graph.getViewport().setMaxX(27.0);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0.0);
        graph.getViewport().setMaxY(100.0);

        getData("http://14.63.214.221/neurosky_get.php");
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
                makeGraph();
            }
        }
        GetDataJSON g = new GetDataJSON();
        g.execute(url);
    }

    ///////////////////////////////////////////////////////////
    protected void makeGraph() {
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            attentions = jsonObj.getJSONArray(TAG_RESULTS);

            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0.0);
            graph.getViewport().setMaxX(attentions.length());

            DataPoint[] dp = new DataPoint[attentions.length()];
            for (int i = 0; i < attentions.length(); i++) {
                JSONObject c = attentions.getJSONObject(i);
                int attention = c.getInt(TAG_ATTENTION);
                dp[i] = new DataPoint(i, attention);
            }
            LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dp);
            graph.addSeries(series);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}