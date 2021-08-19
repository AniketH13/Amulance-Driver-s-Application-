package com.hyozu.example.ems;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;


import com.hyozu.example.ems.Model.DriverInfoModel;
import com.hyozu.example.ems.Utils.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;

public class SplashActivity extends AppCompatActivity {
    private final static  int LOGIN_REQUEST_CODE= 7172;
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth Auth;
    private FirebaseAuth.AuthStateListener listener;

    @BindView(R.id.progress_circle)
    ProgressBar progress_circle;

    FirebaseDatabase database;
    DatabaseReference driverInfoRef;

    @Override
    protected void onStart() {
        super.onStart();
       delaySplashScreen();
    }

    @Override
    protected void onStop() {

        if (Auth !=null && listener !=null)
            Auth.removeAuthStateListener(listener);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        init();
    }

    private void init() {
        ButterKnife.bind(this);

        database = FirebaseDatabase.getInstance();
        driverInfoRef = database.getReference(Common.DRIVER_INFO_REFERENCE);
        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                 new AuthUI.IdpConfig.GoogleBuilder().build());
        Auth = FirebaseAuth.getInstance();
        listener= myFirebaseAuth -> {
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            if (user !=null)
            {
                //Updating Token
                FirebaseInstanceId.getInstance()
                        .getInstanceId()
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(SplashActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }).addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        Log.d("TOKEN",instanceIdResult.getToken());
                        UserUtils.updateToken(SplashActivity.this,instanceIdResult.getToken());
                    }
                });
                CheckUserFromFirebase();
            }
            else
                showLogininLayout();
        };
    }

    private void CheckUserFromFirebase() {
        driverInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists())
                            {
                                //Toast.makeText(SplashActivity.this, "User already register", Toast.LENGTH_SHORT).show();
                            DriverInfoModel driverInfoModel = dataSnapshot.getValue(DriverInfoModel.class);
                            goToHomeActivity(driverInfoModel);
                            }
                            else
                            {
                                showRegisterLayout();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseerror) {
                            Toast.makeText(SplashActivity.this, ""+databaseerror.getMessage()
                                    , Toast.LENGTH_SHORT).show();
                        }
                    });
        ;
    }

    private void goToHomeActivity(DriverInfoModel driverInfoModel) {
        Common.currentUser = driverInfoModel; // Init value
        startActivity(new Intent(SplashActivity.this,DriverMapActivity.class));
        finish();
    }

    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.DialogTheme);
        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null);
        TextInputEditText F_name = (TextInputEditText)itemView.findViewById(R.id.Fname);
        TextInputEditText L_name = (TextInputEditText)itemView.findViewById(R.id.Lname);
        TextInputEditText Phone_number = (TextInputEditText)itemView.findViewById(R.id.Phn_number);

        Button btn_continue = (Button)itemView.findViewById(R.id.register_btn);

        //setting data
        if (Auth.getInstance().getCurrentUser().getPhoneNumber() !=null &&
        !TextUtils.isEmpty(Auth.getInstance().getCurrentUser().getPhoneNumber()))
            Phone_number.setText(Auth.getInstance().getCurrentUser().getPhoneNumber());

        //setting view
        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();

        btn_continue.setOnClickListener(v -> {
            if (TextUtils.isEmpty(F_name.getText().toString()))
            {
                Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show();
                return;
            }
            else
                if (TextUtils.isEmpty(L_name.getText().toString()))
            {
                Toast.makeText(this, "Please enter your Last name", Toast.LENGTH_SHORT).show();
                return;
            }
                else

                    if (TextUtils.isEmpty(Phone_number.getText().toString()))
                    {
                        Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                else
                    {
                        DriverInfoModel model = new DriverInfoModel();
                        model.setFirstName(F_name.getText().toString());
                        model.setLastName(L_name.getText().toString());
                        model.setPhoneNumber(Phone_number.getText().toString());
                        model.setRating(0.0);

                        driverInfoRef.child(Auth.getInstance().getCurrentUser().getUid())
                                .setValue(model)
                                .addOnFailureListener(e ->
                                {
                                    dialog.dismiss();

                                    Toast.makeText(SplashActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                    )
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Registration Completed", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    goToHomeActivity(model);
                                });

                    }
        });
    }

    private void showLogininLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new  AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.But_mobile_login)
                .setGoogleButtonId(R.id.But_login_google)
                .build();
        startActivityForResult(AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAuthMethodPickerLayout(authMethodPickerLayout)
        .setIsSmartLockEnabled(false)
        .setAvailableProviders(providers)
        .build(),LOGIN_REQUEST_CODE);


    }

    private void delaySplashScreen() {
        progress_circle.setVisibility(View.VISIBLE);
       Completable.timer(5, TimeUnit.SECONDS,
        AndroidSchedulers.mainThread())
               .subscribe(new Action() {
                              @Override
                              public void run() throws Exception {
                                  Auth.addAuthStateListener(listener);
                              }
                          }
                       );

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE)
        {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK)
            {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else
            {
                Toast.makeText(this, "Sing in Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

