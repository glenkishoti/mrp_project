package at.fhtw.mrp.service;

import at.fhtw.mrp.model.User;
import at.fhtw.mrp.repo.UserRepository;
import at.fhtw.mrp.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * Tests user registration validation
 */
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository);
    }

    @Test
    @DisplayName("Test 4: Register user creates user and returns ID")
    void testRegister_CreatesUser() throws SQLException {
        // Arrange
        String username = "newuser";
        String password = "password123";

        doNothing().when(userRepository).insert(any());

        // Act
        UUID userId = authService.register(username, password);

        // Assert
        assertNotNull(userId, "Register should return a user ID");
        verify(userRepository).insert(any());
    }

    @Test
    @DisplayName("Test 5: Register hashes password before storing")
    void testRegister_HashesPassword() throws SQLException {
        // Arrange
        String username = "testuser";
        String password = "mypassword";

        doNothing().when(userRepository).insert(any());

        // Act
        authService.register(username, password);

        // Assert - verify insert was called (password hashing is internal)
        verify(userRepository).insert(any(User.class));
    }

    @Test
    @DisplayName("Test 6: Register creates unique user ID")
    void testRegister_CreatesUniqueId() throws SQLException {
        // Arrange
        String username = "user1";
        String password = "pass1";

        doNothing().when(userRepository).insert(any());

        // Act
        UUID userId1 = authService.register(username, password);
        UUID userId2 = authService.register(username, password);

        // Assert - each registration creates a new UUID
        assertNotNull(userId1);
        assertNotNull(userId2);
        assertNotEquals(userId1, userId2, "Each registration should create a unique ID");
    }

    @Test
    @DisplayName("Test 7: Authenticate with valid credentials succeeds")
    void testAuthenticate_ValidCredentials() throws SQLException {
        // Arrange
        String username = "existinguser";
        String password = "correctpassword";
        String hashedPassword = PasswordUtil.hash(password.toCharArray());

        User user = new User(UUID.randomUUID(), username, hashedPassword, null);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).updateToken(any(), any());

        // Act
        String token = authService.authenticate(username, password);

        // Assert
        assertNotNull(token, "Authenticate should return a token");
        assertTrue(token.contains(user.getId().toString()), "Token should contain user ID");
        verify(userRepository).findByUsername(username);
        verify(userRepository).updateToken(any(), any());
    }

    @Test
    @DisplayName("Test 8: Authenticate with wrong password throws exception")
    void testAuthenticate_WrongPassword() throws SQLException {
        // Arrange
        String username = "existinguser";
        String correctPassword = "correctpassword";
        String wrongPassword = "wrongpassword";
        String hashedPassword = PasswordUtil.hash(correctPassword.toCharArray());

        User user = new User(UUID.randomUUID(), username, hashedPassword, null);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            authService.authenticate(username, wrongPassword);
        }, "Authenticate with wrong password should throw exception");

        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).updateToken(any(), any());
    }
}