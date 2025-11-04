import exceptions.InvalidUrlException;
import exceptions.UrlExpiredException;
import exceptions.UrlNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UrlShortenerServiceTest {
    private UrlShortenerService service;
    private String testUserId;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService();
        testUserId = service.createUser("Test User").getId();
    }

    @Test
    @DisplayName("Создание короткой ссылки с валидным URL")
    void testCreateShortUrl_ValidUrl() throws Exception {
        String shortCode = service.createShortUrl("https://example.com", testUserId);
        
        assertNotNull(shortCode);
        assertFalse(shortCode.isEmpty());
        
        String originalUrl = service.getOriginalUrl(shortCode);
        assertEquals("https://example.com", originalUrl);
    }

    @Test
    @DisplayName("Создание короткой ссылки с невалидным URL")
    void testCreateShortUrl_InvalidUrl() {
        // Пустой URL
        assertThrows(InvalidUrlException.class, () -> 
            service.createShortUrl("", testUserId));
        
        // URL без протокола
        assertThrows(InvalidUrlException.class, () -> 
            service.createShortUrl("example.com", testUserId));
        
        // Слишком длинный URL
        String longUrl = "https://example.com/" + "a".repeat(5000);
        assertThrows(InvalidUrlException.class, () -> 
            service.createShortUrl(longUrl, testUserId));
    }

    @Test
    @DisplayName("Уникальность ссылок для разных пользователей")
    void testUniqueUrlsForDifferentUsers() throws Exception {
        String user2Id = service.createUser("User 2").getId();
        
        String url1 = "https://same-url.com";
        String shortCode1 = service.createShortUrl(url1, testUserId);
        String shortCode2 = service.createShortUrl(url1, user2Id);
        
        assertNotEquals(shortCode1, shortCode2);
    }

    @Test
    @DisplayName("Лимит переходов по ссылке")
    void testAccessLimit() throws Exception {
        String shortCode = service.createShortUrl("https://limited.com", testUserId, 2);
        
        // Первые два перехода должны работать
        service.getOriginalUrl(shortCode);
        service.getOriginalUrl(shortCode);
        
        // Третий переход должен выбросить исключение
        assertThrows(UrlExpiredException.class, () -> 
            service.getOriginalUrl(shortCode));
    }

    @Test
    @DisplayName("Получение несуществующей ссылки")
    void testGetNonExistentUrl() {
        assertThrows(UrlNotFoundException.class, () -> 
            service.getOriginalUrl("nonexistent"));
    }

    @Test
    @DisplayName("Удаление ссылки")
    void testDeleteUrl() throws Exception {
        String shortCode = service.createShortUrl("https://delete-me.com", testUserId);
        
        // Ссылка должна существовать
        assertDoesNotThrow(() -> service.getOriginalUrl(shortCode));
        
        // Удаляем ссылку
        service.deleteUrl(shortCode, testUserId);
        
        // Ссылка должна быть удалена
        assertThrows(UrlNotFoundException.class, () -> 
            service.getOriginalUrl(shortCode));
    }

    @Test
    @DisplayName("Удаление чужой ссылки")
    void testDeleteOtherUserUrl() throws Exception {
        String user2Id = service.createUser("Other User").getId();
        String shortCode = service.createShortUrl("https://other-user.com", user2Id);
        
        // Попытка удалить чужую ссылку
        assertThrows(UrlNotFoundException.class, () -> 
            service.deleteUrl(shortCode, testUserId));
    }

    @Test
    @DisplayName("Обновление лимита переходов")
    void testUpdateAccessLimit() throws Exception {
        String shortCode = service.createShortUrl("https://update-limit.com", testUserId);
        
        // Устанавливаем лимит
        service.updateUrlLimit(shortCode, testUserId, 3);
        
        // Проверяем, что лимит установлен
        service.getOriginalUrl(shortCode);
        service.getOriginalUrl(shortCode);
        service.getOriginalUrl(shortCode);
        
        // Четвертый переход должен выбросить исключение
        assertThrows(UrlExpiredException.class, () -> 
            service.getOriginalUrl(shortCode));
        
        // Снимаем лимит
        service.updateUrlLimit(shortCode, testUserId, null);
        
        // Теперь переходы должны работать без ограничений
        assertDoesNotThrow(() -> service.getOriginalUrl(shortCode));
    }

    @Test
    @DisplayName("Поиск ссылок")
    void testSearchUrls() throws Exception {
        service.createShortUrl("https://google.com/search", testUserId);
        service.createShortUrl("https://example.com/test", testUserId);
        
        List<ShortUrl> googleResults = service.searchUrls("google", testUserId);
        assertEquals(1, googleResults.size());
        assertTrue(googleResults.get(0).getOriginalUrl().contains("google"));
        
        List<ShortUrl> exampleResults = service.searchUrls("example", testUserId);
        assertEquals(1, exampleResults.size());
        assertTrue(exampleResults.get(0).getOriginalUrl().contains("example"));
        
        // Поиск по короткому коду
        String shortCode = service.createShortUrl("https://unique.com", testUserId);
        List<ShortUrl> codeResults = service.searchUrls(shortCode, testUserId);
        assertEquals(1, codeResults.size());
    }

    @Test
    @DisplayName("Получение ссылок пользователя")
    void testGetUserUrls() throws Exception {
        String user2Id = service.createUser("User 2").getId();
        
        // Создаем ссылки для обоих пользователей
        service.createShortUrl("https://user1-url1.com", testUserId);
        service.createShortUrl("https://user1-url2.com", testUserId);
        service.createShortUrl("https://user2-url1.com", user2Id);
        
        List<ShortUrl> user1Urls = service.getUserUrls(testUserId);
        assertEquals(2, user1Urls.size());
        
        List<ShortUrl> user2Urls = service.getUserUrls(user2Id);
        assertEquals(1, user2Urls.size());
    }

    @Test
    @DisplayName("Статистика сервиса")
    void testStatistics() throws Exception {
        // Создаем несколько ссылок с переходами
        String url1 = service.createShortUrl("https://stats1.com", testUserId);
        String url2 = service.createShortUrl("https://stats2.com", testUserId, 5);
        
        // Совершаем переходы
        service.getOriginalUrl(url1);
        service.getOriginalUrl(url1);
        service.getOriginalUrl(url2);
        
        // Проверяем, что метод не падает
        assertDoesNotThrow(() -> service.showStatistics());
    }

    @Test
    @DisplayName("Топ популярных ссылок")
    void testTopUrls() throws Exception {
        String url1 = service.createShortUrl("https://popular1.com", testUserId);
        String url2 = service.createShortUrl("https://popular2.com", testUserId);
        
        // Создаем больше переходов для второй ссылки
        service.getOriginalUrl(url2);
        service.getOriginalUrl(url2);
        service.getOriginalUrl(url2);
        service.getOriginalUrl(url1);
        
        // Проверяем, что метод не падает
        assertDoesNotThrow(() -> service.showTopUrls(2));
    }

    @Test
    @DisplayName("Очистка просроченных ссылок")
    void testCleanupExpiredUrls() throws Exception {
        // Создаем ссылку с истекшим сроком через конструктор с датами
        String expiredCode = "expired123";
        LocalDateTime pastCreatedAt = LocalDateTime.now().minusHours(25);
        LocalDateTime pastExpiresAt = pastCreatedAt.plusHours(24);
        
        ShortUrl expiredUrl = new ShortUrl(expiredCode, "https://expired.com", testUserId,
                                         pastCreatedAt, pastExpiresAt, 0, null);
        
        // Добавляем ссылку в сервис через рефлексию
        Field urlMapField = UrlShortenerService.class.getDeclaredField("urlMap");
        urlMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, ShortUrl> urlMap = 
            (java.util.Map<String, ShortUrl>) urlMapField.get(service);
        urlMap.put(expiredCode, expiredUrl);
        
        // Создаем активную ссылку
        String activeCode = service.createShortUrl("https://active.com", testUserId);
        
        // Выполняем очистку
        service.cleanupExpiredUrls();
        
        // Проверяем, что просроченная ссылка удалена, а активная осталась
        assertFalse(urlMap.containsKey(expiredCode));
        assertTrue(urlMap.containsKey(activeCode));
    }

    @Test
    @DisplayName("Создание пользователя")
    void testCreateUser() {
        User user = service.createUser("New User");
        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals("New User", user.getName());
        
        // Проверяем, что пользователь добавлен в сервис
        User retrievedUser = service.getUserById(user.getId());
        assertNotNull(retrievedUser);
        assertEquals(user.getId(), retrievedUser.getId());
    }
}