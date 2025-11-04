import java.time.LocalDateTime;

public class ShortUrl {
    private final String shortCode;
    private final String originalUrl;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private final String userId;
    private int accessCount;
    private Integer maxAccessCount;
    
    public ShortUrl(String shortCode, String originalUrl, String userId) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusHours(Config.getDefaultUrlTtlHours());
        this.accessCount = 0;
        this.maxAccessCount = null;
    }
    
    public ShortUrl(String shortCode, String originalUrl, String userId, 
                   LocalDateTime createdAt, LocalDateTime expiresAt, 
                   int accessCount, Integer maxAccessCount) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.accessCount = accessCount;
        this.maxAccessCount = maxAccessCount;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isAccessLimitReached() {
        return maxAccessCount != null && accessCount >= maxAccessCount;
    }
    
    public boolean isActive() {
        return !isExpired() && !isAccessLimitReached();
    }
    
    public void incrementAccessCount() {
        accessCount++;
    }
    
    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getUserId() { return userId; }
    public int getAccessCount() { return accessCount; }
    public Integer getMaxAccessCount() { return maxAccessCount; }
    
    public void setMaxAccessCount(Integer maxAccessCount) {
        this.maxAccessCount = maxAccessCount;
    }
    
    @Override
    public String toString() {
        String status = isActive() ? "АКТИВНА" : "НЕАКТИВНА";
        String limitInfo = maxAccessCount != null ? 
            " (лимит: " + maxAccessCount + ")" : " (без лимита)";
        return shortCode + " -> " + originalUrl + 
               " [переходов: " + accessCount + limitInfo + ", создана: " + createdAt + 
               ", истекает: " + expiresAt + "] - " + status;
    }
}