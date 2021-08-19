package com.hyozu.example.ems.Services;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.hyozu.example.ems.Common;
import com.hyozu.example.ems.EventBus.DriverRequestReceived;
import com.hyozu.example.ems.Utils.UserUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        if (FirebaseAuth.getInstance().getCurrentUser() !=null)
            UserUtils.updateToken(this,s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String,String> dataRecv = remoteMessage.getData();
        if (dataRecv !=null)
        {
            if (dataRecv.get(Common.NOTI_TITLE).equals(Common.REQUEST_DRIVER_TITLE))
            {
                DriverRequestReceived driverRequestReceived = new DriverRequestReceived();
                driverRequestReceived.setKey(dataRecv.get(Common.PATIENT_KEY));
                driverRequestReceived.setPickupLocation(dataRecv.get(Common.PATIENT_PICKUP_LOCATION));
                driverRequestReceived.setPickupLocationString(dataRecv.get(Common.PATIENT_PICKUP_LOCATION_STRING));
                driverRequestReceived.setDestinationLocation(dataRecv.get(Common.PATIENT_DESTINATION));
                driverRequestReceived.setDestinationLocationString(dataRecv.get(Common.PATIENT_DESTINATION_STRING));

                //Value and fare
                driverRequestReceived.setDistanceValue(Integer.parseInt(dataRecv.get(Common.PATIENT_DISTANCE_VALUE)));
                driverRequestReceived.setDurationValue(Integer.parseInt(dataRecv.get(Common.PATIENT_TIME_VALUE)));
                driverRequestReceived.setTotalFare(Double.parseDouble(dataRecv.get(Common.PATIENT_TOTAL_FARE)));

                EventBus.getDefault().postSticky(driverRequestReceived);

            }
            else {
                Common.showNotification(this, new Random().nextInt(),
                        dataRecv.get(Common.NOTI_TITLE),
                        dataRecv.get(Common.NOTI_CONTENT),
                        null);
            }
        }
    }
}
