package com.amine.corona;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Tracking_service extends Service {
    private DatabaseReference reference;
    private Location myLocation;
    private float min_distance;
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat managerCompat;
    private static MediaPlayer mp;
    private FirebaseAuth auth;
    private String Uid;

        @Override
        public void onCreate() {
            super.onCreate();
            auth = FirebaseAuth.getInstance();
            if(auth == null) {
                stopSelf();
            }
            Uid = auth.getCurrentUser().getUid();
        }

    public Tracking_service() {
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SharedPreferences prefs = getSharedPreferences("corona_settings", Context.MODE_PRIVATE);
        min_distance = prefs.getFloat("min_distance", 500f);
        reference = FirebaseDatabase.getInstance().getReference("users");
        myLocation = new Location("");

        mp= MediaPlayer.create(getApplicationContext(), R.raw.alert);
        forground_notification();
        setup_notification();
        check_myLocation();
        check_danger_distance();
        return START_STICKY;
    }

    private void setup_notification() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("alert", "alert", NotificationManager.IMPORTANCE_HIGH);

            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(true);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }
        builder = new NotificationCompat.Builder(Tracking_service.this, "alert");
        builder.setContentTitle("تنبيه");
        builder.setContentText("يوجد مريض مصاب بكورونا بقربك أو أقل من "+min_distance+"متر.");
        builder.setSmallIcon(R.drawable.icon);
        builder.setAutoCancel(true);

        managerCompat = NotificationManagerCompat.from(Tracking_service.this);
    }

    private void forground_notification() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("working", "working", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableVibration(true);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }
        Intent intent = new Intent(this, login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 2, intent,
                PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "working")
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("كورونا")
                .setContentText("جاري البحث عن المصابين القريبين من مكان تواجدك")
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(2, notificationBuilder.build());
        startForeground(2, notificationBuilder.getNotification());

    }

    private void check_myLocation() {
        DatabaseReference myProfile = reference.child(Uid);
        myProfile.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("Lat") && dataSnapshot.hasChild("Lon")) {
                    Double Lat = dataSnapshot.child("Lat").getValue(Double.class);
                    Double Lon = dataSnapshot.child("Lon").getValue(Double.class);
                    myLocation.setLatitude(Lat);
                    myLocation.setLongitude(Lon);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void check_danger_distance() {

        Query query = reference.orderByChild("users");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot user : snapshot.getChildren()) {
                        if(user.hasChild("Lon") && user.hasChild("Lat")) {
                            User login_user = user.getValue(User.class);

                            if(!user.getKey().equals(Uid)  && login_user.isChecked) {
                                Double lon = user.child("Lon").getValue(Double.class);
                                Double lat = user.child("Lat").getValue(Double.class);

                                Location location = new Location(login_user.phone);
                                location.setLatitude(lat);
                                location.setLongitude(lon);
                                float distance = myLocation.distanceTo(location);
                                if(distance < min_distance && !mp.isPlaying()) {
                                    managerCompat.notify(1, builder.build());
                                    mp.start();
                                }

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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}