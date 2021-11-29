package com.sososhopping.customer.purchase.service;

import com.sososhopping.customer.purchase.dto.OrderRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface PurchaseService {
    @POST("orders")
    Call<Void> requestOrders(@Header("token") String token, @Body OrderRequestDto dto);
}
