package com.car.sharing.app.util;

import com.car.sharing.app.dto.car.CreateCarRequest;
import com.car.sharing.app.dto.car.UpdateCarInventoryRequest;
import com.car.sharing.app.dto.car.UpdateCarRequest;
import com.car.sharing.app.dto.payment.CreatePaymentRequest;
import com.car.sharing.app.dto.rental.CreateRentalRequest;
import com.car.sharing.app.dto.user.auth.UserLoginRequest;
import com.car.sharing.app.dto.user.auth.UserRegistrationRequest;
import com.car.sharing.app.dto.user.profile.UpdateUserRoleRequest;
import com.car.sharing.app.dto.user.profile.UserInfoUpdateRequest;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class TestUtil {

    private TestUtil() {
    }

    public static Car createCar() {
        return Car.builder()
                .model("Camry")
                .brand("Toyota")
                .type(Car.Type.SEDAN)
                .inventory(5)
                .dailyFee(BigDecimal.valueOf(45.50))
                .isDeleted(false)
                .build();
    }

    public static User createUser() {
        return User.builder()
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("encoded-password")
                .role(User.Role.CUSTOMER)
                .isDeleted(false)
                .build();
    }

    public static Rental createRental(Car car, User user) {
        return Rental.builder()
                .rentalDate(LocalDate.now())
                .returnDate(LocalDate.now().plusDays(3))
                .car(car)
                .user(user)
                .build();
    }

    public static Rental createRental(Car car, User user, LocalDate rentalDate,
                                      LocalDate returnDate, LocalDate actualReturnDate) {
        return Rental.builder()
                .rentalDate(rentalDate)
                .returnDate(returnDate)
                .actualReturnDate(actualReturnDate)
                .car(car)
                .user(user)
                .build();
    }

    public static Payment createPayment(Rental rental, Payment.Status status) {
        return Payment.builder()
                .status(status)
                .type(Payment.Type.PAYMENT)
                .rental(rental)
                .sessionUrl("https://checkout.stripe.com/session/test")
                .sessionId("cs_test_" + System.nanoTime())
                .amountToPay(BigDecimal.valueOf(135.00))
                .build();
    }

    public static UserRegistrationRequest createUserRegistrationRequest() {
        return new UserRegistrationRequest(
                "jane.doe@example.com",
                "Jane",
                "Doe",
                "password123",
                "password123"
        );
    }

    public static UserLoginRequest createUserLoginRequest() {
        return new UserLoginRequest("john.doe@example.com", "password123");
    }

    public static CreateCarRequest createCarRequest() {
        return new CreateCarRequest("Corolla", "Toyota", Car.Type.SEDAN, 4, BigDecimal.valueOf(38.00));
    }

    public static UpdateCarRequest updateCarRequest() {
        return new UpdateCarRequest("Corolla Hybrid", "Toyota", Car.Type.SEDAN, BigDecimal.valueOf(42.00));
    }

    public static UpdateCarInventoryRequest updateCarInventoryRequest() {
        return new UpdateCarInventoryRequest(10);
    }

    public static CreateRentalRequest createRentalRequest(Long carId) {
        return new CreateRentalRequest(carId, LocalDate.now().plusDays(5));
    }

    public static UpdateUserRoleRequest updateUserRoleRequest() {
        return new UpdateUserRoleRequest(User.Role.MANAGER);
    }

    public static UserInfoUpdateRequest userInfoUpdateRequest() {
        return new UserInfoUpdateRequest("UpdatedFirst", "UpdatedLast");
    }

    public static CreatePaymentRequest createPaymentRequest(Long rentalId) {
        return new CreatePaymentRequest(rentalId, Payment.Type.PAYMENT);
    }
}
