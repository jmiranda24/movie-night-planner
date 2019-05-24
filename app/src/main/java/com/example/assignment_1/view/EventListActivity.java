package com.example.assignment_1.view;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.assignment_1.R;
import com.example.assignment_1.RESTApi.HttpURLConnectionAsyncTask;
import com.example.assignment_1.RESTApi.HttpURLConnectionAsyncTaskDraft;
import com.example.assignment_1.controller.AddEventOnClickListener;
import com.example.assignment_1.controller.AlertReceiver;
import com.example.assignment_1.controller.EditEventOnClickListener;
import com.example.assignment_1.RESTApi.HttpCall;
import com.example.assignment_1.controller.MapOnClickListener;
import com.example.assignment_1.controller.NotificationListener;
import com.example.assignment_1.model.CustomComparator;
import com.example.assignment_1.model.EventModel;
import com.example.assignment_1.model.FileLoader;
import com.example.assignment_1.service.LocationService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;

import static com.example.assignment_1.model.EventModel.eventAdapter;

public class EventListActivity extends AppCompatActivity {

    public static final String SERVICE_CHANNEL = "serviceChannel";
    private FileLoader fl = new FileLoader();

    private RecyclerView eRecyclerView;
    private RecyclerView.LayoutManager eLayoutManager;
    private Toolbar toolbar;
    private ImageView addEventButton;
    private Button calendarButton;

    private static final String TAG="EventListActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;

//    public static final String CHANNEL_1_ID = "channel1";
//    public static final String CHANNEL_2_ID = "channel2";
    private NotificationListener mNotificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_event);

        toolbar = findViewById(R.id.event_list_toolbar);
        setSupportActionBar(toolbar);

        if(savedInstanceState == null) {
            loadData();
        }

        mNotificationListener = new NotificationListener(this);

        buildRecyclerView();
        setButtons();

        if(isServicesOK()) {
            init();
        }
        createNotificationChannels();


    }

    private void init() {

    }

    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(EventListActivity.this);

        if(available == ConnectionResult.SUCCESS) {
            //everything is fine and user can make map requests
            Log.d(TAG, "isServicesOK: Google Play services is working");
            return true;
        } else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(EventListActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

//    public void sendOnChannel(String title, String message) {
//        NotificationCompat.Builder nb = mNotificationListener.getChannel1Notification(title, message);
//        mNotificationListener.getNotificationManager().notify(1, nb.build());
//    }

    private void startAlarm(int milliseconds) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1413, intent, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, milliseconds, pendingIntent);
    }

//    private void makeHttpRequest() {
//        final HttpCall httpCall = new HttpCall();
//        httpCall.setMethodtype(HttpCall.GET);
//        httpCall.setUrl("https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=Washington,DC&destinations=New+York+City,NY&key=AIzaSyDN4UvDycELwsIbLsF6CmPXgUtmPlbEU4w");
//        httpCall.setUrl("https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins=40.6655101,-73.89188969999998&destinations=40.6905615%2C-73.9976592%7C40.6905615%2C-73.9976592%7C40.6905615%2C-73.9976592%7C40.6905615%2C-73.9976592%7C40.6905615%2C-73.9976592%7C40.6905615%2C-73.9976592%7C40.659569%2C-73.933783%7C40.729029%2C-73.851524%7C40.6860072%2C-73.6334271%7C40.598566%2C-73.7527626%7C40.659569%2C-73.933783%7C40.729029%2C-73.851524%7C40.6860072%2C-73.6334271%7C40.598566%2C-73.7527626&key=YOUR_API_KEY");
//                httpCall.setUrl("https://developer.android.com/");
//        HashMap<String, String> params = new HashMap<>();
//                params.put("name", "James Bond");
//        httpCall.setParams(params);
//        new HttpURLConnectionAsyncTaskDraft() {
//            @Override
//            protected void onPostExecute(String s) {
//                super.onPostExecute(s);
//                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
//                        Toast.makeText(getApplicationContext(), httpCall.getUrl(), Toast.LENGTH_LONG).show();
//            }
//        }.execute(httpCall);
//    }

    private void createNotificationChannels() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    SERVICE_CHANNEL,
                    "Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void startService(View v) {
        Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.list_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        CustomComparator c = new CustomComparator();
        switch(item.getItemId()){
            case R.id.notification_threshold:
//                sendOnChannel("Notification title", "message");
//                startAlarm(5000);
//                NotificationThresholdDialog notificationDialog = new NotificationThresholdDialog();
//                notificationDialog.show(getSupportFragmentManager(), "Notification Threshold");

//                makeHttpRequest();
                Location currentLocation = new Location("");
                currentLocation.setLatitude(-37.807390);
                currentLocation.setLongitude(144.963300);

                Location destination = new Location("");
                destination.setLatitude(-37.8829696);
                destination.setLongitude(145.07782);
                new HttpURLConnectionAsyncTask(currentLocation, destination.getLatitude(), destination.getLongitude()).execute();
                break;
            case R.id.sort_ascending:
                c.sortAscending();
                Toast.makeText(this, "Ascending", Toast.LENGTH_SHORT).show();
                break;
            case R.id.sort_descending:
                c.sortDescending();
                Toast.makeText(this, "Descending", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadData(){
        InputStream isEvents = getResources().openRawResource(R.raw.events);
        InputStream isMovies = getResources().openRawResource(R.raw.movies);
        BufferedReader brEvents = new BufferedReader(new InputStreamReader(isEvents));
        BufferedReader brMovies = new BufferedReader(new InputStreamReader(isMovies));

        try{
            EventModel.loadEvents(fl.loadFile(brEvents));
            EventModel.loadMovies(fl.loadFile(brMovies), this);
        }catch(ParseException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                brEvents.close();
                brMovies.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public void setButtons() {
        addEventButton = findViewById(R.id.addEventButton);
        calendarButton = findViewById(R.id.calendar_button);
        ImageView mapButton = findViewById(R.id.mapButton);
        mapButton.setOnClickListener(new MapOnClickListener(this));
        addEventButton.setOnClickListener(new AddEventOnClickListener(this, eventAdapter));
    }

    public void buildRecyclerView(){
        eRecyclerView = findViewById(R.id.eRecyclerView);
        eRecyclerView.setHasFixedSize(true);
        eLayoutManager = new LinearLayoutManager(this);

        eRecyclerView.setLayoutManager(eLayoutManager);
        eRecyclerView.setAdapter(eventAdapter);

        eventAdapter.setOnItemClickListener(new EditEventOnClickListener(this, eventAdapter));
    }

}
