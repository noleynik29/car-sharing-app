package com.car.sharing.app.repository.rental;

import com.car.sharing.app.entity.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {
    Page<Rental> findByUserIdAndActualReturnDateIsNotNull(Long userId, Pageable pageable);

    Page<Rental> findByUserIdAndActualReturnDateIsNull(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    Page<Rental> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"car", "user"})
    Optional<Rental> findByIdAndUserId(Long id, Long userId);

    List<Rental> findByReturnDateBeforeAndActualReturnDateIsNull(LocalDate returnDateBefore);
}
