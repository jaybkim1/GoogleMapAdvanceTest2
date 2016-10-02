package com.example.jayb.googlemapadvancetest2;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatically Managed Connection을 하려면 (GoogleApiClient) 연결중에 에러가 나는 것을 예외처리 하기 위해서 OnConnectionFailedListener 를 인터페이스 해줘야 된다
 *
 * **/


/*******************************내 자체 DB에서 받아오기 위해서 만듬**********************************/
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    SupportMapFragment mapFragment; //Activity에서 하기 때문에 SupportFragment를 사용

    private GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLocation;
    Marker mCurrLocationMarker, placeMarker;
    MarkerOptions markerOptions;

    /**Spinner**/
    Spinner spinner_distance;
    Button search_btn;

    Double current_latitude;
    Double current_longitude;
    String distance = "500";

    List<LocationData> markers;
    LocationData locationData;


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //버전 체크
            int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.d("PackageManager","Here");
                requestPermission();
            }else{

                locationControl();

                //이 코드를 안적어주면 그냥 지도만 뜨는데 이 코드를 써주면 내가 지정한 경도와 위도에 마커표시되면서 뜬다 (아래 onMapReady 메소드 호출)
                mapFragment.getMapAsync(this);
            }
        }

        /**커스텀 스피너 시간될 때 해보기**/
        //http://blog.nkdroidsolutions.com/android-custom-spinner-dropdown-example-programmatically/
        spinner_distance = (Spinner)findViewById(R.id.spinner);
        spinner_distance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {



                //500 -> 1000 -> 500 으로 돌아 갈 시에 원래 찍여힜던 마커를 없애는 작업
                //이렇게 한번 해보자!!!2시 9분

                if (placeMarker!=null){

                    Log.d("clearmMap", "clear->"+placeMarker);
                    mMap.clear(); //해결됬다..

                }

                //서버에 보내줄 내 주변 반경거리
                distance = spinner_distance.getSelectedItem().toString();
                distance = distance.replaceAll("m","");

                Log.d("onItemSelected","distance->->"+distance);
                Log.d("onItemSelected","current_latitude->->"+current_latitude);
                Log.d("onItemSelected","current_longitude->->"+current_longitude);


                /**String으로 변환 후 보내자**/
                String latitude = String.valueOf(current_latitude);
                String longtitude = String.valueOf(current_longitude);

                Log.d("onItemSelected","convert Latitude->->"+latitude);
                Log.d("onItemSelected","convert Longitude->->"+longtitude);

                locationBasedListData(distance, latitude, longtitude);


                Toast.makeText(getApplicationContext(), spinner_distance.getSelectedItem().toString(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        /**검색버튼 누를 시**/
        search_btn = (Button)findViewById(R.id.search_btn);
        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


    } //End of OnCreate


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**마시멜로 6.0 이상 권한문제**/
    protected void makeRequest() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);

    }

    //Callback for the result from requesting permissions.
    //This method is invoked for every call on requestPermissions
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {

                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                    Log.d("onRequestPermissions", "권한이 거부");
                    requestPermission();

                } else {
                    Log.d("onRequestPermissions", "권한 승인");

                    Toast.makeText(getApplicationContext(), "위치서비스 권한 설정화면으로 이동합니다", Toast.LENGTH_SHORT).show();

                    // GPS설정 화면으로 이동
                    locationControl();

                    mapFragment.getMapAsync(this);

                }
                return;
            }
        }
    }


    /**마시멜로ㅓ 6.0 이상 권한문제**/
    protected  void requestPermission() {
        /**
         * 사용자가 CALL_PHONE 권한을 거부한 적이 있는지 확인한다.
         * 거부한적이 있으면 True를 리턴하고
         * 거부한적이 없으면 False를 리턴한다.
         */
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("'위치서비스' 권한이 필요합니다")
                    .setMessage("이 기능을 사용하기 위해서는 사용자의 단말기 권한 중 '위치서비스' 권한을 허용하셔야합니다.");


            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {

                    makeRequest();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {

            makeRequest();
        }
    }
    //GPS 설정 체크
    private boolean locationControl() {

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            // GPS OFF 일때 Dialog 표시
            AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
            gsDialog.setTitle("위치 서비스 설정");
            gsDialog.setMessage("무선 네트워크 사용, GPS 위성 사용을 모두 체크하셔야 정확한 위치 서비스가 가능합니다.\n위치 서비스 기능을 설정하시겠습니까?");
            gsDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // GPS설정 화면으로 이동
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivity(intent);
                }
            })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {




                            return;
                        }
                    }).create().show();
            return false;

        } else {
            return true;
        }
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/**
 * Manipulates the map once available.
 * This callback is triggered when the map is ready to be used.
 * This is where we can add markers or lines, add listeners or move the camera. In this case,
 * we just add a marker near Sydney, Australia.
 * If Google Play services is not installed on the device, the user will be prompted to install
 * it inside the SupportMapFragment. This method will only be triggered once the user has
 * installed Google Play services and returned to the app.
 */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        LatLng SEOUL = new LatLng(37.56, 126.97); //남성역 현재위치: (37.4840014,126.9725537)

        //초기 위해 설정
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));

        //마커 추가하는 코드
        //mMap.addMarker(new MarkerOptions().position(SEOUL).title("서울"));

        //Initialize Google Play Services
        //Google Play Service 랑 연결시키겠다라는 의미
        buildGoogleApiClient();

        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);

            if (permission != PackageManager.PERMISSION_GRANTED) {

                //현재 위치로 가는 버튼 표시
                requestPermission();

            }else{

                mMap.setMyLocationEnabled(true);

            }
    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API) //장소서비스 API랑 연결하겠다는 의미
                .build();

        mGoogleApiClient.connect(); //연결시켜주기
    }

    //Interface if yor app needs to know when the automatically managed connection is established or suspended.
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        //Toast.makeText(MapsActivity.this, "onConnected!", Toast.LENGTH_SHORT).show();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Log.d("FusedLocationApi","mGoogleApiClient->"+mGoogleApiClient);
            Log.d("FusedLocationApi","mLocationRequest->"+mLocationRequest);
            Log.d("FusedLocationApi","mLastLocation->"+mLocation); //null

            //Fused Location Provider analyses GPS, Cellular and Wi-Fi network location data in order to provide the highest accuracy data.
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //유저가 있는 장소가 바뀌면 적용되게끔 (GPS를 키는 순간 Listener 가 감지해서 현재위치를 찍어준다)
    @Override
    public void onLocationChanged(Location location) {


        mLocation = location;

        Log.d("onLocationChanged","mLocationRequest->"+mLocationRequest);
        Log.d("onLocationChanged","mLastLocation->"+mLocation);
        //Location[fused 37.483992,126.972590 acc=163 et=+3d7h28m3s468ms]

        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }


        /**여기서 현재위치 받는다 (중요) !!!**/
        //위에 Log찍어본걸 보면 알겠지만 location (현재위치)의 현재 경도와 위도를 가져온것
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        current_latitude = latLng.latitude;
        current_longitude = latLng.longitude;

        Log.d("latLng위치:",""+latLng);
        //lat/lng: (37.4840014,126.9725537)

        /**Spinner에 처음 default값인 500m는 여기서 해결해줘야된다**/
        String first_latitude = String.valueOf(current_latitude);
        String first_longitutde = String.valueOf(current_longitude);

        Log.d("firstChk","first_distance->"+distance);
        Log.d("firstChk","first_latitude->"+first_latitude);
        Log.d("firstChk","first_longitutde->"+first_longitutde);
        locationBasedListData(distance, first_latitude, first_longitutde);

        //Marker 찍어주는 곳
        mCurrLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(current_latitude, current_longitude))
                .title("현재위치")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        );

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14)); //얼마나 Zoom-In을 할지에 대한 레벨!


        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently
        Toast.makeText(MapsActivity.this, "에러가 발생하였습니다", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Toast.makeText(MapsActivity.this, "onResume", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Toast.makeText(MapsActivity.this, "onPause", Toast.LENGTH_SHORT).show();
    }

////////////////////////////////////////위치기반 음식정보 가져오기///////////////////////////////////////////////////////////////
    /**
     * API 소개정보
     */
    private void locationBasedListData(final String distance2, final String latitude2, final String longitude2) {

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "http://115.71.238.215/locationBasedSearch.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {

                        Log.d("onReponse", "response-> " + response);

                        markers = new ArrayList<>();

                        try {

                            JSONArray jsonArray = new JSONArray(response);


                            Log.d("jsonObject","--------------------------->");
                            Log.d("jsonObject","jsonArrayCount->"+jsonArray.length());

                            for (int i = 0; i < jsonArray.length(); i++) {


                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                locationData = new LocationData();


                                locationData.title = jsonObject.optString("title");
                                locationData.addr1 = jsonObject.optString("addr1");
                                locationData.mapx = jsonObject.optString("mapx");
                                locationData.mapy = jsonObject.optString("mapy");
                                locationData.dist = jsonObject.optString("dist"); //거리 -> 중심좌표로부터거리(단위:m)

                                Log.d("jsonObject","--------------------------->");
                                Log.d("jsonObject","title->"+locationData.title );
                                Log.d("jsonObject","addr1->"+locationData.addr1 );
                                Log.d("jsonObject","mapx->"+locationData.mapx );
                                Log.d("jsonObject","mapy->"+locationData.mapy );
                                Log.d("jsonObject","dist->"+locationData.dist );

                                //자료구조 Arraylist에 담는다
                                markers.add(locationData);



                                Log.d("jsonObject","markers->"+markers);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.d("checkingMarkers","<-------------------->");
                        Log.d("checkingMarkers","size->"+markers.size());


                        for (int i=0; i<markers.size(); i++){

                            String title = markers.get(i).getTitle();
                            String addr1 = markers.get(i).getAddr1();
                            String mapx = markers.get(i).getMapx();
                            String mapy = markers.get(i).getMapy();

                            Double logitu = Double.parseDouble(mapx);
                            Double latitu = Double.parseDouble(mapy);

                            //Log.d("checkingMarkers","<-------------------->");
                            //Log.d("checkingMarkers","lat->"+title);
                            //Log.d("checkingMarkers","addr1->"+addr1);
                            //Log.d("checkingMarkers","mapx->"+logitu);
                            //Log.d("checkingMarkers","lot->"+latitu);

//위도 경도 위치 햇갈리지 말, 위도(세로)부터 넣고 가로(경도)
                            placeMarker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitu, logitu))
                                    .title(title)
                                    .snippet(addr1)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                            );

                            //Marker 찍어주는 곳
                            mCurrLocationMarker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(current_latitude, current_longitude))
                                    .title("현재위치")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                            );

                        }

                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {

                Log.d("getParams","--------->");
                Log.d("getParams","distance->"+distance2);
                Log.d("getParams","latitude->"+latitude2);
                Log.d("getParams","longitude->"+longitude2);


                Map<String, String> params = new HashMap<String, String>();

                params.put("distance", distance2);
                params.put("latitude", latitude2);
                params.put("longitude", longitude2);

                return params;
            }

        };

        requestQueue.add(stringRequest);
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}//End!