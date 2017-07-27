package liamkengineering.caretaker;

import android.app.ActionBar;
import android.app.Dialog;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import android.widget.ArrayAdapter;
import android.app.ListActivity;

public class Display extends AppCompatActivity {
    private static final String ERIC_SERVER = "https://linkedin-whynot.herokuapp.com";
    private static final String ADD_REQUEST = "/api/add_medication?";
    private static final String LOG_REQUEST = "/api/log";
    private static final String TAKE_NOW = "/api/take_now";

    // Refreshing
    Handler h = new Handler();
    Runnable refreshPills = new Runnable() {
        @Override
        public void run() {
            refresh();
            h.postDelayed(refreshPills, 500);
        }
    };

    EditText test;
    Button newPill;

    private String PILL_DATA;
    private ArrayList<PillTaken> log;

    AlertDialog.Builder builder;
    AlertDialog.Builder frequencyBuilder;
    static PillChoice pc;
    Button freqButton;
    Button timeButton;
    Button refresh;
    Button now;
    View layout;

    LinearLayout display;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        pc = new PillChoice();

        PILL_DATA = getIntent().getStringExtra("PILL_DATA");
        log = pillParser(PILL_DATA);
        test = (EditText) findViewById(R.id.test);

        display = (LinearLayout) findViewById(R.id.pill_log);

        logPills(log);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = inflater.inflate(R.layout.dialog_layout, null, false);
        final EditText pillBox = (EditText) layout.findViewById(R.id.pill_name_box);

        /*
        refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });
        */

        now = (Button) findViewById(R.id.take_now);
        now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                medicateNow();
            }
        });
        freqButton = (Button) layout.findViewById(R.id.choose_freq);
        freqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                frequencyBuilder.show();
            }
        });
        timeButton = (Button) layout.findViewById(R.id.choose_time);
        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new TimePickerFragment();
                newFragment.show(getSupportFragmentManager(), "timePicker");
            }
        });
        builder = new AlertDialog.Builder(this);
        frequencyBuilder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // save info here
                pc.name = pillBox.getText().toString();
                sendPillRequest(pc);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        frequencyBuilder.setTitle(R.string.frequency).
                setSingleChoiceItems(R.array.freq, 2,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                               pc.frequency = which;
                                dialog.dismiss();
                            }
                        });
        frequencyBuilder.create();
        builder.create();
        newPill = (Button) findViewById(R.id.register);
        newPill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDialog();
            }
        });

        h.post(refreshPills);
    }

        private void setDialog() {
            if(layout != null) {
                ViewGroup parent = (ViewGroup) layout.getParent();
                if(parent != null) {
                    parent.removeView(layout);
                    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    layout = inflater.inflate(R.layout.dialog_layout, null, false);
                }
            }
            builder.show();;
        }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            cal.set(Calendar.MINUTE, minute);
            setTime(cal.getTimeInMillis()/1000);
        }
    }

    public String quickEncode(String s) {
        String retString = "";
        for(int i = 0; i < s.length(); ++i) {
            if(s.charAt(i) == ' ') {
                retString += '_';
            }
            else {
                retString += s.charAt(i);
            }
        }
        return retString;
    }

    public void sendPillRequest(PillChoice pc) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = ERIC_SERVER + ADD_REQUEST + "name=" + quickEncode(pc.name);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(Display.this, "Scheduled!", Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        queue.add(stringRequest);
    }

    public void medicateNow() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = ERIC_SERVER + TAKE_NOW;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(Display.this, "Time to Take!", Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(stringRequest);
    }

    public static void setTime(long timestamp) {
        pc.timeOfDay = timestamp;
    }

    private ArrayList<PillTaken> pillParser(String s) {
        Log.v("LENGTH", "" + s.length());
        ArrayList<PillTaken> pillLog = new ArrayList<>();
        for(int i = 0; i < s.length(); ++i) {
            if(s.charAt(i) == '(') {
                ++i;
                String num = "";
                String name = "";
                boolean taken = false;
                while(s.charAt(i) != '.') {
                    num += s.charAt(i);
                    ++i;
                }
                while(s.charAt(i) != '\'') {
                    ++i;
                }
                ++i;
                while(s.charAt(i) != '\'') {
                    name += s.charAt(i);
                    ++i;
                }
                i+=3;
                taken = (s.charAt(i) == 'T');
                PillTaken next = new PillTaken(name, Long.valueOf(num).longValue(), taken);
                pillLog.add(next);
            }
        }
        return pillLog;
    }

    public void addEntry(PillTaken pc) {
        TextView tv = new TextView(Display.this);
        tv.setBackgroundResource((pc.taken ? R.color.green : R.color.red));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ActionBar.LayoutParams.FILL_PARENT,
                ActionBar.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0,0,0,10);
        tv.setLayoutParams(llp);
        tv.setPadding(5,5,5,5);
        tv.setTextSize(25);
        Date df = new Date(pc.time*1000);
        String date = new SimpleDateFormat("MMMM dd, hh:mma").format(df);
        tv.setText(pc.name + ":\t" + date);
        display.addView(tv);
    }

    public void logPills(ArrayList<PillTaken> l) {
        for(int i = l.size()-1; i >=0; --i) {
            addEntry(l.get(i));
        }
    }

    public void refresh() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = ERIC_SERVER + LOG_REQUEST;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        display.removeAllViews();
                        log = pillParser(response);
                        logPills(log);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(stringRequest);
    }

}
class PillChoice {
    String name;
    int frequency; // 0 = daily, 1 = weekly
    long timeOfDay; // UNIX timestamp

    public PillChoice() {

    }
}

class PillTaken {
    String name;
    long time;
    boolean taken;

    public PillTaken(String name, long time, boolean taken) {
        this.name = name;
        this.time = time;
        this.taken = taken;
    }
}

