package com.amine.corona;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.jetbrains.annotations.NotNull;


public class Map extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public Fragment fragment;
    private Intent intent;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
              WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_map);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if(user == null) {
            startActivity(new Intent(Map.this, MainActivity.class));
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_home);

        LinearLayout openDrawer = (LinearLayout)findViewById(R.id.open_drawer);
        openDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(Gravity.LEFT);
            }
        });

        intent = new Intent(Map.this, Tracking_service.class);

        fragment = new MapsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.map_frame_layout, fragment)
                .commit();

        restartService();
    }

    public void restartService() {
        stopService(intent);
        ContextCompat.startForegroundService(this, intent);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {

        int id = item.getItemId();
        switch (id){
            case R.id.nav_home:
                fragment = new MapsFragment();
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.map_frame_layout, fragment)
                        .commit();
                restartService();
                break;
            case R.id.nav_statistics:
                fragment = new stats();
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.map_frame_layout, fragment)
                        .commit();
            break;
            case R.id.nav_settings:
                fragment = new settings();
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.map_frame_layout, fragment)
                        .commit();

                break;
            case R.id.nav_disconnect:
                stopService(intent);
                auth.signOut();
                startActivity(new Intent(Map.this, MainActivity.class));
                finish();
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return false;
    }

    @Override
    public void onBackPressed() {
        return;
    }

}