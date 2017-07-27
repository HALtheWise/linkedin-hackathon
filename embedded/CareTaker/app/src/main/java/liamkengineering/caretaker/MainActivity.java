package liamkengineering.caretaker;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String ERIC_SERVER = "https://linkedin-whynot.herokuapp.com";
    private static final String LOG_REQUEST = "/api/log";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPillInfo();
    }

    private void getPillInfo() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String pillInfo;
        String url = ERIC_SERVER + LOG_REQUEST;
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // move to the main activity here, pass
                // the response
                Intent i = new Intent(MainActivity.this, Display.class);
                i.putExtra("PILL_DATA", response);
                startActivity(i);
            }
        }, new Response.ErrorListener()
            {
                @Override
                public void onErrorResponse (VolleyError error){
                // move to the main activity here
                Intent i = new Intent(MainActivity.this, Display.class);
                i.putExtra("PILL_DATA", "error");
                startActivity(i);
            }
        });
        queue.add(stringRequest);
    }
}
