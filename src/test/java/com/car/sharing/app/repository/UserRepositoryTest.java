package com.car.sharing.app.repository;

import static com.car.sharing.app.util.TestConstants.CLEANUP_USERS_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.car.sharing.app.config.CustomMySQLContainer;
import com.car.sharing.app.entity.User;
import com.car.sharing.app.repository.user.UserRepository;
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
@Sql(scripts = CLEANUP_USERS_PATH, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserRepositoryTest {

    static {
        CustomMySQLContainer container = CustomMySQLContainer.getInstance();
        container.start();
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save user and return it by id")
    void shouldSaveUserAndReturnItById() {
        User user = TestUtil.createUser();
        User saved = userRepository.save(user);

        User actual = userRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(saved.getId());
        assertEquals(saved.getEmail(), actual.getEmail());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindByEmail() {
        User user = TestUtil.createUser();
        userRepository.save(user);

        User actual = userRepository.findByEmail(user.getEmail()).orElseThrow();

        assertEquals(user.getEmail(), actual.getEmail());
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void shouldReturnEmpty_WhenEmailNotFound() {
        boolean found = userRepository.findByEmail("nonexistent@example.com").isPresent();

        assertFalse(found);
    }

    @Test
    @DisplayName("Should return true when email exists")
    void shouldReturnTrue_WhenEmailExists() {
        User user = TestUtil.createUser();
        userRepository.save(user);

        boolean exists = userRepository.existsByEmail(user.getEmail());

        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalse_WhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertFalse(exists);
    }

    @Test
    @DisplayName("Should soft delete user")
    void shouldSoftDeleteUser() {
        User user = TestUtil.createUser();
        User saved = userRepository.save(user);

        userRepository.deleteById(saved.getId());

        assertTrue(userRepository.findById(saved.getId()).isEmpty(),
                "User must be soft deleted!");
    }

    @Test
    @DisplayName("Should throw exception when saving user with null fields")
    void shouldThrowException_WhenSavingInvalidUser() {
        User user = new User();

        assertThrows(Exception.class, () ->
                userRepository.save(user)
        );
    }
}
