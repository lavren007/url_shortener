import exceptions.UrlExpiredException;
import exceptions.UrlNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    @Test
    @DisplayName("Полный сценарий: создание пользователя, ссылки, переходы, удаление")
    void testFullUserScenario() throws Exception {
        UrlShortenerService service = new UrlShortenerService();
        
        String userId = service.createUser("Integration Test User").getId();
        assertNotNull(userId);
        
        String originalUrl = "https://integration-test.com";
        String shortCode = service.createShortUrl(originalUrl, userId, 2);
        assertNotNull(shortCode);
        
        String retrievedUrl = service.getOriginalUrl(shortCode);
        assertEquals(originalUrl, retrievedUrl);
        
        assertDoesNotThrow(() -> service.getOriginalUrl(shortCode));
        
        assertThrows(UrlExpiredException.class, () -> 
            service.getOriginalUrl(shortCode));
        
        var userUrls = service.getUserUrls(userId);
        assertEquals(1, userUrls.size());
        assertEquals(shortCode, userUrls.get(0).getShortCode());
        
        service.deleteUrl(shortCode, userId);
        
        assertThrows(UrlNotFoundException.class, () -> 
            service.getOriginalUrl(shortCode));
    }

    @Test
    @DisplayName("Мультипользовательский сценарий")
    void testMultiUserScenario() throws Exception {
        UrlShortenerService service = new UrlShortenerService();
        
        String user1Id = service.createUser("User 1").getId();
        String user2Id = service.createUser("User 2").getId();
        
        String commonUrl = "https://common-website.com";
        String user1ShortCode = service.createShortUrl(commonUrl, user1Id);
        String user2ShortCode = service.createShortUrl(commonUrl, user2Id);
        
        assertNotEquals(user1ShortCode, user2ShortCode);
        
        assertEquals(1, service.getUserUrls(user1Id).size());
        assertEquals(1, service.getUserUrls(user2Id).size());
        
        assertThrows(UrlNotFoundException.class, () -> 
            service.deleteUrl(user2ShortCode, user1Id));
        
        assertThrows(UrlNotFoundException.class, () -> 
            service.deleteUrl(user1ShortCode, user2Id));
    }

    @Test
    @DisplayName("Сценарий поиска и фильтрации")
    void testSearchAndFilterScenario() throws Exception {
        UrlShortenerService service = new UrlShortenerService();
        String userId = service.createUser("Search Test User").getId();
        
        service.createShortUrl("https://google.com/search/query1", userId);
        service.createShortUrl("https://example.com/test/page", userId);
        service.createShortUrl("https://github.com/project/repo", userId);
        
        var googleResults = service.searchUrls("google", userId);
        assertEquals(1, googleResults.size());
        
        var exampleResults = service.searchUrls("example", userId);
        assertEquals(1, exampleResults.size());
        
        var comResults = service.searchUrls(".com", userId);
        assertEquals(3, comResults.size());
        
        var noResults = service.searchUrls("nonexistent", userId);
        assertTrue(noResults.isEmpty());
    }
}