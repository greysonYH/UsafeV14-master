package com.example.greyson.test1.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.greyson.test1.R;
import com.example.greyson.test1.entity.DeletePinRes;
import com.example.greyson.test1.entity.GetAllPinRes;
import com.example.greyson.test1.entity.GetMyPinRes;
import com.example.greyson.test1.entity.MyMarker;
import com.example.greyson.test1.entity.RouteRes;
import com.example.greyson.test1.entity.SafePlaceRes;
import com.example.greyson.test1.entity.SavePinRes;
import com.example.greyson.test1.entity.UserPinHistory;
import com.example.greyson.test1.net.WSNetService;
import com.example.greyson.test1.ui.activity.MapSettingActivity;
import com.example.greyson.test1.ui.base.BaseFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.content.Context.MODE_PRIVATE;


/**
 * This class is used to achieve the map function.
 * Users can view the safe places and pin a event on the map
 * @author Greyson, Carson
 * @version 1.0
 */

public class SafetyMapFragment extends BaseFragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, View.OnClickListener, OnMapReadyCallback {
    private static final int REQUEST_FINE_LOCATION = 1;
    private GoogleMap googleMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    MapView mapView;
    //TextView mTvSafetyPlace;

    //private LinearLayout mLLSafePlace;
    private FloatingActionButton mFAB;
    private SharedPreferences preferences;
    private String cloLocation;
    private boolean hidePin;
    private int firstLogin;
    private LatLng latLng;

