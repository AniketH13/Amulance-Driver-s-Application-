package com.hyozu.example.ems;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;
import com.hyozu.example.ems.Model.DriverInfoModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Common {
    public static final String DRIVER_INFO_REFERENCE = "DriverInfo";
    public static final String DRIVER_LOCATION_REFERENCE = "DriversLocation";
    public static final String TOKEN_REFERENCE ="Token" ;
    public static final String NOTI_TITLE = "title";
    public static final String NOTI_CONTENT ="body" ;
    public static final String REQUEST_DRIVER_TITLE ="RequestAmbulance" ;
    public static final String PATIENT_PICKUP_LOCATION = "PickupLocation";
    public static final String PATIENT_KEY = "PatientKey";
    public static final String REQUEST_DRIVER_DECLINE = "Decline";
    public static final String DRIVER_KEY = "DriverKey";
    public static final String PATIENT_PICKUP_LOCATION_STRING = "PickupLocationString";
    public static final String PATIENT_DESTINATION_STRING = "DestinationLocationString";
    public static final String PATIENT_DESTINATION = "DestinationLocation";
    public static final String PATIENT_INFO = "Users";
    public static final String REQUEST_DRIVER_ACCEPT = "Accept";
    public static final String TRIP_KEY = "TripKey";
    public static final String TRIP_PICKUP_REF = "TripPickupLocation";
    public static final double MIN_RANGE_PICKUP_IN_KM = 0.5;
    public static final int WAIT_TIME_IN_MIN = 1;
    public static final String TRIP_DESTINATION_LOCATION_REF = "TripDestinationLocation";
    public static final String REQUEST_DRIVER_DECLINE_AND_REMOVE_TRIP = "DeclineAndRemoveTrip";
    public static final String PATIENT_COMPLETE_TRIP = "CompleteTripByDriver";
    public static final String PATIENT_DISTANCE_VALUE = "DistanceValue";
    public static final String PATIENT_TIME_VALUE = "TimeValue";
    public static String PATIENT_TOTAL_FARE = "TotalFare";

    public static DriverInfoModel currentUser;
    public static String Trip="Trips";

    public static String buildWelcomeMessage() {
       if (Common.currentUser !=null)
       {
           return new StringBuilder("Welcome")
                   .append(Common.currentUser.getFirstName())
                   .append(" ")
                   .append(Common.currentUser.getLastName()).toString();
       }
       else
           return "";
    }

    public static List<LatLng> decodepoly(String encoded) {
        List poly = new ArrayList();
        int index=0,len=encoded.length();
        int lat=0,lng=0;
        while(index < len)
        {
            int b,shift=0,result=0;
            do{
                b=encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift+=5;

            }while(b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1):(result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do{
                b = encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift +=5;
            }while(b >= 0x20);
            int dlng = ((result & 1)!=0 ? ~(result >> 1): (result >> 1));
            lng +=dlng;

            LatLng p = new LatLng((((double)lat / 1E5)),
                    (((double)lng/1E5)));
            poly.add(p);
        }
        return poly;
    }


    public static void showNotification(Context context, int id, String title, String body, Intent intent) {
        PendingIntent pendingIntent = null;
        if (intent !=null)
            pendingIntent = pendingIntent.getActivity(context,id,intent,pendingIntent.FLAG_UPDATE_CURRENT);
        String NOTIFICATION_CHANNEL_ID = "Aniket_Hyoju";
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Emergency Services", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Emergency Service");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_baseline_directions_car_24)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_baseline_directions_car_24));
                if (pendingIntent !=null)
                {
                    builder.setContentIntent(pendingIntent);
                }
                Notification notification = builder.build();
                notificationManager.notify(id,notification);

    }

    public static String createUniqueTripIdNumber(long timeoffset) {
        Random random = new Random();
        Long current = System.currentTimeMillis()+timeoffset;
        Long unique = current + random.nextLong();
        if (unique < 0) unique*=(-1);
        return String.valueOf(unique);
    }
}
