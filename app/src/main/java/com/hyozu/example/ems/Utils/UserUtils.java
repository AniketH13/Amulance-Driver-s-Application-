package com.hyozu.example.ems.Utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hyozu.example.ems.Common;
import com.hyozu.example.ems.EventBus.NotifyToPatientEvent;
import com.hyozu.example.ems.Model.FCMResponse;
import com.hyozu.example.ems.Model.FCMSendData;
import com.hyozu.example.ems.Model.TokenModel;
import com.hyozu.example.ems.R;
import com.hyozu.example.ems.Remote.IFCMService;
import com.hyozu.example.ems.Remote.RetrofitFCMClient;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class UserUtils {
    public static void updateUser (View view, Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.DRIVER_INFO_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .updateChildren(updateData)
                .addOnFailureListener(e -> Snackbar.make(view,e.getMessage(),Snackbar.LENGTH_SHORT).show())
                .addOnSuccessListener(aVoid -> Snackbar.make(view,"Update information successfully!",Snackbar.LENGTH_SHORT).show());
    }

    public static void updateToken(Context context, String token) {
        TokenModel tokenModel = new TokenModel(token);

        FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(tokenModel)
                .addOnFailureListener(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(aVoid -> {

                });
    }

    public static void sendDeclineRequest(View view, Context context, String key) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase
                .getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot databaseSnapshot) {
                        if (databaseSnapshot.exists())
                        {
                            TokenModel tokenModel = databaseSnapshot.getValue(TokenModel.class);

                            Map<String,String> notificationData = new HashMap<>();
                            notificationData.put(Common.NOTI_TITLE,Common.REQUEST_DRIVER_DECLINE);
                            notificationData.put(Common.NOTI_CONTENT, "This message represent for decline action");
                            notificationData.put(Common.DRIVER_KEY,FirebaseAuth.getInstance().getCurrentUser().getUid());


                            FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(),notificationData);

                            compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Consumer<FCMResponse>() {
                                        @Override
                                        public void accept(FCMResponse fcmResponse) throws Exception {
                                            if (fcmResponse.getSuccess() == 0) {
                                                compositeDisposable.clear();
                                                Snackbar.make(view, context.getString(R.string.decline_failed), Snackbar.LENGTH_LONG).show();
                                            } else {
                                                Snackbar.make(view, context.getString(R.string.decline_Success), Snackbar.LENGTH_LONG).show();
                                            }

                                        }
                                    }, throwable -> {
                                        compositeDisposable.clear();
                                        Snackbar.make(view,throwable.getMessage(),Snackbar.LENGTH_LONG).show();
                                    }));

                        }
                        else
                        {
                            compositeDisposable.clear();
                            Snackbar.make(view,context.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        compositeDisposable.clear();
                        Snackbar.make(view,databaseError.getMessage(),Snackbar.LENGTH_LONG).show();
                    }
                });

    }

    public static void sendAcceptRequestToPatient(View view, Context context, String key, String tripNumberId) {

        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase
                .getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot databaseSnapshot) {
                        if (databaseSnapshot.exists())
                        {
                            TokenModel tokenModel = databaseSnapshot.getValue(TokenModel.class);

                            Map<String,String> notificationData = new HashMap<>();
                            notificationData.put(Common.NOTI_TITLE,Common.REQUEST_DRIVER_ACCEPT);
                            notificationData.put(Common.NOTI_CONTENT, "This message represent for accept action");
                            notificationData.put(Common.DRIVER_KEY,FirebaseAuth.getInstance().getCurrentUser().getUid());
                            notificationData.put(Common.TRIP_KEY,tripNumberId);


                            FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(),notificationData);

                            compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(fcmResponse -> {
                                        if (fcmResponse.getSuccess() == 0)
                                        {
                                            compositeDisposable.clear();
                                            Snackbar.make(view,context.getString(R.string.accept_failed),Snackbar.LENGTH_LONG).show();
                                        }

                                    }, throwable -> {
                                        compositeDisposable.clear();
                                        Snackbar.make(view,throwable.getMessage(),Snackbar.LENGTH_LONG).show();
                                    }));

                        }
                        else
                        {
                            compositeDisposable.clear();
                            Snackbar.make(view,context.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        compositeDisposable.clear();
                        Snackbar.make(view,databaseError.getMessage(),Snackbar.LENGTH_LONG).show();
                    }
                });


    }

    public static void sendNotifyToRider(Context context, View view, String key) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        FirebaseDatabase
                .getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot databaseSnapshot) {
                        if (databaseSnapshot.exists())
                        {
                            TokenModel tokenModel = databaseSnapshot.getValue(TokenModel.class);

                            Map<String,String> notificationData = new HashMap<>();
                            notificationData.put(Common.NOTI_TITLE,context.getString(R.string.driver_arrived));
                            notificationData.put(Common.NOTI_CONTENT, context.getString(R.string.your_driver_arrived));
                            notificationData.put(Common.DRIVER_KEY,FirebaseAuth.getInstance().getCurrentUser().getUid());
                            notificationData.put(Common.PATIENT_KEY,key);


                            FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(),notificationData);

                            compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Consumer<FCMResponse>() {
                                        @Override
                                        public void accept(FCMResponse fcmResponse) throws Exception {
                                            if (fcmResponse.getSuccess() == 0) {
                                                compositeDisposable.clear();
                                                Snackbar.make(view, context.getString(R.string.accept_failed), Snackbar.LENGTH_LONG).show();
                                            } else
                                                EventBus.getDefault().postSticky(new NotifyToPatientEvent());

                                        }
                                    }, throwable -> {
                                        compositeDisposable.clear();
                                        Snackbar.make(view,throwable.getMessage(),Snackbar.LENGTH_LONG).show();
                                    }));

                        }
                        else
                        {
                            compositeDisposable.clear();
                            Snackbar.make(view,context.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        compositeDisposable.clear();
                        Snackbar.make(view,error.getMessage(),Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    public static void sendDeclineAndRemoveTripRequest(View view, Context context, String key, String tripNumberId) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        //remove trip from database
        FirebaseDatabase.getInstance()
                .getReference(Common.Trip)
                .child(tripNumberId)
                .removeValue()
                .addOnFailureListener(e -> {
                    Snackbar.make(view,e.getMessage(),Snackbar.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(aVoid -> {
                    //Delete success,send notification
                    FirebaseDatabase
                            .getInstance()
                            .getReference(Common.TOKEN_REFERENCE)
                            .child(key)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot databaseSnapshot) {
                                    if (databaseSnapshot.exists())
                                    {
                                        TokenModel tokenModel = databaseSnapshot.getValue(TokenModel.class);

                                        Map<String,String> notificationData = new HashMap<>();
                                        notificationData.put(Common.NOTI_TITLE,Common.REQUEST_DRIVER_DECLINE_AND_REMOVE_TRIP);
                                        notificationData.put(Common.NOTI_CONTENT, "This message represent for decline action");
                                        notificationData.put(Common.DRIVER_KEY,FirebaseAuth.getInstance().getCurrentUser().getUid());


                                        FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(),notificationData);

                                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                                                .subscribeOn(Schedulers.newThread())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(fcmResponse -> {
                                                    if (fcmResponse.getSuccess() == 0)
                                                    {
                                                        compositeDisposable.clear();
                                                        Snackbar.make(view,context.getString(R.string.decline_failed),Snackbar.LENGTH_LONG).show();
                                                    }
                                                    else
                                                    {
                                                        Snackbar.make(view,context.getString(R.string.decline_Success),Snackbar.LENGTH_LONG).show();
                                                    }

                                                }, throwable -> {
                                                    compositeDisposable.clear();
                                                    Snackbar.make(view,throwable.getMessage(),Snackbar.LENGTH_LONG).show();
                                                }));

                                    }
                                    else
                                    {
                                        compositeDisposable.clear();
                                        Snackbar.make(view,context.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show();
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    compositeDisposable.clear();
                                    Snackbar.make(view,databaseError.getMessage(),Snackbar.LENGTH_LONG).show();
                                }
                            });

                });

    }

    public static void sendCompleteTripTOPatient(View view, Context context, String key, String tripNumberId) {
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);


                    //update trip ,send notification
                    FirebaseDatabase
                            .getInstance()
                            .getReference(Common.TOKEN_REFERENCE)
                            .child(key)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot databaseSnapshot) {
                                    if (databaseSnapshot.exists())
                                    {
                                        TokenModel tokenModel = databaseSnapshot.getValue(TokenModel.class);

                                        Map<String,String> notificationData = new HashMap<>();
                                        notificationData.put(Common.NOTI_TITLE,Common.PATIENT_COMPLETE_TRIP);
                                        notificationData.put(Common.NOTI_CONTENT, "This message represent for trip complete action");
                                        notificationData.put(Common.TRIP_KEY, tripNumberId);


                                        FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(),notificationData);

                                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                                                .subscribeOn(Schedulers.newThread())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(new Consumer<FCMResponse>() {
                                                    @Override
                                                    public void accept(FCMResponse fcmResponse) throws Exception {
                                                        if (fcmResponse.getSuccess() == 0) {
                                                            compositeDisposable.clear();
                                                            Snackbar.make(view, context.getString(R.string.complete_trip_failed), Snackbar.LENGTH_LONG).show();
                                                        } else {
                                                            Snackbar.make(view, context.getString(R.string.complete_trip_success), Snackbar.LENGTH_LONG).show();
                                                        }

                                                    }
                                                }, throwable -> {
                                                    compositeDisposable.clear();
                                                    Snackbar.make(view,throwable.getMessage(),Snackbar.LENGTH_LONG).show();
                                                }));

                                    }
                                    else
                                    {
                                        compositeDisposable.clear();
                                        Snackbar.make(view,context.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show();
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    compositeDisposable.clear();
                                    Snackbar.make(view,databaseError.getMessage(),Snackbar.LENGTH_LONG).show();
                                }
                            });



    }
}
