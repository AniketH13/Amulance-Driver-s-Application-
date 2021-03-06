package com.hyozu.example.ems.ui.home;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hyozu.example.ems.Common;
import com.hyozu.example.ems.EventBus.DriverRequestReceived;
import com.hyozu.example.ems.EventBus.NotifyToPatientEvent;
import com.hyozu.example.ems.Model.PatientModel;
import com.hyozu.example.ems.Model.TripPlanModel;
import com.hyozu.example.ems.R;
import com.hyozu.example.ems.Remote.IGoogleAPI;
import com.hyozu.example.ems.Remote.RetrofitClient;
import com.hyozu.example.ems.Utils.UserUtils;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.kusu.library.LoadingButton;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    @BindView(R.id.chip_decline)
    Chip chip_decline;
    @BindView(R.id.layout_accept)
    CardView layout_accept;
    @BindView(R.id.circularProgressBar)
    CircularProgressBar circularProgressBar;
    @BindView(R.id.estimate_time_text)
    TextView estimate_time_text;
    @BindView(R.id.estimate_distance_text)
    TextView estimate_distance_text;
    @BindView(R.id.root_layout)
    FrameLayout root_layout;

    @BindView(R.id.rating_driver)
    TextView rating_driver;
    @BindView(R.id.type_ambulance)
    TextView type_ambulance;
    @BindView(R.id.round_img)
    ImageView round_img;
    @BindView(R.id.layout_start_Ride)
    CardView layout_start_Ride;
    @BindView(R.id.txt_Patient_name)
    TextView txt_Patient_name;
    @BindView(R.id.txt_start_ride_estimate_distance)
    TextView txt_start_ride_estimate_distance;
    @BindView(R.id.txt_start_ride_estimate_time)
    TextView txt_start_ride_estimate_time;
    @BindView(R.id.img_phone_msg)
    ImageView img_phone_msg;
    @BindView(R.id.btn_start_ride)
    LoadingButton btn_start_ride;
    @BindView(R.id.btn_trip_complete)
    LoadingButton btn_trip_complete;

    @BindView(R.id.layout_notify_patient)
    LinearLayout layout_notify_patient;
    @BindView(R.id.notify_patient_txt)
    TextView notify_patient_txt;
    @BindView(R.id.progress_notify)
    ProgressBar progress_notify;


    private String tripNumberId = " ";
    private boolean isTripStart = false, onlineSystemAlreadyRegister = false;

    private GeoFire pickupGeoFire, destinationGeoFire;
    private GeoQuery pickupGeoQuery, destinationGeoQuery;

    private GeoQueryEventListener pickupGeoQueryListener = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            btn_start_ride.setEnabled(true); // when driver arrived location, they can start
            UserUtils.sendNotifyToRider(getContext(), root_layout, key);
            if (pickupGeoQuery != null) {
                pickupGeoFire.removeLocation(key);
                pickupGeoFire = null;
                pickupGeoQuery.removeAllListeners();
            }
        }

        @Override
        public void onKeyExited(String key) {
            btn_start_ride.setEnabled(false);

        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };

    private GeoQueryEventListener destinationGeoQuaryListener = new GeoQueryEventListener() {
        @Override
        public void onKeyEntered(String key, GeoLocation location) {
            Toast.makeText(getContext(), "Destination Entered!!", Toast.LENGTH_SHORT).show();
            btn_trip_complete.setEnabled(true);
            if (destinationGeoQuery != null) {
                destinationGeoFire.removeLocation(key);
                destinationGeoFire = null;
                destinationGeoQuery.removeAllListeners();
            }

        }

        @Override
        public void onKeyExited(String key) {

        }

        @Override
        public void onKeyMoved(String key, GeoLocation location) {

        }

        @Override
        public void onGeoQueryReady() {

        }

        @Override
        public void onGeoQueryError(DatabaseError error) {

        }
    };

    private CountDownTimer waiting_timer;

    @OnClick(R.id.chip_decline)
    void onDeclineClick() {
        if (driverRequestReceived != null) {
            if (TextUtils.isEmpty(tripNumberId)) {
                if (countDownEvent != null)
                    countDownEvent.dispose();
                chip_decline.setVisibility(View.GONE);
                layout_accept.setVisibility(View.GONE);
                mMap.clear();
                circularProgressBar.setProgress(0);
                UserUtils.sendDeclineRequest(root_layout, getContext(), driverRequestReceived.getKey());
                driverRequestReceived = null;

            } else {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(mapFragment.getView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show();
                    return;
                }
                fusedLocationProviderClient.getLastLocation()
                        .addOnFailureListener(e -> {
                            Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();

                        })
                        .addOnSuccessListener(location -> {
                            chip_decline.setVisibility(View.GONE);
                            layout_start_Ride.setVisibility(View.GONE);
                            mMap.clear();
                            UserUtils.sendDeclineAndRemoveTripRequest(root_layout, getContext(), driverRequestReceived.getKey()
                                    , tripNumberId);
                            tripNumberId = "";
                            driverRequestReceived = null;
                            makeDriverOnline(location);
                        });
            }

        }
    }

    @OnClick(R.id.btn_start_ride)
    void onStartRideClick() {
        if (blackPolyline != null) blackPolyline.remove();
        if (greyPolyline != null) greyPolyline.remove();
        //Cancel waiting timer
        if (waiting_timer != null) waiting_timer.cancel();
        layout_notify_patient.setVisibility(View.GONE);
        if (driverRequestReceived != null) {
            LatLng destinationLatLng = new LatLng(
                    Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[0]),
                    Double.parseDouble(driverRequestReceived.getDestinationLocation().split(",")[1])
            );
            mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title(driverRequestReceived.getDestinationLocationString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
            drawPathFromCurrentLocation(driverRequestReceived.getDestinationLocation());
        }
        btn_start_ride.setVisibility(View.GONE);
        chip_decline.setVisibility(View.GONE);
        btn_trip_complete.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.btn_trip_complete)
    void onCompleteTripClick() {
        //Toast.makeText(getContext(), "Complete Trip fake action", Toast.LENGTH_SHORT).show();
        //updating trip set done to true
        Map<String, Object> update_trip = new HashMap<>();
        update_trip.put("Trip done", true);
        FirebaseDatabase.getInstance()
                .getReference(Common.Trip)
                .child(tripNumberId)
                .updateChildren(update_trip)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(mapFragment.requireView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnSuccessListener(aVoid -> {

                    //Get location
                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Snackbar.make(mapFragment.requireView(), getContext().getString(R.string.permission_require), Snackbar.LENGTH_LONG);
                        return;
                    }
                    fusedLocationProviderClient.getLastLocation()
                            .addOnFailureListener(e -> {
                                Snackbar.make(mapFragment.requireView(),e.getMessage(), Snackbar.LENGTH_LONG);

                            }).addOnSuccessListener(location -> {
                                UserUtils.sendCompleteTripTOPatient(mapFragment.requireView(),getContext(),driverRequestReceived.getKey(),
                        tripNumberId);
                                //clearing map
                        mMap.clear();
                        tripNumberId="";
                        isTripStart = false;
                        chip_decline.setVisibility(View.GONE);

                        layout_accept.setVisibility(View.GONE);
                        circularProgressBar.setProgress(0);

                        layout_start_Ride.setVisibility(View.GONE);

                        layout_notify_patient.setVisibility(View.GONE);
                        progress_notify.setProgress(0);

                        btn_trip_complete.setEnabled(false);
                        btn_trip_complete.setVisibility(View.GONE);

                        btn_start_ride.setEnabled(false);
                        btn_start_ride.setVisibility(View.VISIBLE);

                        destinationGeoFire = null;
                        pickupGeoFire = null;

                        driverRequestReceived = null;
                        makeDriverOnline(location);

                            });
                });

    }

    private void drawPathFromCurrentLocation(String destinationLocation) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(requireView(), getString(R.string.permission_require), Snackbar.LENGTH_LONG).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> Snackbar.make(requireView(), e.getMessage(), Snackbar.LENGTH_LONG).show())
                .addOnSuccessListener(location -> {

                    compositeDisposable.add(iGoogleAPI.getDirections("driving", "less_driving",
                            new StringBuilder()
                                    .append(location.getLatitude())
                                    .append(",")
                                    .append(location.getLongitude())
                                    .toString(),
                            destinationLocation,
                            getString(R.string.google_api_key))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(returnResult -> {


                                try {
                                    //parse Json
                                    JSONObject jsonObject = new JSONObject(returnResult);
                                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject route = jsonArray.getJSONObject(i);
                                        JSONObject poly = route.getJSONObject("overview_polyline");
                                        String polyline = poly.getString("points");
                                        polylineList = Common.decodepoly(polyline);
                                    }

                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color(Color.GRAY);
                                    polylineOptions.width(12);
                                    polylineOptions.startCap(new SquareCap());
                                    polylineOptions.jointType(JointType.ROUND);
                                    polylineOptions.addAll(polylineList);
                                    greyPolyline = mMap.addPolyline(polylineOptions);

                                    blackPolylineOptions = new PolylineOptions();
                                    blackPolylineOptions.color(Color.BLACK);
                                    blackPolylineOptions.width(5);
                                    blackPolylineOptions.startCap(new SquareCap());
                                    blackPolylineOptions.jointType(JointType.ROUND);
                                    blackPolylineOptions.addAll(polylineList);
                                    blackPolyline = mMap.addPolyline(blackPolylineOptions);


                                    LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                                    LatLng destination = new LatLng(Double.parseDouble(destinationLocation.split(",")[0]),
                                            Double.parseDouble(destinationLocation.split(",")[1]));

                                    LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                            .include(origin)
                                            .include(destination)
                                            .build();

                                    createGeoFireDestinationLocation(driverRequestReceived.getKey(),destination);


                                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 1));


                                } catch (Exception e) {
                                    // Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_SHORT).show();
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            })
                    );

                });

    }

    private void createGeoFireDestinationLocation(String key, LatLng destination) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Common.TRIP_DESTINATION_LOCATION_REF);
        destinationGeoFire = new GeoFire(ref);
        destinationGeoFire.setLocation(key, new GeoLocation(destination.latitude, destination.longitude),
                (key1, error) -> {
                    
                });
    }

    //Routes
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;
    private Polyline blackPolyline, greyPolyline;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private List<LatLng> polylineList;

    private Disposable countDownEvent;
    private DriverRequestReceived driverRequestReceived;

    private GoogleMap mMap;

    private HomeViewModel homeViewModel;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    SupportMapFragment mapFragment;
    private boolean isFirstTime = true;


    //online system
    DatabaseReference onlineRef, currentUserRef, driversLocationRef;
    GeoFire geoFire;
    ValueEventListener onlineValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot databaseSnapshot) {
            if (databaseSnapshot.exists() && currentUserRef != null) {
                currentUserRef.onDisconnect().removeValue();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Snackbar.make(mapFragment.getView(), databaseError.getMessage(), Snackbar.LENGTH_LONG)
                    .show();
        }
    };

    @Override
    public void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        geoFire.removeLocation(FirebaseAuth.getInstance().getCurrentUser().getUid());
        onlineRef.removeEventListener(onlineValueEventListener);

        if (EventBus.getDefault().hasSubscriberForEvent(DriverRequestReceived.class))
            EventBus.getDefault().removeStickyEvent(DriverRequestReceived.class);
        if (EventBus.getDefault().hasSubscriberForEvent(NotifyToPatientEvent.class))
            EventBus.getDefault().removeStickyEvent(NotifyToPatientEvent.class);
        EventBus.getDefault().unregister(this);

        compositeDisposable.clear();

        onlineSystemAlreadyRegister=false;
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerOnlineSystem();
    }

    private void registerOnlineSystem() {
        if (!onlineSystemAlreadyRegister)
        {
            onlineRef.addValueEventListener(onlineValueEventListener);
            onlineSystemAlreadyRegister = true;
        }

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(root);
        init();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return root;
    }

    private void initViews(View root) {
        ButterKnife.bind(this, root);
    }

    private void init() {

        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);

        onlineRef = FirebaseDatabase.getInstance().getReference().child("info/connected");

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(root_layout, getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show();
            return;
        }


        buildLocationRequest();
        buildLocationCallback();
        updateLocation();

    }

    private void updateLocation() {
        if (fusedLocationProviderClient == null) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    &&
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(getView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show();
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        }
    }

    private void buildLocationCallback() {
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);

                    LatLng newposition = new LatLng(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude());

                    if (pickupGeoFire !=null)
                    {
                        pickupGeoQuery = pickupGeoFire.queryAtLocation(new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                locationResult.getLastLocation().getLongitude()),Common.MIN_RANGE_PICKUP_IN_KM);
                        pickupGeoQuery.addGeoQueryEventListener(pickupGeoQueryListener);
                    }
                    //Destination
                    if (destinationGeoFire !=null)
                    {
                        destinationGeoQuery = destinationGeoFire.queryAtLocation(new GeoLocation(locationResult.getLastLocation().getLatitude(),
                                locationResult.getLastLocation().getLongitude()),Common.MIN_RANGE_PICKUP_IN_KM);
                        destinationGeoQuery.addGeoQueryEventListener(destinationGeoQuaryListener);
                    }

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newposition, 18f));

                    if (!isTripStart) {
                        makeDriverOnline(locationResult.getLastLocation());


                    }
                    else
                    {
                        if (!TextUtils.isEmpty(tripNumberId))
                        {
                            Map<String,Object> update_data = new HashMap<>();
                            update_data.put("currentLat",locationResult.getLastLocation().getLatitude());
                            update_data.put("currentLng",locationResult.getLastLocation().getLongitude());

                            FirebaseDatabase.getInstance()
                                    .getReference(Common.Trip)
                                    .child(tripNumberId)
                                    .updateChildren(update_data)
                                    .addOnFailureListener(e -> Snackbar.make(mapFragment.getView(),e.getMessage(),Snackbar.LENGTH_SHORT).show())
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                }
                            });
                        }

                    }
                }
            };
        }
    }

    private void makeDriverOnline(Location location) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        List<Address> addressList;
        try {
            addressList = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1);
            String CityName = addressList.get(0).getLocality();

            driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVER_LOCATION_REFERENCE)
                    .child(CityName);
            currentUserRef = driversLocationRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            geoFire = new GeoFire(driversLocationRef);


            //update location
            geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                    new GeoLocation(location.getLatitude(),
                            location.getLongitude()),
                    (key, error) -> {
                        if (error != null)
                            Snackbar.make(mapFragment.getView(), error.getMessage(), Snackbar.LENGTH_LONG)
                                    .show();

                    });
            registerOnlineSystem();
        } catch (IOException e) {
            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void buildLocationRequest() {
        if (locationRequest == null) {
            locationRequest = new LocationRequest();
            locationRequest.setSmallestDisplacement(50f); // 50 metre
            locationRequest.setInterval(15000); //15second
            locationRequest.setFastestInterval(10000); //10 second
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        //checking permission
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                &&
                                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Snackbar.make(getView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        mMap.setOnMyLocationButtonClickListener(() -> {
                            fusedLocationProviderClient.getLastLocation()
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                    .addOnSuccessListener(location -> {
                                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));
                                    });
                            return true;
                        });

                        //set Layout button
                        View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).getParent())
                                .findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                        //Right bottom
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        params.setMargins(0, 0, 0, 250);


                        //Location moving
                        buildLocationRequest();
                        buildLocationCallback();
                        updateLocation();
                    }


                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getContext(), "Permission" + permissionDeniedResponse.getPermissionName() + "" +
                                "was denied!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
        mMap.getUiSettings().setZoomControlsEnabled(true);
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style));
            if (!success)
                Log.e("My_Error", "style parsing error");
        } catch (Resources.NotFoundException e) {
            Log.e("My_error", e.getMessage());
        }

        Snackbar.make(mapFragment.getView(), "Welcome!! You are online", Snackbar.LENGTH_LONG)
                .show();
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDriverRequestReceive(DriverRequestReceived event) {

        driverRequestReceived = event;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(requireView(), getString(R.string.permission_require), Snackbar.LENGTH_LONG).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> Snackbar.make(requireView(), e.getMessage(), Snackbar.LENGTH_LONG).show())
                .addOnSuccessListener(location -> {

                    compositeDisposable.add(iGoogleAPI.getDirections("driving", "less_driving",
                            new StringBuilder()
                                    .append(location.getLatitude())
                                    .append(",")
                                    .append(location.getLongitude())
                                    .toString(),
                            event.getPickupLocation(),
                            getString(R.string.google_api_key))
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<String>() {
                                @Override
                                public void accept(String returnResult) throws Exception {


                                    try {
                                        //parse Json
                                        JSONObject jsonObject = new JSONObject(returnResult);
                                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            JSONObject route = jsonArray.getJSONObject(i);
                                            JSONObject poly = route.getJSONObject("overview_polyline");
                                            String polyline = poly.getString("points");
                                            polylineList = Common.decodepoly(polyline);
                                        }

                                        polylineOptions = new PolylineOptions();
                                        polylineOptions.color(Color.GRAY);
                                        polylineOptions.width(12);
                                        polylineOptions.startCap(new SquareCap());
                                        polylineOptions.jointType(JointType.ROUND);
                                        polylineOptions.addAll(polylineList);
                                        greyPolyline = mMap.addPolyline(polylineOptions);

                                        blackPolylineOptions = new PolylineOptions();
                                        blackPolylineOptions.color(Color.BLACK);
                                        blackPolylineOptions.width(5);
                                        blackPolylineOptions.startCap(new SquareCap());
                                        blackPolylineOptions.jointType(JointType.ROUND);
                                        blackPolylineOptions.addAll(polylineList);
                                        blackPolyline = mMap.addPolyline(blackPolylineOptions);

                                        //Animator
                                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 100);
                                        valueAnimator.setDuration(1100);
                                        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
                                        valueAnimator.setInterpolator(new LinearInterpolator());
                                        valueAnimator.addUpdateListener(value -> {
                                            List<LatLng> points = greyPolyline.getPoints();
                                            int percentValue = (int) value.getAnimatedValue();
                                            int size = points.size();
                                            int newPoints = (int) (size * (percentValue / 100.0f));
                                            List<LatLng> p = points.subList(0, newPoints);
                                            blackPolyline.setPoints(p);
                                        });

                                        valueAnimator.start();

                                        LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());
                                        LatLng destination = new LatLng(Double.parseDouble(event.getPickupLocation().split(",")[0]),
                                                Double.parseDouble(event.getPickupLocation().split(",")[1]));

                                        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                                .include(origin)
                                                .include(destination)
                                                .build();

                                        //Add car icon for origin
                                        JSONObject object = jsonArray.getJSONObject(0);
                                        JSONArray legs = object.getJSONArray("legs");
                                        JSONObject legObjects = legs.getJSONObject(0);

                                        JSONObject time = legObjects.getJSONObject("duration");
                                        String duration = time.getString("text");

                                        JSONObject distanceEstimate = legObjects.getJSONObject("distance");
                                        String distance = distanceEstimate.getString("text");

                                        estimate_time_text.setText(duration);
                                        estimate_distance_text.setText(distance);

                                        mMap.addMarker(new MarkerOptions()
                                                .position(destination)
                                                .icon(BitmapDescriptorFactory.defaultMarker())
                                                .title("pickup Location"));

                                        HomeFragment.this.createGeoFirePickupLocation(event.getKey(), destination);


                                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 160));
                                        mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.getCameraPosition().zoom - 1));

                                        //Show layout
                                        chip_decline.setVisibility(View.VISIBLE);
                                        layout_accept.setVisibility(View.VISIBLE);

                                        //Counting
                                        countDownEvent = Observable.interval(50, TimeUnit.MILLISECONDS)
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .doOnNext(x -> {
                                                    circularProgressBar.setProgress(circularProgressBar.getProgress() + 1f);
                                                })
                                                .takeUntil(aLong -> aLong == 100)//10 sec
                                                .doOnComplete(() -> {
                                                    HomeFragment.this.createTripPlan(event, duration, distance);
                                                }).subscribe();


                                    } catch (Exception e) {
                                        // Snackbar.make(getView(),e.getMessage(),Snackbar.LENGTH_SHORT).show();
                                        Toast.makeText(HomeFragment.this.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                    );

                });
    }

    private void createGeoFirePickupLocation(String key, LatLng destination) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(Common.TRIP_PICKUP_REF);
        pickupGeoFire = new GeoFire(ref);
        pickupGeoFire.setLocation(key, new GeoLocation(destination.latitude, destination.longitude),
                (key1, error) -> {
                    if (error != null)
                        Snackbar.make(root_layout,error.getMessage(),Snackbar.LENGTH_LONG).show();
                    else
                        Log.d("Aniket Hyoju", key1 +"Was created success on geo fire");
                });

    }

    private void createTripPlan(DriverRequestReceived event, String duration, String distance) {
        setProcessLayout(true);
        //Sync server and device
        FirebaseDatabase.getInstance()
                .getReference(".info/serverTimeoffset")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long timeoffset = dataSnapshot.getValue(Long.class);
                        long estimateTimeInMs = System.currentTimeMillis()+timeoffset;
                        String timeText = new SimpleDateFormat("dd/MM/yyyy HH:mm aa")
                                .format(estimateTimeInMs);

                        FirebaseDatabase.getInstance()
                                .getReference(Common.PATIENT_INFO)
                                .child(event.getKey())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            PatientModel patientModel = dataSnapshot.getValue(PatientModel.class);

                                            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                                    && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                                Snackbar.make(mapFragment.getView(),getContext().getString(R.string.permission_require),
                                                        Snackbar.LENGTH_LONG).show();

                                            }
                                            fusedLocationProviderClient.getLastLocation()
                                                    .addOnFailureListener(e -> Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show())
                                                    .addOnSuccessListener(location -> {

                                                        //Trip
                                                        TripPlanModel tripPlanModel = new TripPlanModel();
                                                        tripPlanModel.setDriver(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                        tripPlanModel.setPatient(event.getKey());

                                                        //Time text
                                                        tripPlanModel.setTimeText(timeText);
                                                        //Value and fee
                                                        tripPlanModel.setDistanceValue(event.getDistanceValue());
                                                        tripPlanModel.setDurationValue(event.getDurationValue());
                                                        tripPlanModel.setTotalFare(event.getTotalFare());

                                                        tripPlanModel.setDriverInfoModel(Common.currentUser);
                                                        tripPlanModel.setPatientModel(patientModel);
                                                        tripPlanModel.setOrigin(event.getPickupLocation());
                                                        tripPlanModel.setOriginString(event.getPickupLocationString());
                                                        tripPlanModel.setDestination(event.getDestinationLocation());
                                                        tripPlanModel.setDestinationString(event.getDestinationLocationString());
                                                        tripPlanModel.setDistancePickup(distance);
                                                        tripPlanModel.setDurationPickup(duration);
                                                        tripPlanModel.setCurrentLat(location.getLatitude());
                                                        tripPlanModel.setCurrentLng(location.getLongitude());

                                                        tripNumberId = Common.createUniqueTripIdNumber(timeoffset);

                                                        FirebaseDatabase.getInstance()
                                                                .getReference(Common.Trip)
                                                                .child(tripNumberId)
                                                                .setValue(tripPlanModel)
                                                                .addOnFailureListener(e -> {
                                                                    Snackbar.make(mapFragment.getView(),e.getMessage(),Snackbar.LENGTH_SHORT).show();
                                                                }).addOnSuccessListener(aVoid -> {
                                                                    txt_Patient_name.setText(patientModel.getFirstName());
                                                                    txt_start_ride_estimate_time.setText(duration);
                                                                    txt_start_ride_estimate_distance.setText(distance);

                                                                    setOfflineModeForDriver(event,duration,distance);

                                                                });


                                                    });

                                        }
                                        else
                                            Snackbar.make(mapFragment.getView(),getContext().getString(R.string.patient_not_found)+" "+event.getKey(),Snackbar.LENGTH_SHORT).show();

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Snackbar.make(mapFragment.getView(),databaseError.getMessage(),Snackbar.LENGTH_SHORT).show();

                                    }
                                });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Snackbar.make(mapFragment.getView(),databaseError.getMessage(),Snackbar.LENGTH_SHORT).show();

                    }
                });

    }

    private void setOfflineModeForDriver(DriverRequestReceived event, String duration, String distance) {

        UserUtils.sendAcceptRequestToPatient(mapFragment.getView(),getContext(),event.getKey(),tripNumberId);

        //Offline Mode
        if (currentUserRef != null)
            currentUserRef.removeValue();

        setProcessLayout(false);
        layout_accept.setVisibility(View.GONE);
        layout_start_Ride.setVisibility(View.VISIBLE);

        isTripStart=false;
    }

    private void setProcessLayout(boolean isProcess) {
        int color = -1;
        if (isProcess) {
            color = ContextCompat.getColor(getContext(), R.color.dark_gray);
            circularProgressBar.setIndeterminateMode(true);
            rating_driver.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_star_24_dark_gray, 0);

        }
             else {
            color = ContextCompat.getColor(getContext(), android.R.color.white);
            circularProgressBar.setIndeterminateMode(false);
            circularProgressBar.setProgress(0);
            rating_driver.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_star_24, 0);

        }
            estimate_time_text.setTextColor(color);
            estimate_distance_text.setTextColor(color);
            ImageViewCompat.setImageTintList(round_img, ColorStateList.valueOf(color));
            rating_driver.setTextColor(color);
            type_ambulance.setTextColor(color);
        }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onNotifyToPatient(NotifyToPatientEvent event)
    {
        layout_notify_patient.setVisibility(View.VISIBLE);
        progress_notify.setMax(Common.WAIT_TIME_IN_MIN * 60);
        waiting_timer = new CountDownTimer(Common.WAIT_TIME_IN_MIN * 60*1000,1000) {
            @Override
            public void onTick(long l) {
                progress_notify.setProgress(progress_notify.getProgress()+1);
                notify_patient_txt.setText(String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(l) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(l)),
                        TimeUnit.MILLISECONDS.toSeconds(l) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(l))));

            }

            @Override
            public void onFinish() {
                Snackbar.make(root_layout,getString(R.string.time_taken_1),Snackbar.LENGTH_LONG).show();

            }
        };
    }
}
