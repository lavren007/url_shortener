import exceptions.InvalidUrlException;
import exceptions.UrlExpiredException;
import exceptions.UrlNotFoundException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UrlShortenerService {
    private final Map<String, ShortUrl> urlMap;
    private final Map<String, User> userMap;
    private final Random random;
    private final ScheduledExecutorService cleanupScheduler;
    
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
    public UrlShortenerService() {
        this.urlMap = new ConcurrentHashMap<>();
        this.userMap = new ConcurrentHashMap<>();
        this.random = new Random();
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        startCleanupTask();
    }
    
    public User createUser(String name) {
        User user = new User(name);
        userMap.put(user.getId(), user);
        System.out.println("Создан пользователь: " + user);
        return user;
    }
    
    public User getUserById(String userId) {
        return userMap.get(userId);
    }
    
    public String createShortUrl(String originalUrl, String userId) throws InvalidUrlException {
        return createShortUrl(originalUrl, userId, null);
    }
    
    public String createShortUrl(String originalUrl, String userId, Integer maxAccessCount) 
            throws InvalidUrlException {
        
        validateUrl(originalUrl);
        
        if (!userMap.containsKey(userId)) {
            throw new InvalidUrlException("Пользователь не найден: " + userId);
        }
        
        String shortCode = generateShortCode();
        ShortUrl shortUrl = new ShortUrl(shortCode, originalUrl, userId);
        
        if (maxAccessCount != null) {
            shortUrl.setMaxAccessCount(maxAccessCount);
        }
        
        urlMap.put(shortCode, shortUrl);
        return shortCode;
    }
    
    public String getOriginalUrl(String shortCode) throws UrlNotFoundException, UrlExpiredException {
        ShortUrl shortUrl = urlMap.get(shortCode);
        if (shortUrl == null) {
            throw new UrlNotFoundException("Короткая ссылка не найдена: " + shortCode);
        }
        
        if (shortUrl.isExpired()) {
            throw new UrlExpiredException("Срок действия ссылки истек: " + shortCode);
        }
        
        if (shortUrl.isAccessLimitReached()) {
            throw new UrlExpiredException("Лимит переходов по ссылке исчерпан: " + shortCode);
        }
        
        shortUrl.incrementAccessCount();
        return shortUrl.getOriginalUrl();
    }
    
    public void deleteUrl(String shortCode, String userId) throws UrlNotFoundException {
        ShortUrl shortUrl = urlMap.get(shortCode);
        if (shortUrl == null) {
            throw new UrlNotFoundException("Короткая ссылка не найдена: " + shortCode);
        }
        
        if (!shortUrl.getUserId().equals(userId)) {
            throw new UrlNotFoundException("У вас нет прав для удаления этой ссылки");
        }
        
        urlMap.remove(shortCode);
        System.out.println("Удалена ссылка: " + shortCode);
    }
    
    public void updateUrlLimit(String shortCode, String userId, Integer newMaxAccessCount) 
            throws UrlNotFoundException {
        ShortUrl shortUrl = urlMap.get(shortCode);
        if (shortUrl == null) {
            throw new UrlNotFoundException("Короткая ссылка не найдена: " + shortCode);
        }
        
        if (!shortUrl.getUserId().equals(userId)) {
            throw new UrlNotFoundException("У вас нет прав для редактирования этой ссылки");
        }
        
        shortUrl.setMaxAccessCount(newMaxAccessCount);
        System.out.println("Лимит переходов для ссылки " + shortCode + " установлен: " + 
                          (newMaxAccessCount != null ? newMaxAccessCount : "без лимита"));
    }
    
    public List<ShortUrl> getUserUrls(String userId) {
        return urlMap.values().stream()
                .filter(url -> url.getUserId().equals(userId))
                .sorted(Comparator.comparing(ShortUrl::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    public void showAllUrls() {
        if (urlMap.isEmpty()) {
            System.out.println("Нет сохраненных ссылок");
            return;
        }
        
        System.out.println("\n=== Все сокращенные ссылки ===");
        urlMap.values().stream()
                .sorted(Comparator.comparing(ShortUrl::getCreatedAt).reversed())
                .forEach(System.out::println);
    }
    
    public void showUserUrls(String userId) {
        List<ShortUrl> userUrls = getUserUrls(userId);
        if (userUrls.isEmpty()) {
            System.out.println("У вас нет созданных ссылок");
            return;
        }
        
        System.out.println("\n=== Ваши ссылки ===");
        userUrls.forEach(System.out::println);
    }
    
    public void showStatistics() {
        if (urlMap.isEmpty()) {
            System.out.println("Нет данных для статистики");
            return;
        }
        
        System.out.println("\n=== Статистика ===");
        System.out.println("Всего ссылок: " + urlMap.size());
        System.out.println("Всего пользователей: " + userMap.size());
        
        int activeUrls = (int) urlMap.values().stream().filter(ShortUrl::isActive).count();
        System.out.println("Активных ссылок: " + activeUrls);
        
        int totalClicks = urlMap.values().stream().mapToInt(ShortUrl::getAccessCount).sum();
        System.out.println("Всего переходов: " + totalClicks);
        
        double avgClicks = urlMap.values().stream()
                .mapToInt(ShortUrl::getAccessCount)
                .average()
                .orElse(0.0);
        System.out.println("Средних переходов на ссылку: " + String.format("%.2f", avgClicks));
        
        Optional<ShortUrl> mostPopular = urlMap.values().stream()
                .max(Comparator.comparingInt(ShortUrl::getAccessCount));
        
        if (mostPopular.isPresent() && mostPopular.get().getAccessCount() > 0) {
            System.out.println("\nСамая популярная ссылка:");
            System.out.println(mostPopular.get());
        }
    }
    
    public void showTopUrls(int n) {
        System.out.println("\n=== Топ-" + n + " самых популярных ссылок ===");
        urlMap.values().stream()
                .sorted(Comparator.comparingInt(ShortUrl::getAccessCount).reversed())
                .limit(n)
                .forEach(url -> System.out.println(url.getShortCode() + " -> " + 
                        url.getOriginalUrl() + " (" + url.getAccessCount() + " переходов)"));
    }
    
    public List<ShortUrl> searchUrls(String query, String userId) {
        return urlMap.values().stream()
                .filter(url -> url.getUserId().equals(userId) &&
                        (url.getOriginalUrl().toLowerCase().contains(query.toLowerCase()) ||
                         url.getShortCode().toLowerCase().contains(query.toLowerCase())))
                .sorted(Comparator.comparing(ShortUrl::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    public void showRecentUrls(int n) {
        System.out.println("\n=== Последние " + n + " созданных ссылок ===");
        urlMap.values().stream()
                .sorted(Comparator.comparing(ShortUrl::getCreatedAt).reversed())
                .limit(n)
                .forEach(System.out::println);
    }
    
    public void cleanupExpiredUrls() {
        int initialSize = urlMap.size();
        urlMap.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removedCount = initialSize - urlMap.size();
        if (removedCount > 0) {
            System.out.println("Очистка: удалено " + removedCount + " просроченных ссылок");
        }
    }
    
    private String generateShortCode() {
        String shortCode;
        do {
            StringBuilder sb = new StringBuilder(Config.getShortCodeLength());
            for (int i = 0; i < Config.getShortCodeLength(); i++) {
                sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            shortCode = sb.toString();
        } while (urlMap.containsKey(shortCode));
        return shortCode;
    }
    
    private void validateUrl(String url) throws InvalidUrlException {
        if (url == null || url.trim().isEmpty()) {
            throw new InvalidUrlException("URL не может быть пустым");
        }
        if (!url.matches("^https?://.*")) {
            throw new InvalidUrlException("URL должен начинаться с http:// или https://");
        }
        if (url.length() > Config.getMaxUrlLength()) {
            throw new InvalidUrlException("URL слишком длинный (максимум " + 
                    Config.getMaxUrlLength() + " символов)");
        }
    }
    
    private void startCleanupTask() {
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredUrls, 
                Config.getCleanupIntervalMinutes(), 
                Config.getCleanupIntervalMinutes(), 
                TimeUnit.MINUTES);
    }
    
    public void shutdown() {
        cleanupScheduler.shutdown();
    }
}