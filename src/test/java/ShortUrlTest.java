import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class ShortUrlTest {
    private ShortUrl shortUrl;
    private final String testUserId = "test-user-id";

    @BeforeEach
    void setUp() {
        shortUrl = new ShortUrl("abc123", "https://example.com", testUserId);
    }

    @Test
    void testShortUrlCreation() {
        assertEquals("abc123", shortUrl.getShortCode());
        assertEquals("https://example.com", shortUrl.getOriginalUrl());
        assertEquals(testUserId, shortUrl.getUserId());
        assertEquals(0, shortUrl.getAccessCount());
        assertNull(shortUrl.getMaxAccessCount());
        assertNotNull(shortUrl.getCreatedAt());
        assertNotNull(shortUrl.getExpiresAt());
    }

    @Test
    void testIncrementAccessCount() {
        assertEquals(0, shortUrl.getAccessCount());
        shortUrl.incrementAccessCount();
        assertEquals(1, shortUrl.getAccessCount());
        shortUrl.incrementAccessCount();
        assertEquals(2, shortUrl.getAccessCount());
    }

    @Test
    void testIsActive_NoLimit() {
        assertTrue(shortUrl.isActive());
    }

    @Test
    void testIsActive_WithLimitNotReached() {
        shortUrl.setMaxAccessCount(5);
        assertTrue(shortUrl.isActive());
    }

    @Test
    void testIsActive_WithLimitReached() {
        shortUrl.setMaxAccessCount(2);
        shortUrl.incrementAccessCount();
        shortUrl.incrementAccessCount();
        assertTrue(shortUrl.isAccessLimitReached());
        assertFalse(shortUrl.isActive());
    }

    @Test
    void testIsExpired() {
        // Создаем ссылку с прошлой датой создания и истечением
        LocalDateTime pastCreatedAt = LocalDateTime.now().minusHours(25);
        LocalDateTime pastExpiresAt = pastCreatedAt.plusHours(24); // уже истекло
        
        ShortUrl expiredUrl = new ShortUrl("expired", "https://example.com", testUserId, 
                                         pastCreatedAt, pastExpiresAt, 0, null);
        
        assertTrue(expiredUrl.isExpired());
        assertFalse(expiredUrl.isActive());
    }

    @Test
    void testIsNotExpired() {
        // Создаем ссылку с будущей датой истечения
        LocalDateTime futureCreatedAt = LocalDateTime.now().minusHours(1);
        LocalDateTime futureExpiresAt = LocalDateTime.now().plusHours(23);
        
        ShortUrl activeUrl = new ShortUrl("active", "https://example.com", testUserId, 
                                        futureCreatedAt, futureExpiresAt, 0, null);
        
        assertFalse(activeUrl.isExpired());
        assertTrue(activeUrl.isActive());
    }

    @Test
    void testSetMaxAccessCount() {
        assertNull(shortUrl.getMaxAccessCount());
        shortUrl.setMaxAccessCount(10);
        assertEquals(10, shortUrl.getMaxAccessCount());
        
        shortUrl.setMaxAccessCount(null);
        assertNull(shortUrl.getMaxAccessCount());
    }

    @Test
    void testToString() {
        String toString = shortUrl.toString();
        assertTrue(toString.contains("abc123"));
        assertTrue(toString.contains("https://example.com"));
        assertTrue(toString.contains("АКТИВНА"));
    }
}