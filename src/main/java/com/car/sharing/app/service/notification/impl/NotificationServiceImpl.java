package com.car.sharing.app.service.notification.impl;

import static java.lang.String.format;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.car.sharing.app.dto.notification.Context;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final TelegramClient telegramClient;

    @Value("${telegram.bot.api}")
    private String token;

    @Value("${telegram.user.id}")
    private String userId;

    public NotificationServiceImpl(
            @Value("${telegram.bot.api}") String token
    ) {
        this.telegramClient = new OkHttpTelegramClient(token);
    }

    @Override
    public void sendRentalMessage(Rental rental) {
        Car car = rental.getCar();
        User user = rental.getUser();

        String msg = format("""
                        ──────────────
                        🚗 Rental Created
                        ──────────────
                        👤 User: %s
                        🚙 Car: %s %s
                        📅 Period: %s → %s
                        """,
                user.getEmail(),
                car.getBrand(),
                car.getModel(),
                rental.getRentalDate(),
                rental.getReturnDate()
        );

        send(userId, msg, null);
    }

    @Override
    public void sendOverdueMessage(Rental rental) {
        Car car = rental.getCar();
        User user = rental.getUser();

        long overdueDays = ChronoUnit.DAYS.between(
                rental.getReturnDate(),
                LocalDate.now()
        );

        String msg = format("""
                        ⚠️ Overdue Rental!
                        
                        User: %s
                        Car: %s %s
                        
                        Return date: %s
                        Overdue: %d days
                        
                        Please return the car ASAP 🚗""",
                user.getEmail(),
                car.getBrand(),
                car.getModel(),
                rental.getReturnDate(),
                overdueDays
        );

        send(userId, msg, null);
    }

    @Override
    public void sendPaymentSucceededMessage(Payment payment) {
        Context context = ctx(payment);

        String msg = format("""
                        ───────────────
                        💵 Payment Succeeded!
                        ───────────────
                        👤 User: %s
                        🚗 Car: %s %s
                        📅 Period: %s → %s
                        
                        Enjoy your car and don't forget to return in time!
                        """,
                context.user().getEmail(),
                context.car().getBrand(),
                context.car().getModel(),
                context.rental().getRentalDate(),
                context.rental().getReturnDate()
        );

        send(userId, msg, null);
    }

    @Override
    public void sendCreatePaymentMessage(Payment payment) {
        Context context = ctx(payment);

        String msg = format("""
                        ───────────────
                        💵 Payment Created
                        ───────────────
                        👤 User: %s
                        🚗 Car: %s %s
                        📅 Period: %s → %s
                        💰 Amount: %s USD
                        
                        Click below to complete payment 👇
                        """,
                context.user().getEmail(),
                context.car().getBrand(),
                context.car().getModel(),
                context.rental().getRentalDate(),
                context.rental().getReturnDate(),
                payment.getAmountToPay()
        );

        send(userId, msg, payButton(payment.getSessionUrl(), "💳 Pay now"));
    }

    @Override
    public void sendPaymentCancelledMessage(Payment payment) {
        Context context = ctx(payment);

        String msg = format("""
                        ───────────────
                        ❌ Payment Cancelled
                        ───────────────
                        👤 User: %s
                        🚗 Car: %s %s
                        💵 Payment Id: %s
                        
                        You can click below to return to payment 👇
                        """,
                context.user().getEmail(),
                context.car().getBrand(),
                context.car().getModel(),
                payment.getSessionId()
        );

        send(userId, msg, payButton(payment.getSessionUrl(), "💳 Return to payment"));
    }

    @Override
    public void send(String chatId, String msg, InlineKeyboardMarkup markup) {
        try {
            var builder = SendMessage.builder()
                    .chatId(chatId)
                    .text(msg);

            if (markup != null) {
                builder.replyMarkup(markup);
            }

            telegramClient.execute(builder.build());
        } catch (Exception e) {
            System.out.println("Telegram error: " + e.getMessage());
        }
    }

    private InlineKeyboardMarkup payButton(String url, String text) {
        return new InlineKeyboardMarkup(
                List.of(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(text)
                                .url(url)
                                .build()
                ))
        );
    }

    private Context ctx(Payment payment) {
        Rental r = payment.getRental();
        return new Context(r, r.getCar(), r.getUser());
    }
}
