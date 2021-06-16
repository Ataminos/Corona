package com.amine.corona;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private DatabaseReference reference;
    private GoogleMap gm;
    private SupportMapFragment supportMapFragment;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private final long MIN_TIME = 5000;
    private final long MIN_DIST = 15;
    private String session_key;
    private Switch is_corona;
    private Location last_location;
    private boolean find_last_location = false;
    private SeekBar days_seekBar;
    private TextView customDays;
    private LinearLayout active_gps;
    private boolean active = true;
    private FirebaseAuth auth;
    private String Uid;


    private Map mContext;
    private View view;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = (Map) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_maps, container, false);

            ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 100);

        reference = FirebaseDatabase.getInstance().getReference("users");
        auth = FirebaseAuth.getInstance();
        Uid = auth.getCurrentUser().getUid();

        reference.child(Uid).child("status").setValue("connected");
        reference.child(Uid).child("status").onDisconnect().setValue("disconnected");

        events();
        active_gps = (LinearLayout)view.findViewById(R.id.enable_gps);

        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        session_key = reference.child(auth.getCurrentUser().getUid()).child("itinerary").push().getKey();
        active = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        active = false;
    }

    private void events() {
        customDays = (TextView)view.findViewById(R.id.customDays);
        days_seekBar = (SeekBar)view.findViewById(R.id.days_seekBar);
        days_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                reference.child(Uid).child("visible_days").setValue(progress);
                if(progress == 0)
                    customDays.setText((progress+1)+" يوم");
                else
                    customDays.setText((progress+1)+" أيام");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        is_corona = (Switch)mContext.findViewById(R.id.is_corona);
        is_corona.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                reference.child(Uid).child("isChecked").setValue(isChecked);
                if(isChecked)
                    session_key = reference.child(auth.getCurrentUser().getUid()).child("itinerary").push().getKey();
            }
        });

        reference.child(Uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                boolean checked = snapshot.child("isChecked").getValue(Boolean.class);
                int count_days = snapshot.child("visible_days").getValue(Integer.class);
                is_corona.setChecked(checked);
                days_seekBar.setProgress(count_days);
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        LinearLayout get_my_location = (LinearLayout)view.findViewById(R.id.get_my_location);
        get_my_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(find_last_location)
                    goToMyPosition();
            }
        });
    }

    private void loadOtherPlaces() {
        Query query = reference.orderByChild("users");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    gm.clear();
                    for (DataSnapshot user : snapshot.getChildren()) {


                            if (user.hasChild("Lon") && user.hasChild("Lat")) {
                                User login_user = user.getValue(User.class);
                                Double lon = user.child("Lon").getValue(Double.class);
                                Double lat = user.child("Lat").getValue(Double.class);

                                if (user.hasChild("itinerary")) {
                                    if (user.child("itinerary").getChildrenCount() > 0) {

                                        Iterable<DataSnapshot> Children = user.child("itinerary").getChildren();
                                        for (DataSnapshot itinerary : Children) {
                                            Date date = itinerary.child("time").getValue(Date.class);
                                            Calendar cal = Calendar.getInstance();
                                            cal.setTime(setTimeToMidnight(Timestamp.now().toDate()));

                                            cal.add(Calendar.DAY_OF_MONTH, -days_seekBar.getProgress());
                                            if (date.after(cal.getTime()) || date.equals(cal.getTime())) {
                                                List<LatLng> list = new ArrayList<>();

                                                for (DataSnapshot singleItinerary : itinerary.getChildren()) {
                                                    if (!singleItinerary.getKey().equals("time")) {
                                                        Double Lat = singleItinerary.child("Lat").getValue(Double.class);
                                                        Double Lon = singleItinerary.child("Lon").getValue(Double.class);
                                                        list.add(new LatLng(Lat, Lon));
                                                    }
                                                }

                                                if (!list.isEmpty()) {
                                                    PolylineOptions options = new PolylineOptions().width(8).color(Color.RED).geodesic(true);
                                                    for (int z = 0; z < list.size(); z++) {
                                                        options.add(list.get(z));
                                                    }
                                                    gm.addPolyline(options);
                                                }
                                            }
                                        }
                                    }
                                }
                                if(!user.getKey().toString().equals(Uid)) {
                                    if (login_user.isVisible && login_user.isChecked && login_user.status.equals("connected")) {
                                        String fullName = login_user.phone;
                                        if (login_user.isAnonyme)
                                            fullName = "مجهول";
                                        gm.addMarker(new MarkerOptions()
                                                .position(new LatLng(lat, lon))
                                                .title(fullName)
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_warning)));
                                        gm.addCircle(new CircleOptions()
                                                .center(new LatLng(lat, lon))
                                                .radius(300)
                                                .strokeColor(Color.RED)
                                                .fillColor(Color.argb(40, 255, 65, 65)));
                                    }
                                } else {
                                    gm.addMarker(new MarkerOptions()
                                            .position(new LatLng(lat, lon))
                                            .title("موقعي")
                                            .icon(bitmapDiscriptorFromVector(R.drawable.ic_position)));
                                }

                            }
                        }
                    }
                }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private BitmapDescriptor bitmapDiscriptorFromVector(int vectorId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(mContext, vectorId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static Date setTimeToMidnight(Date date) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime( date );
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    private void goToMyPosition() {
        LatLng myPosition = new LatLng(last_location.getLatitude(), last_location.getLongitude());
        gm.animateCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 17));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gm = googleMap;

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(active) {
                    last_location = location;
                    if (!find_last_location) {
                        goToMyPosition();
                        find_last_location = true;
                    }
                    reference.child(Uid).child("Lat").setValue(location.getLatitude());
                    reference.child(Uid).child("Lon").setValue(location.getLongitude());
                    if (is_corona.isChecked()) {
                        reference.child(Uid).child("itinerary").child(session_key).child("time").setValue(Timestamp.now().toDate());
                        reference.child(Uid).child("itinerary").child(session_key).push().setValue(new LatLon(location.getLatitude(), location.getLongitude()));
                    }
                }
            }
            @Override
            public void onProviderEnabled(@NonNull String provider) {
                active_gps.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                active_gps.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
        };

        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST
                                                                            , locationListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadOtherPlaces();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        active = false;
        reference.child(Uid).child("status").setValue("disconnected");
    }
}