    /**
     * This method is used to initialize the map view and request the current location
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    protected View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_safetymap, container, false);

        //mLLSafePlace = (LinearLayout) view.findViewById(R.id.ll_safetyplace); // Initialize the layout uesd to call safe places map
        //mTvSafetyPlace = (TextView) view.findViewById(R.id.tv_safetyplace);
        mFAB = (FloatingActionButton) view.findViewById(R.id.fab_map);
        mFAB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mFAB.isSelected()) {
                    initPlaceMap();
                    //loadAllPinFromServer();///
                    mFAB.setImageResource(R.drawable.ic_place_black_24dp);
                    mFAB.setSelected(false);
                } else if (!mFAB.isSelected()) {
                    initPinMap();
                    mFAB.setImageResource(R.drawable.ic_arrow_back_white_24dp);
                    mFAB.setSelected(true);
                }
            }
        });

        mapView = (MapView) view.findViewById(R.id.map);  // Initialize the map view
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        try {
            MapsInitializer.initialize(mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapView.getMapAsync(this); // Make the map view ready to be used

        // Create the google api client connection
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(3000)        // 3 seconds, in milliseconds
                .setFastestInterval(3000) // 3 second, in milliseconds
                .setSmallestDisplacement(3); // 3 meter
        return view;
    }

    /**
     * This method is the defult setting of map view
     * @param mMap
     */
    @Override
    public void onMapReady(GoogleMap mMap) {
        googleMap = mMap;
        //Check the permissions
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
            }
        }
        googleMap.getUiSettings().setZoomControlsEnabled(true);  // Add zoom in/out button
        googleMap.getUiSettings().setCompassEnabled(true);       // Add compass button
        googleMap.getUiSettings().setMapToolbarEnabled(true);    // Add map tool bar
        //googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    /**
     * This map is used to execute other method when google api client connected
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            updateLocation();
            googleMap.setMyLocationEnabled(true);
            if (mFAB.isSelected() == true) {
                initPinMap();
                //mFAB.setBackground(getResources().getDrawable(R.drawable.addincident));
            }/////////////////
            else {
                initPlaceMap();
                //mFAB.setBackground(getResources().getDrawable(R.drawable.returnmap1));
            }
        }
    }

    private void moveCamera(int zoomLevel) {
        LatLng latLng = getCurrentLocation();                                             // Get the latitude and lontitude current location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));                   // Move the camera to the current location
        //googleMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel));                       // Add the defult zoom value
    }
    /**
     *  This method is used to get the safe places locations from server database and mark them
     */
    private void handleNewLocation() {
        latLng = getCurrentLocation();                                             // Get the latitude and lontitude current location
        //if (mLLSafePlace.isSelected() || mLLSafePlace.isSelected()) {
        moveCamera(14);                      // Add the defult zoom value
        //}
        Map<String, String> params = new HashMap<>();                                       // Store the params of the URL which is used to request corresponding data from server database
        params.put("lat", String.valueOf(latLng.latitude));
        params.put("lng", String.valueOf(latLng.longitude));
        mRetrofit.create(WSNetService.class)                                                 // Create a listener to observe the change of the server database and update the local data
                .getSafePlaceData(params)
                .subscribeOn(Schedulers.io())
                .compose(this.<SafePlaceRes>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<SafePlaceRes>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(SafePlaceRes safePlaceRes) {                       // The action if the update is running
                        showMarker(safePlaceRes);
                        requestRoute(latLng);
                    }
                });
        loadAllPinFromServer();//
    }

    /**
     *  This method is used to get the safe places locations from server database and mark them
     */
    private void handleNewLocationUpdate() {
        latLng = getCurrentLocation();                                             // Get the latitude and lontitude current location
        Map<String, String> params = new HashMap<>();                                       // Store the params of the URL which is used to request corresponding data from server database
        params.put("lat", String.valueOf(latLng.latitude));
        params.put("lng", String.valueOf(latLng.longitude));
        mRetrofit.create(WSNetService.class)                                                 // Create a listener to observe the change of the server database and update the local data
                .getSafePlaceData(params)
                .subscribeOn(Schedulers.io())
                .compose(this.<SafePlaceRes>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<SafePlaceRes>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(SafePlaceRes safePlaceRes) {                       // The action if the update is running
                        showMarker(safePlaceRes);
                        requestRoute(latLng);
                    }
                });
        loadAllPinFromServer();//
    }

    private void requestRoute(LatLng latLng) {
        Map<String, String> params2 = new HashMap<>();
        String ori = String.valueOf(latLng.latitude) + "," + String.valueOf(latLng.longitude);
        //String ori = "-37.8767,145.0423";
        String des = cloLocation;
        //String des = "-37.876706,145.042316";
        params2.put("origin", ori);
        params2.put("destination", des);
        params2.put("mode", "walking");
        params2.put("key", "AIzaSyAYPtaZmfpFdvNd3_-ur4X2Bvn-35uVoAQ");
        mRetrofit2.create(WSNetService.class)
                .getSafePlaceRoute(params2)
                .subscribeOn(Schedulers.io())
                .compose(this.<RouteRes>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<RouteRes>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(RouteRes routeRes) {
                        showRoute(routeRes);
                    }
                });
    }

    /**
     * This method is to show the closest route
     * @param routeRes
     */
    private void showRoute(RouteRes routeRes) {
        PolylineOptions polylineOpt = new PolylineOptions();
        try {
            for (RouteRes.RoutesBean rRes : routeRes.getRoutes()) {
                for (RouteRes.RoutesBean.LegsBean lRes : rRes.getLegs()) {
                    for (RouteRes.RoutesBean.LegsBean.StepsBean sRes : lRes.getSteps()) {
                        String polyline = "";
                        polyline = sRes.getPolyline().getPoints();
                        List<LatLng> list = decodePoly(polyline);
                        for (int l = 0; l < list.size(); l++) {
                            Double lat = list.get(l).latitude;   // The closest location
                            Double lng = list.get(l).longitude;  // The closest location
                            polylineOpt.add(new LatLng(lat, lng));
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        polylineOpt.color(R.color.welcomeMenuBg);
        Polyline line = googleMap.addPolyline(polylineOpt);
        line.setWidth(18);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    /**
     * This method is used to mark the locations of the safe places
     * @param safePlaceRes
     */
    private void showMarker(SafePlaceRes safePlaceRes) {
        String message = safePlaceRes.getMessage();
        // Decide range
        if (!mFAB.isSelected() && message.equalsIgnoreCase("5 KM")) {
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
            showDialog("Sorry, There are no Safe Place in 2KM, Change range to 5KM");
            //Toast.makeText(mContext, "There are no Safe Place in 2KM, Change range to 5KM", Toast.LENGTH_LONG).show();
        } else if (message.equalsIgnoreCase("Nothing found")) {
            showDialog("Sorry, There are no Safe Place in 5KM");
            //Toast.makeText(mContext, "There are no Safe Place in 5KM", Toast.LENGTH_LONG).show();
        }
        // Decide different icon
        for (SafePlaceRes.ResultsBean sfRes : safePlaceRes.getResults()) {
            String lat = sfRes.getLatitude();
            String lng = sfRes.getLongitude();
            String type = sfRes.getType();
            String establishment = sfRes.getEstablishment();
            String address = sfRes.getAddress() + sfRes.getSuburb() + sfRes.getState();
            cloLocation = lat + "," + lng;
            switch (type) {
                case "Firestation":
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.valueOf(lat), Double.valueOf(lng)))
                            .title(establishment)
                            .snippet(address)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.safeplace_f)));
                    break;
                case "Convenience Shop":
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.valueOf(lat), Double.valueOf(lng)))
                            .title(establishment)
                            .snippet(address)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.safeplace_7)));
                    break;
                case "Petrol Station":
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.valueOf(lat), Double.valueOf(lng)))
                            .title(establishment)
                            .snippet(address)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.safeplace_g)));
                    break;
                case "Restaurant":
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.valueOf(lat), Double.valueOf(lng)))
                            .title(establishment)
                            .snippet(address)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.safeplace_m)));
                    break;
                case "Police Station":
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.valueOf(lat), Double.valueOf(lng)))
                            .title(establishment)
                            .snippet(address)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.safeplace_p)));
                    break;
                case "Hospital":
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.valueOf(lat), Double.valueOf(lng)))
                            .title(establishment)
                            .snippet(address)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.safeplace_h)));
                    break;
                case "Supermarket":
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.valueOf(lat), Double.valueOf(lng)))
                            .title(establishment)
                            .snippet(address)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.safeplace_s)));
                    break;
                default:
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.valueOf(lat), Double.valueOf(lng)))
                            .title(establishment));


            }
            //googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(type));

        }
    }

    private void showDialog(String text) {
        new SweetAlertDialog(mContext, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText("Sorry")
                .setContentText(text)
                .show();
    }
    /**
     * This method s used to get the current location
     * @return
     */
    private LatLng getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_FINE_LOCATION);
            }
        }
        //LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, this);////////
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        double currentLatitude = location.getLatitude();     // Get laititude
        double currentLongitude = location.getLongitude();   // Get laititude
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        saveLastLocationToSharedPreference(latLng);
        return latLng;
    }

    /**
     * This method to send current location
     * @param latLng
     */
    private void saveLastLocationToSharedPreference(LatLng latLng) {
        String lat = String.valueOf(latLng.latitude);
        String lng = String.valueOf(latLng.longitude);
        SharedPreferences preferences1 = mContext.getSharedPreferences("LastLocation", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences1.edit();
        editor.putString("last location", lat + "," + lng);
        editor.commit();
    }


    /**
     * This method is to request permissions
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                    PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    googleMap.setMyLocationEnabled(true);
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                    initPlaceMap();
                } else {
                    getActivity().finish();
                }
                return;
            }
        }
    }

    /**
     * This method is to send pin info
     */
    @Override
    protected void initEvent() {
        mFAB.setSelected(false);
    }

    /**
     * This method is the action of selecting the two layout
     * @param v
     */
    @Override
    public void onClick(View v) {
        //mLLSafePlace.setSelected(false);
        //mLLSafePlace.setSelected(false);
        // The action if one of two layouts is activated
        switch (v.getId()) {
            case R.id.ll_safetyplace:
                if (mFAB.isSelected()) {
                    showAllMyPin();
                    mFAB.setImageResource(R.drawable.ic_place_black_24dp);
                    mFAB.setSelected(false);
                } else if (!mFAB.isSelected()) {
                    initPlaceMap();
                }
                break;
        }
    }

    private void showAllMyPin() {
        googleMap.clear();
        moveCamera(10);
        showMyPinFromServer();
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (mFAB.isSelected() == true) {
                    //marker.setSnippet("Click here for setting");//
                    marker.showInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        });
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (mFAB.isSelected() == true) {
                    sendPinStatus(marker);
                }
            }
        });
    }

    private void showAllMyPins() {
        googleMap.clear();
        LatLng latLng = getCurrentLocation();
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
        preferences = mContext.getSharedPreferences("LocalUser", MODE_PRIVATE);
        showMarkerFromSharedPreference(getObjectFromSharedPreference("admin"));

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (mFAB.isSelected() == true) {
                    //marker.setSnippet("Click here for setting");//
                    marker.showInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        });

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (mFAB.isSelected() == true) {
                    sendPinStatus(marker);
                }
            }
        });
    }

    /**
     * This method is used to initialize the map view of safe places
     */
    private void initPlaceMap() {
        googleMap.clear();
        moveCamera(14);
        handleNewLocation();
    }

    private void showMyPinMap(GetMyPinRes getMyPinRes) {
        List<GetMyPinRes.ResultsBean> resultsBeanList = getMyPinRes.getResults();
        Iterator<GetMyPinRes.ResultsBean> iterator = resultsBeanList.iterator();
        while (iterator.hasNext()) {
            GetMyPinRes.ResultsBean resultsBean = iterator.next();
            LatLng l = new LatLng(Double.valueOf(resultsBean.getLatitude()), Double.valueOf(resultsBean.getLongitude()));
            Marker marker = googleMap.addMarker(new MarkerOptions().position(l));
            setMarkerColor(marker, resultsBean.getCrime());
            marker.setSnippet(resultsBean.getCrimedesc());
            marker.setTag(resultsBean.getDeviceid());
            //marker.showInfoWindow();
            //marker.setZIndex((float)(Integer.valueOf(resultsBean.getDeviceid())));///////
        }
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (mFAB.isSelected() == true) {
                    //marker.setSnippet("Click here for setting");//
                    marker.showInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        });

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (mFAB.isSelected() == true ) {
                    if (marker.getTag() != null && !marker.getTag().toString().isEmpty()) {
                        sendPinStatus(marker);
                    }
                }
            }
        });
    }

    private void showAllPinMap(GetAllPinRes getAllPinRes) {
        List<GetAllPinRes.ResultsBean> resultsBeanList = getAllPinRes.getResults();
        Iterator<GetAllPinRes.ResultsBean> iterator = resultsBeanList.iterator();
        while (iterator.hasNext()) {
            GetAllPinRes.ResultsBean resultsBean = iterator.next();
            LatLng l = new LatLng(Double.valueOf(resultsBean.getLatitude()), Double.valueOf(resultsBean.getLongitude()));
            Marker marker = googleMap.addMarker(new MarkerOptions().position(l));
            setMarkerColor(marker, resultsBean.getCrime());
            marker.setSnippet(resultsBean.getCrimedesc());
            marker.setTag(resultsBean.getDeviceid());///////
            //marker.setZIndex((float)(Integer.valueOf(resultsBean.getDeviceid())));///////
            marker.showInfoWindow();
        }
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (mFAB.isSelected() == true) {
                    //marker.setSnippet("Click here for setting");//
                    marker.showInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        });

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (mFAB.isSelected() == true) {///////////
                    sendPinStatus(marker);
                }
            }
        });
    }

    private void showPinMap() {
        preferences = mContext.getSharedPreferences("LocalUser", MODE_PRIVATE);
        showMarkerFromSharedPreference(getObjectFromSharedPreference("admin"));
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (mFAB.isSelected() == true) {
                    //marker.setSnippet("Click here for setting");//
                    marker.showInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        });

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (mFAB.isSelected() == true) {
                    sendPinStatus(marker);
                }
            }
        });
    }

    /**
     * This method is to initialize the map view of pin
     */
    private void initPinMap() {
        googleMap.clear();
        moveCamera(18);
        //preferences = mContext.getSharedPreferences("LocalUser",MODE_PRIVATE);
        //showMarkerFromSharedPreference(getObjectFromSharedPreference("admin"));
        Marker pinMarker = googleMap.addMarker(new MarkerOptions().position(latLng)
                .draggable(true).title("New Pin").snippet("Long Press to Drag")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
        pinMarker.showInfoWindow();
        pinMarker.setTag("new");
        showMyPinFromServer();

        Map<String, String> params = new HashMap<>();                                       // Store the params of the URL which is used to request corresponding data from server database
        params.put("lat", String.valueOf(latLng.latitude));
        params.put("lng", String.valueOf(latLng.longitude));
        mRetrofit.create(WSNetService.class)                                                 // Create a listener to observe the change of the server database and update the local data
                .getSafePlaceData(params)
                .subscribeOn(Schedulers.io())
                .compose(this.<SafePlaceRes>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<SafePlaceRes>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(SafePlaceRes safePlaceRes) {                       // The action if the update is running
                        showMarker(safePlaceRes);
                    }
                });


        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                marker.setSnippet("Drag me");
                marker.showInfoWindow();
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                marker.setSnippet("Drop me");
                marker.showInfoWindow();
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                marker.setSnippet("Tap here to Edit");
                marker.showInfoWindow();
            }
        });

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (mFAB.isSelected() == true) {
                    //marker.setSnippet("Click here for setting");//
                    marker.showInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        });

        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (mFAB.isSelected() == true ) {
                    sendPinStatus(marker);
                }
            }
        });
    }

    /**
     * This method is to send pin information
     * @param marker
     */
    public void sendPinStatus(Marker marker) {
        Intent intent = new Intent();
        intent.setClass(mContext, MapSettingActivity.class);
        String markerTag;
        String markerStatus;
        markerTag = (String) marker.getTag();
        markerStatus = "old";
        if (markerTag == "new") {
            markerStatus = "new";
            markerTag = "1";
            marker.setSnippet("");
        }
        intent.putExtra("status", markerStatus);
        intent.putExtra("tag", markerTag);
        intent.putExtra("lat", marker.getPosition().latitude);
        intent.putExtra("lng", marker.getPosition().longitude);
        intent.putExtra("note", marker.getSnippet());
        intent.putExtra("deviceId", getDeviceId() + getTimeStamp());//////////////////////
        startActivityForResult(intent, 1); // send info to other activity
    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;}
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }
    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {

        } else if (resultCode == 1) {
            //handleDeletePin(data);
            handleDeletePinFromServer(data);
            //initPinMap();
        } else if (resultCode == 2) {
            //handleSavePin(data);
            savePinToServer(data);
            //initPinMap();
        }
    }

    private void showMyPinFromServer() {
        Map<String, String> params = new HashMap<>();
        params.put("deviceid", getDeviceId());
        mRetrofit.create(WSNetService.class)
                .getMyPinData("http://usafe.epnjkefarc.us-west-2.elasticbeanstalk.com/api/posts/?format=json",params)
                .subscribeOn(Schedulers.io())
                .compose(this.<GetMyPinRes>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<GetMyPinRes>() {
                    @Override
                    public void onCompleted() {
                    }
                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(GetMyPinRes getMyPinRes) {
                        showMyPinMap(getMyPinRes);
                    }
                });
    }

    private void handleDeletePinFromServer(Intent data) {
        Bundle b = data.getExtras();
        String deviceId = b.getString("tag");
        deletePinFromServer(deviceId);
    }

    private String getDeviceId() {
        TelephonyManager tManager = (TelephonyManager) mContext.getSystemService(mContext.TELEPHONY_SERVICE);
        String uid = tManager.getDeviceId();
        return uid;
    }

    private String getTimeStamp() {
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        return ts;
    }

    private void loadAllPinFromServer() {
        Map<String, String> params = new HashMap<>();
        mRetrofit.create(WSNetService.class)
                .getAllPinData("http://usafe.epnjkefarc.us-west-2.elasticbeanstalk.com/api/posts/?format=json",params)
                .subscribeOn(Schedulers.io())
                .compose(this.<GetAllPinRes>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<GetAllPinRes>() {
                    @Override
                    public void onCompleted() {
                    }
                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(GetAllPinRes getAllPinRes) {
                        showAllPinMap(getAllPinRes);
                    }
                });
    }

    private void savePinToServer(Intent data) {
        Bundle b = data.getExtras();
        String deviceId = b.getString("deviceId");
        String color = b.getString("color");
        String type = color.split(" ")[0];
        String note = b.getString("note");
        Double lat = b.getDouble("lat");
        Double lng = b.getDouble("lng");
        String tag = b.getString("tag");

        String pinStatus = b.getString("status");

        if (pinStatus.equals("old")) {
            updatePinToServer(tag,type,note,lat,lng);////
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("deviceid", getDeviceId() + getTimeStamp());
        params.put("crime", type);
        params.put("crimedesc", note);
        params.put("lat", String.valueOf(lat));
        params.put("lng", String .valueOf(lng));
        mRetrofit.create(WSNetService.class)
                .getSavePinData("http://usafe.epnjkefarc.us-west-2.elasticbeanstalk.com/api/posts/createincident/?",params)
                .subscribeOn(Schedulers.io())
                .compose(this.<SavePinRes>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<SavePinRes>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(SavePinRes savePinRes) {

                    }
                });
    }

    private void updatePinToServer(String deviceId, String crime, String crimedesc, double lat, double lng) {
        Map<String, String> params = new HashMap<>();
        params.put("deviceid", deviceId);
        params.put("crime", crime);
        params.put("crimedesc", crimedesc);
        params.put("lat", String.valueOf(lat));
        params.put("lng", String .valueOf(lng));
        mRetrofit.create(WSNetService.class)
                .getSavePinData("http://usafe.epnjkefarc.us-west-2.elasticbeanstalk.com/api/posts/update/?",params)
                .subscribeOn(Schedulers.io())
                .compose(this.<SavePinRes>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<SavePinRes>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(SavePinRes savePinRes) {

                    }
                });
    }

    private void deletePinFromServer(String id) {
        Map<String, String> params = new HashMap<>();
        params.put("deviceid", id);
        mRetrofit.create(WSNetService.class)
                .getDeletePinData("http://usafe.epnjkefarc.us-west-2.elasticbeanstalk.com/api/posts/delete/?",params)
                .subscribeOn(Schedulers.io())
                .compose(this.<DeletePinRes>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DeletePinRes>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(DeletePinRes deletePinRes) {

                    }
                });
    }
    public void saveObjectToSharedPreference(String key, Object obj) {
        Gson gson = new Gson();
        String jsonObj = gson.toJson(obj);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, jsonObj);
        editor.commit();
    }

    public UserPinHistory getObjectFromSharedPreference(String key) {

        Gson gson = new Gson();
        String str = preferences.getString(key, null);
        if (str == null) {//first empty
            return new UserPinHistory();
        } else {
            try {
                return gson.fromJson(str, UserPinHistory.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Can not get object with key " + key);
            }
        }
    }

    private void showMarkerFromSharedPreference(UserPinHistory pinHistory) {
        ArrayList<MyMarker> myMarkerList = pinHistory.getMmk();
        Iterator<MyMarker> iterator = myMarkerList.iterator();
        int count = 0;
        while(iterator.hasNext()){
            MyMarker mk = iterator.next();
            LatLng l =new LatLng(mk.getMkLat(),mk.getMkLnt());
            Marker marker = googleMap.addMarker(new MarkerOptions().position(l));
            setMarkerColor(marker, mk.getMkColor());
            marker.setSnippet(mk.getMkDescription());
            marker.setTag(String.valueOf(count));
            mk.setMkTag(String.valueOf(count));//
            marker.showInfoWindow();
            count ++;
        }
        UserPinHistory latestPinHistory = new UserPinHistory();
        latestPinHistory.setMmk(myMarkerList);
        saveObjectToSharedPreference("admin",latestPinHistory);
    }


    /**
     * This method is to set color of pins
     * @param marker
     * @param color
     */
    private void setMarkerColor(Marker marker, String color) {
        switch (color) {
            case "Assault (Red)":case "Assault":
                marker.setTitle("Assault");
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(10));
                break;
            case "Discrimination (Purple)":case "Discrimination":
                marker.setTitle("Discrimination");
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(280));
                break;
            case "Stalking (Blue)":case "Stalking":
                marker.setTitle("Stalking");
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(230));
                break;
            case "Theft/Robbery (Green)":case "Theft/Robbery":
                marker.setTitle("Theft/Robbery");
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(90));
                break;
            case "Others (Yellow)":case "Others":
                marker.setTitle("Others");
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(70));
                break;
            default:
                marker.setTitle("Others");
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                break;
        }
    }
    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * This is to monitor location change
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        if (!mFAB.isSelected() && firstLogin == 1) {
            handleNewLocation();
            firstLogin = 0;
        } else if (!mFAB.isSelected() && !this.isHidden()) {
                googleMap.clear();
                handleNewLocationUpdate();
        }
    }

    /**
     * This method called when the activity will start interacting with the user.
     */
    @Override
    public void onResume() {
        firstLogin = 1;
        super.onResume();
        mGoogleApiClient.connect();
    }

    /**
     * This method called when the system is about to start resuming a previous activity.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void destroyView() {

    }
}
