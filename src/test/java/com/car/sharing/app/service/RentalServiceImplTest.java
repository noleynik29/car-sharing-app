package com.car.sharing.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.car.sharing.app.service.rental.impl.RentalServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RentalServiceImplTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private RentalMapper rentalMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private RentalServiceImpl rentalService;

    private Car car;
    private User customer;
    private User manager;
    private Rental rental;
    private RentalResponse rentalResponse;

    @BeforeEach
    void setUp() {
        car = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Camry")
                .type(Car.Type.SEDAN)
                .inventory(5)
                .dailyFee(BigDecimal.valueOf(45.50))
                .build();

        customer = User.builder()
                .id(1L)
                .email("customer@example.com")
                .role(User.Role.CUSTOMER)
                .build();

        manager = User.builder()
                .id(2L)
                .email("manager@example.com")
                .role(User.Role.MANAGER)
                .build();

        rental = Rental.builder()
                .id(1L)
                .rentalDate(LocalDate.of(2026, 6, 1))
                .returnDate(LocalDate.of(2026, 6, 5))
                .car(car)
                .user(customer)
                .build();

        rentalResponse = new RentalResponse(
                1L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5),
                null,
                null,
                1L
        );
    }

    @Test
    @DisplayName("Should save rental and return RentalResponse")
    void save_ValidRequest_ReturnsRentalResponse() {
        CreateRentalRequest request = new CreateRentalRequest(1L, LocalDate.of(2026, 6, 5));

        when(rentalMapper.toModel(request)).thenReturn(rental);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(paymentRepository.existsByRentalUserAndStatus(customer, Payment.Status.PENDING))
                .thenReturn(false);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        RentalResponse actual = rentalService.save(customer, request);

        assertNotNull(actual);
        assertEquals(rentalResponse.id(), actual.id());
        verify(rentalRepository).save(rental);
    }

    @Test
    @DisplayName("Should decrement car inventory on rental creation")
    void save_ValidRequest_DecrementsCarInventory() {
        CreateRentalRequest request = new CreateRentalRequest(1L, LocalDate.of(2026, 6, 5));
        int initialInventory = car.getInventory();

        when(rentalMapper.toModel(request)).thenReturn(rental);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(paymentRepository.existsByRentalUserAndStatus(customer, Payment.Status.PENDING))
                .thenReturn(false);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        rentalService.save(customer, request);

        assertEquals(initialInventory - 1, car.getInventory());
    }

    @Test
    @DisplayName("Should set rental date to today on creation")
    void save_ValidRequest_SetsRentalDateToToday() {
        CreateRentalRequest request = new CreateRentalRequest(1L, LocalDate.of(2026, 6, 5));
        Rental mappedRental = Rental.builder().build();

        when(rentalMapper.toModel(request)).thenReturn(mappedRental);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(paymentRepository.existsByRentalUserAndStatus(customer, Payment.Status.PENDING))
                .thenReturn(false);
        when(rentalRepository.save(mappedRental)).thenReturn(mappedRental);
        when(rentalMapper.toDto(mappedRental)).thenReturn(rentalResponse);

        rentalService.save(customer, request);

        assertEquals(LocalDate.now(), mappedRental.getRentalDate());
    }

    @Test
    @DisplayName("Should send rental notification on creation")
    void save_ValidRequest_SendsNotification() {
        CreateRentalRequest request = new CreateRentalRequest(1L, LocalDate.of(2026, 6, 5));

        when(rentalMapper.toModel(request)).thenReturn(rental);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(paymentRepository.existsByRentalUserAndStatus(customer, Payment.Status.PENDING))
                .thenReturn(false);
        when(rentalRepository.save(rental)).thenReturn(rental);
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        rentalService.save(customer, request);

        verify(notificationService).sendRentalMessage(rental);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when car not found")
    void save_CarNotFound_ThrowsEntityNotFoundException() {
        CreateRentalRequest request = new CreateRentalRequest(999L, LocalDate.of(2026, 6, 5));

        when(rentalMapper.toModel(request)).thenReturn(rental);
        when(carRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> rentalService.save(customer, request));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CarNotAvailableException when car is out of stock")
    void save_CarOutOfStock_ThrowsCarNotAvailableException() {
        car.setInventory(0);
        CreateRentalRequest request = new CreateRentalRequest(1L, LocalDate.of(2026, 6, 5));

        when(rentalMapper.toModel(request)).thenReturn(rental);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThrows(CarNotAvailableException.class, () -> rentalService.save(customer, request));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CarNotAvailableException when user has pending payment")
    void save_UserHasPendingPayment_ThrowsCarNotAvailableException() {
        CreateRentalRequest request = new CreateRentalRequest(1L, LocalDate.of(2026, 6, 5));

        when(rentalMapper.toModel(request)).thenReturn(rental);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(paymentRepository.existsByRentalUserAndStatus(customer, Payment.Status.PENDING))
                .thenReturn(true);

        assertThrows(CarNotAvailableException.class, () -> rentalService.save(customer, request));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    @DisplayName("CUSTOMER should get all their own rentals when isActive is null")
    void getRentals_CustomerIsActiveNull_ReturnsAllRentals() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Rental> page = new PageImpl<>(List.of(rental));

        when(rentalRepository.findByUserId(1L, pageable)).thenReturn(page);
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        Page<RentalResponse> result = rentalService.getRentals(null, null, customer, pageable);

        assertEquals(1, result.getTotalElements());
        verify(rentalRepository).findByUserId(1L, pageable);
    }

    @Test
    @DisplayName("CUSTOMER should get active rentals when isActive is true")
    void getRentals_CustomerIsActiveTrue_ReturnsActiveRentals() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Rental> page = new PageImpl<>(List.of(rental));

        when(rentalRepository.findByUserIdAndActualReturnDateIsNull(1L, pageable))
                .thenReturn(page);
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        Page<RentalResponse> result = rentalService.getRentals(null, true, customer, pageable);

        assertEquals(1, result.getTotalElements());
        verify(rentalRepository).findByUserIdAndActualReturnDateIsNull(1L, pageable);
    }

    @Test
    @DisplayName("CUSTOMER should get returned rentals when isActive is false")
    void getRentals_CustomerIsActiveFalse_ReturnsReturnedRentals() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Rental> page = new PageImpl<>(List.of(rental));

        when(rentalRepository.findByUserIdAndActualReturnDateIsNotNull(1L, pageable))
                .thenReturn(page);
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        Page<RentalResponse> result = rentalService.getRentals(null, false, customer, pageable);

        assertEquals(1, result.getTotalElements());
        verify(rentalRepository).findByUserIdAndActualReturnDateIsNotNull(1L, pageable);
    }

    @Test
    @DisplayName("MANAGER should use provided userId to get rentals")
    void getRentals_ManagerWithUserId_UsesProvidedUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Rental> page = new PageImpl<>(List.of(rental));

        when(rentalRepository.findByUserId(1L, pageable)).thenReturn(page);
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        rentalService.getRentals(1L, null, manager, pageable);

        verify(rentalRepository).findByUserId(1L, pageable);
    }

    @Test
    @DisplayName("CUSTOMER should ignore provided userId and use their own")
    void getRentals_CustomerIgnoresProvidedUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        when(rentalRepository.findByUserId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(rental)));
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        rentalService.getRentals(99L, null, customer, pageable);

        verify(rentalRepository).findByUserId(1L, pageable);
        verify(rentalRepository, never()).findByUserId(eq(99L), any());
    }

    @Test
    @DisplayName("CUSTOMER should get their own rental by id")
    void getRentalById_AsCustomer_ReturnsOwnRental() {
        when(rentalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rental));
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        RentalResponse actual = rentalService.getRentalById(customer, 1L);

        assertNotNull(actual);
        verify(rentalRepository).findByIdAndUserId(1L, 1L);
        verify(rentalRepository, never()).findById(any());
    }

    @Test
    @DisplayName("MANAGER should get any rental by id")
    void getRentalById_AsManager_ReturnsAnyRental() {
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        RentalResponse actual = rentalService.getRentalById(manager, 1L);

        assertNotNull(actual);
        verify(rentalRepository).findById(1L);
        verify(rentalRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when rental not found for customer")
    void getRentalById_CustomerRentalNotFound_ThrowsEntityNotFoundException() {
        when(rentalRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> rentalService.getRentalById(customer, 99L));
        verify(rentalMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when rental not found for manager")
    void getRentalById_ManagerRentalNotFound_ThrowsEntityNotFoundException() {
        when(rentalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> rentalService.getRentalById(manager, 99L));
        verify(rentalMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Should set actual return date to today")
    void setActualReturnDate_ActiveRental_SetsReturnDateToToday() {
        when(rentalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rental));
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        rentalService.setActualReturnDate(customer, 1L);

        assertEquals(LocalDate.now(), rental.getActualReturnDate());
    }

    @Test
    @DisplayName("Should increment car inventory when car is returned")
    void setActualReturnDate_ActiveRental_IncrementsCarInventory() {
        int initialInventory = car.getInventory();
        when(rentalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rental));
        when(rentalMapper.toDto(rental)).thenReturn(rentalResponse);

        rentalService.setActualReturnDate(customer, 1L);

        assertEquals(initialInventory + 1, car.getInventory());
    }

    @Test
    @DisplayName("Should throw CarAlreadyReturnedException when car is already returned")
    void setActualReturnDate_AlreadyReturned_ThrowsCarAlreadyReturnedException() {
        rental.setActualReturnDate(LocalDate.of(2026, 6, 4));
        when(rentalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rental));

        assertThrows(CarAlreadyReturnedException.class,
                () -> rentalService.setActualReturnDate(customer, 1L));
    }

    @Test
    @DisplayName("Should not modify inventory when car is already returned")
    void setActualReturnDate_AlreadyReturned_DoesNotModifyInventory() {
        rental.setActualReturnDate(LocalDate.of(2026, 6, 4));
        int initialInventory = car.getInventory();
        when(rentalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(rental));

        assertThrows(CarAlreadyReturnedException.class,
                () -> rentalService.setActualReturnDate(customer, 1L));

        assertEquals(initialInventory, car.getInventory());
    }

    @Test
    @DisplayName("Should send overdue notification for each overdue rental")
    void checkOverdueRentals_WithOverdueRentals_SendsNotificationForEach() {
        Rental overdueRental1 = Rental.builder()
                .id(1L)
                .returnDate(LocalDate.now().minusDays(3))
                .car(car)
                .user(customer)
                .build();
        Rental overdueRental2 = Rental.builder()
                .id(2L)
                .returnDate(LocalDate.now().minusDays(1))
                .car(car)
                .user(customer)
                .build();

        when(rentalRepository.findByReturnDateBeforeAndActualReturnDateIsNull(LocalDate.now()))
                .thenReturn(List.of(overdueRental1, overdueRental2));

        rentalService.checkOverdueRentals();

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(notificationService, org.mockito.Mockito.times(2)).sendOverdueMessage(captor.capture());
        assertEquals(List.of(overdueRental1, overdueRental2), captor.getAllValues());
    }

    @Test
    @DisplayName("Should send no notifications when no overdue rentals exist")
    void checkOverdueRentals_NoOverdueRentals_SendsNoNotifications() {
        when(rentalRepository.findByReturnDateBeforeAndActualReturnDateIsNull(LocalDate.now()))
                .thenReturn(List.of());

        rentalService.checkOverdueRentals();

        verify(notificationService, never()).sendOverdueMessage(any());
    }
}
