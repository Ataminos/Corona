package com.amine.corona;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class stats extends Fragment {
    private View view;
    private TextView users_count, users_count_malade,
                    itinerary_count, itinerary_count_today;
    private Map mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = (Map) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_stats, container, false);
        users_count = (TextView)view.findViewById(R.id.users_count);
        users_count_malade = (TextView)view.findViewById(R.id.users_count_malade);
        itinerary_count = (TextView)view.findViewById(R.id.itinerary_count);
        loadData();
        return view;
    }

    public float calcDistance(List<Location> positions) {
        Location location = new Location("");
        float distance = 0f;
        for(int i=0;i<positions.size()-1;i++) {
            location = positions.get(i);
            distance += location.distanceTo(positions.get(i+1));
        }
        return distance / 1000;
    }

    private void loadData() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                int count = (int)snapshot.getChildrenCount();
                users_count.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        Query query = reference.orderByChild("users");
        query.addValueEventListener(new ValueEventListener() {
                                        @Override
           public void onDataChange(DataSnapshot snapshot) {
               if (snapshot.exists()) {
                   int malade_people = 0;
                   for (DataSnapshot user : snapshot.getChildren()) {
                       boolean is_malade = user.child("isChecked").getValue(Boolean.class);
                       if(is_malade)
                           malade_people++;
                   }
                   users_count_malade.setText(String.valueOf(malade_people));
               }
           }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    float distance = 0f;
                    for (DataSnapshot user : snapshot.getChildren()) {
                        if (user.hasChild("itinerary")) {
                            if (user.child("itinerary").getChildrenCount() > 0) {

                                Iterable<DataSnapshot> Children = user.child("itinerary").getChildren();
                                for (DataSnapshot itinerary : Children) {
                                    List<Location> positions = new ArrayList<Location>();
                                    if(itinerary.getChildrenCount() > 2) {
                                        for (DataSnapshot singleItinerary : itinerary.getChildren()) {

                                            if (!singleItinerary.getKey().equals("time")) {
                                                Double Lat = singleItinerary.child("Lat").getValue(Double.class);
                                                Double Lon = singleItinerary.child("Lon").getValue(Double.class);
                                                Location location = new Location("");
                                                location.setLatitude(Lat);
                                                location.setLongitude(Lon);
                                                positions.add(location);
                                            }

                                        }
                                        distance += calcDistance(positions);
                                    }
                                }
                            }
                            itinerary_count.setText(String.valueOf(distance + " Km"));
                        }
                    }
                }
            }

                    @Override
                    public void onCancelled (@NonNull @NotNull DatabaseError error){

                    }
        });
    }
}