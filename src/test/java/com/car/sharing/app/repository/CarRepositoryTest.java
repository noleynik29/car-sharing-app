package com.car.sharing.app.repository;

import static com.car.sharing.app.util.TestConstants.CLEANUP_CARS_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.car.sharing.app.config.CustomMySQLContainer;
import com.car.sharing.app.entity.Car;
import com.car.sharing.app.repository.car.CarRepository;
import com.car.sharing.app.util.TestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = CLEANUP_CARS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CarRepositoryTest {

    static {
        CustomMySQLContainer container = CustomMySQLContainer.getInstance();
        container.start();
    }

    @Autowired
    private CarRepository carRepository;

    @Test
    @DisplayName("Should save car and return it by id")
    void shouldSaveCarAndReturnItById() {
        Car car = TestUtil.createCar();
        Car saved = carRepository.save(car);

        Car actual = carRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(saved.getId());
        assertEquals(saved.getBrand(), actual.getBrand());
        assertEquals(saved.getModel(), actual.getModel());
    }

    @Test
    @DisplayName("Should soft delete car")
    void shouldSoftDeleteCar() {
        Car car = TestUtil.createCar();
        Car saved = carRepository.save(car);

        carRepository.deleteById(saved.getId());

        assertTrue(carRepository.findById(saved.getId()).isEmpty(),
                "Car must be soft deleted!");
    }

    @Test
    @DisplayName("Should throw exception when saving car with null fields")
    void shouldThrowException_WhenSavingInvalidCar() {
        Car car = new Car();

        assertThrows(Exception.class, () ->
                carRepository.save(car)
        );
    }
}
