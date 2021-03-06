package com.cookandroid.k_project;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.cookandroid.k_project.Header.closeDrawer;
import static com.cookandroid.k_project.Header.openDrawer;
import static com.cookandroid.k_project.Header.redirectActivity;

public class SaveLocation extends AppCompatActivity implements OnMapReadyCallback {
    DrawerLayout drawerLayout;
    Button first,second,third,fourth;
    Button btnStart, btnSearch, btnStop, btnUpdate;
    EditText edtX , edtY;
    //TextView tvResult, tvTest;
    SQLiteDatabase sqlDB;
    myDBHelper myHelper;
    String username=((Header) Header.header).username;;
    TextView tvuser;
    static TimerTask timerTask;
    private GoogleMap mMap;
    private GpsTracker gpsTracker;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.savelocation);
        drawerLayout = findViewById(R.id.drawer_layout);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();
        }else {

            checkRunTimePermission();
        }

        btnStart = (Button) findViewById(R.id.btnStart);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnUpdate = (Button) findViewById(R.id.btnUpdate);
        //tvResult = (TextView) findViewById(R.id.tvResult);
        //tvTest = (TextView) findViewById(R.id.tvTest);
        gpsTracker = new GpsTracker(SaveLocation.this);

        myHelper = new myDBHelper(this);



        timerTask = timerTaskMaker();

        final Timer timer = new Timer();
        timer.schedule(timerTask,0,5000);

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timerTask.cancel();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timerTask = timerTaskMaker();
                Log.e(".",".");
                timer.schedule(timerTask,0,5000);
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sqlDB = myHelper.getReadableDatabase();
                Cursor cursor;
                cursor = sqlDB.rawQuery("SELECT * FROM locationTBL;",null);

                String date = "";
                String locateX = "";
                String locateY = "";
                String result = "";

                while(cursor.moveToNext()){
                    date = cursor.getString(0);
                    locateX = cursor.getString(1);
                    locateY = cursor.getString(2);
                    result += date + "   " + locateX + "   " + locateY + "\r\n";
                }

                //tvResult.setText(result);

                cursor.close();
                sqlDB.close();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sqlDB = myHelper.getReadableDatabase();
                Cursor cursor;
                cursor = sqlDB.rawQuery("SELECT * FROM locationTBL;",null);

                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat mFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String time = mFormat.format(date);
                int currentMonth = Integer.parseInt(time.substring(5,7));
                int currentDay = Integer.parseInt(time.substring(8,10));

                while(cursor.moveToNext()){
                    String dateName = cursor.getString(0);
                    int buffMonth = Integer.parseInt(dateName.substring(5,7));
                    int buffDay = Integer.parseInt(dateName.substring(8,10));
                    int stateMonth = currentMonth;
                    int stateDay = currentDay;

                    if(currentMonth > buffMonth || currentDay < buffDay){
                        if(buffMonth == 2 || buffMonth == 4 || buffMonth == 6 || buffMonth == 9 || buffMonth == 11 )
                            stateMonth += 30;
                        else
                            stateDay += 31;
                    }

                    if(stateDay - buffDay == 14){
                        sqlDB.execSQL("DELETE FROM locationTBL WHERE gName = '" + dateName + "';");
                    }else if(stateDay - buffDay < 14)
                        break;


                }




            }
        });


        first=(Button)findViewById(R.id.btn_first);
        second=(Button)findViewById(R.id.btn_second);
        third=(Button)findViewById(R.id.btn_third);
        fourth=(Button)findViewById(R.id.btn_fourth);

        first.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Header.class);
                startActivity(intent);
                finish();
            }
        });
        second.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        third.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoadingActivity.class);
                startActivity(intent);
                finish();
            }
        });
        fourth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getApplicationContext(), Header.class);
                startActivity(intent);
                finish();
            }
        });
    }
    public TimerTask timerTaskMaker(){
        TimerTask tempTask = new TimerTask() {
            @Override
            public void run() {
                Log.e(".",".");
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat mFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String time = mFormat.format(date);

                double latitude = gpsTracker.getLatitude();
                double longitude = gpsTracker.getLongitude();

//                tvTest.setText(Double.toString(latitude) + Double.toString(longitude));

                sqlDB = myHelper.getWritableDatabase();
                sqlDB.execSQL("INSERT INTO locationTBL VALUES ('" + time + "'," + latitude + "," + longitude + ");");
            }
        };
        return tempTask;
    }

    public void onMapReady(final GoogleMap googleMap) {

        mMap = googleMap;

        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();

        LatLng Current = new LatLng(latitude, longitude);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(Current);
        markerOptions.title("?????? ??????");
        markerOptions.snippet("?????? ??????");
        mMap.addMarker(markerOptions);

        sqlDB = myHelper.getReadableDatabase();
        Cursor cursor;
        cursor = sqlDB.rawQuery("SELECT * FROM locationTBL;",null);

        String locateX = "";
        String locateY = "";
        String result = "";

        while(cursor.moveToNext()){

            locateX = cursor.getString(1);
            locateY = cursor.getString(2);

            double doubleX = Double.parseDouble(locateX);
            double doubleY = Double.parseDouble(locateY);

            MarkerOptions makerOptions = new MarkerOptions();
            makerOptions // LatLng??? ?????? ???????????? ???????????? ????????? ?????? ??????.
                    .position(new LatLng(doubleX , doubleY))
                    .title(".");


            // 2. ?????? ?????? (????????? ?????????)
            mMap.addMarker(makerOptions);

        }


        cursor.close();
        sqlDB.close();


        // ????????? ???????????? ?????? 2?????? ????????? ????????????.

        // CameraUpdateFactory.zoomTo??? ??????????????????.
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Current, 10));


    }

    /*public class myDBHelper extends SQLiteOpenHelper {

        public myDBHelper(Context context) {
            super(context, "weloDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            //sqLiteDatabase.execSQL("CREATE TABLE locationTBL(gName CHAR(30) PRIMARY KEY, locationX NUMERIC, locationY NUMERIC);");

        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS locationTBL");
            onCreate(sqLiteDatabase);

        }
    }*/

    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // ?????? ????????? PERMISSIONS_REQUEST_CODE ??????, ????????? ????????? ???????????? ??????????????????

            boolean check_result = true;


            // ?????? ???????????? ??????????????? ???????????????.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {

                //?????? ?????? ????????? ??? ??????
                ;
            }
            else {
                // ????????? ???????????? ????????? ?????? ????????? ??? ?????? ????????? ??????????????? ?????? ???????????????.2 ?????? ????????? ????????????.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(SaveLocation.this, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????.", Toast.LENGTH_LONG).show();
                    finish();


                }else {

                    Toast.makeText(SaveLocation.this, "???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ???????????? ?????????. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){

        //????????? ????????? ??????
        // 1. ?????? ???????????? ????????? ????????? ???????????????.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(SaveLocation.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(SaveLocation.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. ?????? ???????????? ????????? ?????????
            // ( ??????????????? 6.0 ?????? ????????? ????????? ???????????? ???????????? ????????? ?????? ????????? ?????? ???????????????.)


            // 3.  ?????? ?????? ????????? ??? ??????



        } else {  //2. ????????? ????????? ????????? ?????? ????????? ????????? ????????? ???????????????. 2?????? ??????(3-1, 4-1)??? ????????????.

            // 3-1. ???????????? ????????? ????????? ??? ?????? ?????? ????????????
            if (ActivityCompat.shouldShowRequestPermissionRationale(SaveLocation.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. ????????? ???????????? ?????? ?????????????????? ???????????? ????????? ????????? ???????????? ????????? ????????????.
                Toast.makeText(SaveLocation.this, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.", Toast.LENGTH_LONG).show();
                // 3-3. ??????????????? ????????? ????????? ?????????. ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(SaveLocation.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. ???????????? ????????? ????????? ??? ?????? ?????? ???????????? ????????? ????????? ?????? ?????????.
                // ?????? ????????? onRequestPermissionResult?????? ???????????????.
                ActivityCompat.requestPermissions(SaveLocation.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    public String getCurrentAddress( double latitude, double longitude) {

        //????????????... GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //???????????? ??????
            Toast.makeText(this, "???????????? ????????? ????????????", Toast.LENGTH_LONG).show();
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "????????? GPS ??????", Toast.LENGTH_LONG).show();
            return "????????? GPS ??????";

        }



        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "?????? ?????????", Toast.LENGTH_LONG).show();
            return "?????? ?????????";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";

    }


    //??????????????? GPS ???????????? ?????? ????????????
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(SaveLocation.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
                + "?????? ????????? ???????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //???????????? GPS ?????? ???????????? ??????
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS ????????? ?????????");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void ClickMenu(View view){
        Header.openDrawer(drawerLayout);
        tvuser = findViewById(R.id.tvuser);
        tvuser.setText(username+"???,");
    }

    public void ClickLogo(View view){
        Header.closeDrawer(drawerLayout);
        //Header.redirectActivity(this, Chart.class);
        Intent intent = new Intent(getApplicationContext(), Chart.class);
        startActivity(intent);
        finish();
    }

    public void ClickSelfDiagnosis(View view){
        //Header.redirectActivity(this,Selfdiagnosis.class);
        Intent intent = new Intent(getApplicationContext(), Selfdiagnosis.class);
        startActivity(intent);
        finish();
    }

    public void ClickSetting(View view){
        //Header.redirectActivity(this, Setting.class);
        Intent intent = new Intent(getApplicationContext(), Setting.class);
        startActivity(intent);
        finish();
    }

    public void ClickLogout(View view){
        Header.logout(this);
    }
    protected void onPause(){
        super.onPause();
        Header.closeDrawer(drawerLayout);
    }

}