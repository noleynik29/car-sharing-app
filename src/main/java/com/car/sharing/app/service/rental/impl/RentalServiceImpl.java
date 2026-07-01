package com.car.sharing.app.service.rental.impl;

import com.car.sharing.app.dto.rental.CreateRentalRequest;
import com.car.sharing.app.dto.rental.RentalResponse;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.entity.Payment;
import com.car.sharing.app.entity.Rental;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.exception.EntityNotFoundException;
import com.car.sharing.app.exception.car.CarAlreadyReturnedException;
import com.car.sharing.app.exception.car.CarNotAvailableException;
import com.car.sharing.app.mapper.RentalMapper;
import com.car.sharing.app.repository.car.CarRepository;
import com.car.sharing.app.repository.payment.PaymentRepository;
import com.car.sharing.app.repository.rental.RentalRepository;
import com.car.sharing.app.service.notification.NotificationService;
import com.car.sharing.app.service.rental.RentalService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final CarRepository carRepository;
    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final NotificationService notificationService;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public RentalResponse save(User user, CreateRentalRequest request) {
        Rental rental = rentalMapper.toModel(request);

        Car car = carRepository.findById(request.carId()).orElseThrow(
                () -> new EntityNotFoundException("Car by id: "
                        + request.carId() + " not found")
        );

        validateRentalCreation(user, car);

        rental.setCar(car);
        rental.setUser(user);
        rental.setRentalDate(LocalDate.now());

        car.setInventory(car.getInventory() - 1);

        notificationService.sendRentalMessage(rental);
        return rentalMapper.toDto(rentalRepository.save(rental));
    }

    @Override
    public Page<RentalResponse> getRentals(
            Long userId,
            Boolean isActive,
            User user,
            Pageable pageable) {
        Long targetUserId = user.getRole() == User.Role.MANAGER
                ? userId
                : user.getId();

        if (isActive == null) {
            return rentalRepository.findByUserId(targetUserId, pageable)
                    .map(rentalMapper::toDto);
        }

        return isActive
                ? rentalRepository.findByUserIdAndActualReturnDateIsNull(targetUserId, pageable)
                  .map(rentalMapper::toDto)
                : rentalRepository.findByUserIdAndActualReturnDateIsNotNull(targetUserId, pageable)
                  .map(rentalMapper::toDto);
    }

    @Override
    public RentalResponse getRentalById(User user, Long id) {
        return rentalMapper.toDto(getRentalByUserRoleAndId(user, id));
    }

    @Override
    @Transactional
    public RentalResponse setActualReturnDate(User user, Long id) {
        Rental rental = getRentalByUserRoleAndId(user, id);

        if (rental.getActualReturnDate() != null) {
            throw new CarAlreadyReturnedException("Car by id " + id + " is already returned!");
        }
        rental.setActualReturnDate(LocalDate.now());

        Car car = rental.getCar();
        car.setInventory(car.getInventory() + 1);
        return rentalMapper.toDto(rental);
    }

    @Scheduled(cron = "0 0 10 * * *")
    public void checkOverdueRentals() {
        List<Rental> overdueRentals = rentalRepository
                .findByReturnDateBeforeAndActualReturnDateIsNull(LocalDate.now());

        for (Rental rental : overdueRentals) {
            notificationService.sendOverdueMessage(rental);
        }
    }

    private void validateRentalCreation(User user, Car car) {
        if (car.getInventory() < 1) {
            throw new CarNotAvailableException("Car with id: "
                    + car.getId() + " is out of stock");
        }

        if (paymentRepository.existsByRentalUserAndStatus(
                user, Payment.Status.PENDING)
        ) {
            throw new CarNotAvailableException("You already have a payment for another rental");
        }
    }

    private Rental getRentalByUserRoleAndId(User user, Long id) {
        return user.getRole() == User.Role.MANAGER
                ? rentalRepository.findById(id)
                  .orElseThrow(() -> new EntityNotFoundException("Rental by id: "
                                                                 + id + " not found!")
                  )
                : rentalRepository.findByIdAndUserId(id, user.getId())
                  .orElseThrow(() -> new EntityNotFoundException("Rental by id: "
                                                                 + id + " not found!")
                  );
    }
}
