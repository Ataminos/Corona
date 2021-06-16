package com.amine.corona;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

public class register extends AppCompatActivity {

    private TextInputLayout email, password, phone;
    private RelativeLayout progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_register);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        phone = findViewById(R.id.phone);
        progressBar = (RelativeLayout)findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
    }

    private Boolean validateEmail() {
        String email_field = email.getEditText().getText().toString();
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email_field);
        if (email_field.isEmpty()) {
            email.setError("الحقل مطلوب");
            return false;
        }
        else if(!m.matches()) {
            email.setError("البريد الإلكتروني غير صالح");
            return false;
        }else {
            email.setError(null);
            email.setErrorEnabled(false);
            return true;
        }
    }

    private Boolean validatePhone() {
        String phone_field = phone.getEditText().getText().toString();
        if (phone_field.isEmpty()) {
            phone.setError("الحقل مطلوب");
            return false;
        } else if(phone_field.length() < 8) {
            phone.setError("اسم المستخدم يجب أن يتخطى 8 أحرف");
            return false;
        }
         else {
            phone.setError(null);
            phone.setErrorEnabled(false);
            return true;
        }
    }

    private Boolean validatePassword() {
        String password_field = password.getEditText().getText().toString();
        if (password_field.isEmpty()) {
            password.setError("الحقل مطلوب");
            return false;
        } else if(password_field.length() < 8) {
            password.setError("كلمة المرور أقل من 8 أحرف");
            return false;
        } else {
            password.setError(null);
            password.setErrorEnabled(false);
            return true;
        }
    }

    public void register(View view) {
        String email_field = email.getEditText().getText().toString();
        String phone_field = phone.getEditText().getText().toString();
        String password_field = password.getEditText().getText().toString();

        if(validateEmail() && validatePhone() && validatePassword()) {
            progressBar.setVisibility(View.VISIBLE);
                mAuth.createUserWithEmailAndPassword(email_field, password_field)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser current_user = mAuth.getCurrentUser();
                                    User user = new User(email_field, phone_field, password_field, false,
                                            10, true, false, "disconnected");

                                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                                    DatabaseReference reference = database.getReference("users");
                                    reference.child(current_user.getUid()).setValue(user);
                                    Toast.makeText(view.getContext(), "تم التسجيل بنجاح", Toast.LENGTH_SHORT)
                                            .show();
                                    login(view);

                                }
                            }
                        });
        }
    }

    public void login(View view) {
        Intent intent = new Intent(this, login.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        return;
    }

}