import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();
    
    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Файл config.properties не найден, используются значения по умолчанию");
                setDefaultProperties();
            } else {
                properties.load(input);
            }
        } catch (IOException e) {
            System.out.println("Ошибка загрузки конфигурации: " + e.getMessage());
            setDefaultProperties();
        }
    }
    
    private static void setDefaultProperties() {
        properties.setProperty("base.url", "http://short.url/");
        properties.setProperty("short.code.length", "6");
        properties.setProperty("default.url.ttl.hours", "24");
        properties.setProperty("max.url.length", "2048");
        properties.setProperty("cleanup.interval.minutes", "30");
    }
    
    public static String getBaseUrl() {
        return properties.getProperty("base.url");
    }
    
    public static int getShortCodeLength() {
        return Integer.parseInt(properties.getProperty("short.code.length"));
    }
    
    public static int getDefaultUrlTtlHours() {
        return Integer.parseInt(properties.getProperty("default.url.ttl.hours"));
    }
    
    public static int getMaxUrlLength() {
        return Integer.parseInt(properties.getProperty("max.url.length"));
    }
    
    public static int getCleanupIntervalMinutes() {
        return Integer.parseInt(properties.getProperty("cleanup.interval.minutes"));
    }
}