package com.hyozu.example.ems.Remote;

import com.hyozu.example.ems.Model.FCMResponse;
import com.hyozu.example.ems.Model.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "content-Type:application/json",
            "Authorization:key=AAAAzIoGxww:APA91bGNtgQU19as81qO6JXbAKexmfTxBaFzr0FCenMz2ulPcuuUX9OeCOWUSpMjDs2MARWERL4QaEhJM-yQaQj4vtcvlrr7KQgqNo-driUYxWecC_XSoP1FXDiymLIxLIGo-_hPNCSJ\t\n"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
