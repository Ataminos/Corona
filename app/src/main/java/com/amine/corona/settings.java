package com.amine.corona;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

public class settings extends Fragment {

    private TextInputLayout min_distance_input;
    private Switch isVisible, isAnonyme;
    private DatabaseReference reference;
    private FirebaseAuth auth;
    private Map mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = (Map) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("users");
        min_distance_input = view.findViewById(R.id.min_distance);
        isVisible = (Switch) view.findViewById(R.id.isVisible);
        isAnonyme = (Switch) view.findViewById(R.id.isAnonyme);
        loadSettings();
        Button save = (Button)view.findViewById(R.id.saveSettings);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        return view;
    }

    private void loadSettings() {
        SharedPreferences prefs = getActivity().getSharedPreferences("corona_settings", Context.MODE_PRIVATE);
        float min_distance = prefs.getFloat("min_distance", 500f);
        min_distance_input.getEditText().setText(String.valueOf(min_distance));

        reference.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    boolean Visible = snapshot.child("isVisible").getValue(Boolean.class);
                    isVisible.setChecked(Visible);
                    boolean Anonyme = snapshot.child("isAnonyme").getValue(Boolean.class);
                    isAnonyme.setChecked(Anonyme);
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    private void saveSettings() {
        SharedPreferences prefs = getActivity().getSharedPreferences("corona_settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        float min_distance = Float.parseFloat(min_distance_input.getEditText().getText().toString());
        editor.putFloat("min_distance", min_distance);
        editor.commit();

        reference.child(auth.getCurrentUser().getUid()).child("isVisible").setValue(isVisible.isChecked());
        reference.child(auth.getCurrentUser().getUid()).child("isAnonyme").setValue(isAnonyme.isChecked());

        Toast.makeText(getContext(), "تم حفظ البيانات بنجاح", Toast.LENGTH_SHORT).show();
        mContext.restartService();
        mContext.fragment = new MapsFragment();
        mContext.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.map_frame_layout, mContext.fragment)
                .commit();


    }
}