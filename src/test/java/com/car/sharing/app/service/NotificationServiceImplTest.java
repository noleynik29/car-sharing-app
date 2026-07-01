package com.car.sharing.app.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.service.notification.impl.NotificationServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private TelegramClient telegramClient;

    private NotificationServiceImpl notificationService;

    private Rental rental;
    private Payment payment;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl("fake-token");
        ReflectionTestUtils.setField(notificationService, "telegramClient", telegramClient);
        ReflectionTestUtils.setField(notificationService, "userId", "123456789");

        Car car = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Camry")
                .type(Car.Type.SEDAN)
                .inventory(5)
                .dailyFee(BigDecimal.valueOf(45.50))
                .build();

        User user = User.builder()
                .id(1L)
                .email("customer@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.CUSTOMER)
                .build();

        rental = Rental.builder()
                .id(1L)
                .rentalDate(LocalDate.of(2026, 6, 1))
                .returnDate(LocalDate.of(2026, 6, 5))
                .car(car)
                .user(user)
                .build();

        payment = Payment.builder()
                .id(1L)
                .status(Payment.Status.PENDING)
                .type(Payment.Type.PAYMENT)
                .rental(rental)
                .sessionUrl("https://checkout.stripe.com/session/test")
                .sessionId("cs_test_123")
                .amountToPay(BigDecimal.valueOf(182.00))
                .build();
    }

    @Test
    @DisplayName("Should send rental message with user email")
    void sendRentalMessage_ContainsUserEmail() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendRentalMessage(rental);

        verify(telegramClient).execute(captor.capture());
        assertTrue(captor.getValue().getText().contains("customer@example.com"));
    }

    @Test
    @DisplayName("Should send rental message with car brand and model")
    void sendRentalMessage_ContainsCarInfo() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendRentalMessage(rental);

        verify(telegramClient).execute(captor.capture());
        String text = captor.getValue().getText();
        assertTrue(text.contains("Toyota"));
        assertTrue(text.contains("Camry"));
    }

    @Test
    @DisplayName("Should send rental message with rental period dates")
    void sendRentalMessage_ContainsDates() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendRentalMessage(rental);

        verify(telegramClient).execute(captor.capture());
        String text = captor.getValue().getText();
        assertTrue(text.contains("2026-06-01"));
        assertTrue(text.contains("2026-06-05"));
    }

    @Test
    @DisplayName("Should send rental message to configured user id")
    void sendRentalMessage_SentToConfiguredUserId() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendRentalMessage(rental);

        verify(telegramClient).execute(captor.capture());
        assertTrue(captor.getValue().getChatId().equals("123456789"));
    }

    @Test
    @DisplayName("Should send overdue message with user and car info")
    void sendOverdueMessage_ContainsUserAndCarInfo() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendOverdueMessage(rental);

        verify(telegramClient).execute(captor.capture());
        String text = captor.getValue().getText();
        assertTrue(text.contains("customer@example.com"));
        assertTrue(text.contains("Toyota"));
        assertTrue(text.contains("Camry"));
    }

    @Test
    @DisplayName("Should send overdue message with return date")
    void sendOverdueMessage_ContainsReturnDate() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendOverdueMessage(rental);

        verify(telegramClient).execute(captor.capture());
        assertTrue(captor.getValue().getText().contains("2026-06-05"));
    }

    @Test
    @DisplayName("Should send payment succeeded message with user and car info")
    void sendPaymentSucceededMessage_ContainsUserAndCarInfo() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendPaymentSucceededMessage(payment);

        verify(telegramClient).execute(captor.capture());
        String text = captor.getValue().getText();
        assertTrue(text.contains("customer@example.com"));
        assertTrue(text.contains("Toyota"));
        assertTrue(text.contains("Camry"));
    }

    @Test
    @DisplayName("Should send payment succeeded message without inline keyboard")
    void sendPaymentSucceededMessage_NoKeyboard() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendPaymentSucceededMessage(payment);

        verify(telegramClient).execute(captor.capture());
        assertTrue(captor.getValue().getReplyMarkup() == null);
    }

    @Test
    @DisplayName("Should send create payment message with amount")
    void sendCreatePaymentMessage_ContainsAmount() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendCreatePaymentMessage(payment);

        verify(telegramClient).execute(captor.capture());
        assertTrue(captor.getValue().getText().contains("182.00"));
    }

    @Test
    @DisplayName("Should send create payment message with inline pay button")
    void sendCreatePaymentMessage_HasPayButton() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendCreatePaymentMessage(payment);

        verify(telegramClient).execute(captor.capture());
        assertTrue(captor.getValue().getReplyMarkup() != null);
    }

    @Test
    @DisplayName("Should send create payment message with session URL in pay button")
    void sendCreatePaymentMessage_PayButtonContainsSessionUrl() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendCreatePaymentMessage(payment);

        verify(telegramClient).execute(captor.capture());
        var markup = (org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup)
                captor.getValue().getReplyMarkup();
        String buttonUrl = markup.getKeyboard().get(0).get(0).getUrl();
        assertTrue(buttonUrl.contains("checkout.stripe.com"));
    }

    @Test
    @DisplayName("Should send payment cancelled message with session id")
    void sendPaymentCancelledMessage_ContainsSessionId() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendPaymentCancelledMessage(payment);

        verify(telegramClient).execute(captor.capture());
        assertTrue(captor.getValue().getText().contains("cs_test_123"));
    }

    @Test
    @DisplayName("Should send payment cancelled message with return-to-payment button")
    void sendPaymentCancelledMessage_HasReturnButton() throws Exception {
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

        notificationService.sendPaymentCancelledMessage(payment);

        verify(telegramClient).execute(captor.capture());
        assertTrue(captor.getValue().getReplyMarkup() != null);
    }

    @Test
    @DisplayName("Should not propagate exception when Telegram client throws")
    void send_TelegramClientThrows_DoesNotPropagate() throws Exception {
        doThrow(new RuntimeException("Telegram is down"))
                .when(telegramClient).execute(any(SendMessage.class));

        assertDoesNotThrow(() -> notificationService.sendRentalMessage(rental));
    }
}
