package com.car.sharing.app.mapper;

import com.car.sharing.app.config.MapperConfig;
import com.car.sharing.app.dto.payment.CreatePaymentRequest;
import com.car.sharing.app.dto.payment.PaymentResponse;
import com.car.sharing.app.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface PaymentMapper {

    @Mapping(source = "rental.id", target = "rentalId")
    PaymentResponse toDto(Payment payment);

    @Mapping(source = "paymentType", target = "type")
    Payment toModel(CreatePaymentRequest request);
}
