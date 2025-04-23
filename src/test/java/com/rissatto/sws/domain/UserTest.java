package com.rissatto.sws.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateViaConstructorWithValidIdAndName() {
        // Arrange
        UUID id = UUID.randomUUID();
        String name = "John Doe";

        // Act
        User u = new User(id, name);

        // Assert
        assertEquals(id, u.id());
        assertEquals(name, u.name());
    }

    @Test
    void shouldCreateViaFactoryWithValidName() {
        // Arrange
        String name = "John Doe";

        // Act
        User u = User.create(name);

        // Assert
        assertNotNull(u.id());
        assertEquals(name, u.name());
    }

    @Test
    void shouldTrimWhitespaceFromNameViaConstructor() {
        // Arrange
        UUID id = UUID.randomUUID();
        String name = " John Doe ";

        // Act
        User u = new User(id, name);

        // Assert
        assertEquals("John Doe", u.name());
    }

    @Test
    void shouldTrimWhitespaceFromNameViaFactory() {
        // Arrange
        String name = " John Doe ";

        // Act
        User u = User.create(name);

        // Assert
        assertEquals("John Doe", u.name());
    }

    @Test
    void shouldThrowViaConstructorWhenIdIsNull() {
        // Arrange
        String name = "John Doe";

        // Act
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new User(null, name));

        // Assert
        assertTrue(ex.getMessage().contains("id must not be null"));
    }

    @Test
    void shouldThrowWhenCreateViaFactoryWithNullName() {
        // Act
        NullPointerException ex = assertThrows(NullPointerException.class, () -> User.create(null));

        // Assert
        assertTrue(ex.getMessage().contains("name must not be null"));
    }

    @Test
    void shouldThrowViaConstructorWhenNameIsNull() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new User(id, null));

        // Assert
        assertTrue(ex.getMessage().contains("name must not be null"));
    }

    @Test
    void shouldThrowViaConstructorWhenNameIsBlank() {
        // Arrange
        UUID id = UUID.randomUUID();
        String name = " ";

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new User(id, name));

        // Assert
        assertTrue(ex.getMessage().contains("name must not be blank"));
    }

    @Test
    void shouldThrowWhenCreateViaFactoryWithBlankName() {
        // Arrange
        String name = " ";

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> User.create(name));

        // Assert
        assertTrue(ex.getMessage().contains("name must not be blank"));
    }
}
