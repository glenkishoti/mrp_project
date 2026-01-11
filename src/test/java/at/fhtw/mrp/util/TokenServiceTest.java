package at.fhtw.mrp.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenService
 * Tests token generation and format validation
 */
class TokenServiceTest {

    @Test
    @DisplayName("Test 1: Generate token produces valid format")
    void testGenerateToken_ValidFormat() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String username = "testuser";

        // Act
        String token = TokenService.generateToken(userId, username);

        // Assert
        assertNotNull(token, "Generated token should not be null");
        assertTrue(token.contains(";"), "Token should contain semicolon separator");
        assertTrue(token.startsWith(userId.toString()), "Token should start with userId");
        assertTrue(token.contains(username), "Token should contain username");

        // Verify format: "userId;username;secret"
        String[] parts = token.split(";");
        assertEquals(3, parts.length, "Token should have 3 parts separated by semicolons");
        assertEquals(userId.toString(), parts[0], "First part should be userId");
        assertEquals(username, parts[1], "Second part should be username");
        assertFalse(parts[2].isEmpty(), "Third part (secret) should not be empty");
    }

    @Test
    @DisplayName("Test 2: Two tokens for same user have different secrets")
    void testGenerateToken_DifferentSecrets() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String username = "testuser";

        // Act
        String token1 = TokenService.generateToken(userId, username);
        String token2 = TokenService.generateToken(userId, username);

        // Assert
        assertNotEquals(token1, token2, "Two tokens should be different due to random secret");

        // But both should have same userId and username
        String[] parts1 = token1.split(";");
        String[] parts2 = token2.split(";");

        assertEquals(parts1[0], parts2[0], "UserId should be same");
        assertEquals(parts1[1], parts2[1], "Username should be same");
        assertNotEquals(parts1[2], parts2[2], "Secrets should be different");
    }

    @Test
    @DisplayName("Test 3: Token contains valid UUID as first part")
    void testGenerateToken_ValidUUID() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String username = "testuser";

        // Act
        String token = TokenService.generateToken(userId, username);
        String[] parts = token.split(";");

        // Assert
        assertDoesNotThrow(() -> {
            UUID parsedId = UUID.fromString(parts[0]);
            assertEquals(userId, parsedId, "Parsed UUID should match original");
        }, "First part should be a valid UUID");
    }
}