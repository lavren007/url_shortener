import exceptions.InvalidUrlException;
import exceptions.UrlExpiredException;
import exceptions.UrlNotFoundException;

import java.awt.*;
import java.net.URI;
import java.util.List;
import java.util.*;

public class Main {
    private static final UrlShortenerService service = new UrlShortenerService();
    private static final Scanner scanner = new Scanner(System.in);
    private static String currentUserId = null;
    
    public static void main(String[] args) {
        boolean running = true;
        
        System.out.println("🐝 Добро пожаловать в Сервис Сокращения Ссылок!");
        System.out.println("══════════════════════════════════════════");
        
        setupUser();
        
        while (running) {
            printMenu();
            int choice = getIntInput("Выберите действие: ");
            
            try {
                switch (choice) {
                    case 1:
                        createShortUrl();
                        break;
                    case 2:
                        getOriginalUrl();
                        break;
                    case 3:
                        deleteUrl();
                        break;
                    case 4:
                        service.showUserUrls(currentUserId);
                        break;
                    case 5:
                        searchUrls();
                        break;
                    case 6:
                        service.showStatistics();
                        break;
                    case 7:
                        showTopUrls();
                        break;
                    case 8:
                        showRecentUrls();
                        break;
                    case 9:
                        updateUrlLimit();
                        break;
                    case 10:
                        switchUser();
                        break;
                    case 11:
                        service.showAllUrls();
                        break;
                    case 0:
                        System.out.println("👋 До свидания!");
                        running = false;
                        break;
                    default:
                        System.out.println("❌ Неверный выбор");
                }
            } catch (Exception e) {
                System.out.println("❌ Ошибка: " + e.getMessage());
            }
            
            if (running) {
                System.out.println("\n══════════════════════════════════════════");
                System.out.print("Нажмите Enter для продолжения...");
                scanner.nextLine();
            }
        }
        
        service.shutdown();
        scanner.close();
    }
    
    private static void printMenu() {
        System.out.println("\n🎯 ГЛАВНОЕ МЕНЮ");
        System.out.println("══════════════════════════════════════════");
        System.out.println(" 1.  Создать короткую ссылку");
        System.out.println(" 2.  Перейти по короткой ссылке");
        System.out.println(" 3.  Удалить ссылку");
        System.out.println(" 4.  Мои ссылки");
        System.out.println(" 5.  Поиск ссылок");
        System.out.println(" 6.  Статистика");
        System.out.println(" 7.  Топ популярных");
        System.out.println(" 8.  Последние созданные");
        System.out.println(" 9.  Изменить лимит переходов");
        System.out.println(" 10. Сменить пользователя");
        System.out.println(" 11. Все ссылки (админ)");
        System.out.println(" 0.  Выход");
        System.out.println("══════════════════════════════════════════");
        if (currentUserId != null) {
            User currentUser = service.getUserById(currentUserId);
            System.out.println("👤 Текущий пользователь: " + currentUser);
        } else {
            System.out.println("👤 Текущий пользователь: Не выбран");
        }
    }
    
    private static void setupUser() {
        System.out.println("\n👤 Настройка пользователя");
        System.out.println("1. Создать нового пользователя");
        System.out.println("2. Использовать существующего (по ID)");
        
        int choice = getIntInput("Выберите вариант: ");
        
        if (choice == 1) {
            System.out.print("Введите имя пользователя: ");
            String name = scanner.nextLine();
            User user = service.createUser(name);
            currentUserId = user.getId();
        } else if (choice == 2) {
            System.out.print("Введите ID пользователя: ");
            String userId = scanner.nextLine();
            User user = service.getUserById(userId);
            if (user != null) {
                currentUserId = userId;
                System.out.println("✅ Пользователь найден: " + user);
            } else {
                System.out.println("❌ Пользователь не найден. Создаем нового...");
                System.out.print("Введите имя пользователя: ");
                String name = scanner.nextLine();
                user = service.createUser(name);
                currentUserId = user.getId();
            }
        } else {
            System.out.println("❌ Неверный выбор. Создаем нового пользователя...");
            System.out.print("Введите имя пользователя: ");
            String name = scanner.nextLine();
            User user = service.createUser(name);
            currentUserId = user.getId();
        }
    }
    
    private static void createShortUrl() throws InvalidUrlException {
        System.out.print("🌐 Введите полный URL: ");
        String originalUrl = scanner.nextLine();
        
        System.out.print("🔢 Установить лимит переходов? (оставьте пустым для отсутствия лимита): ");
        String limitInput = scanner.nextLine();
        Integer maxAccessCount = null;
        
        if (!limitInput.trim().isEmpty()) {
            try {
                maxAccessCount = Integer.parseInt(limitInput);
                if (maxAccessCount <= 0) {
                    System.out.println("⚠️ Лимит должен быть положительным числом. Устанавливается без лимита.");
                    maxAccessCount = null;
                }
            } catch (NumberFormatException e) {
                System.out.println("⚠️ Неверный формат числа. Устанавливается без лимита.");
            }
        }
        
        String shortCode = service.createShortUrl(originalUrl, currentUserId, maxAccessCount);
        System.out.println("\n✅ Короткая ссылка создана!");
        System.out.println("🌐 Оригинальный URL: " + originalUrl);
        System.out.println("🔗 Короткая ссылка: " + Config.getBaseUrl() + shortCode);
        System.out.println("🔑 Код: " + shortCode);
        if (maxAccessCount != null) {
            System.out.println("🎯 Лимит переходов: " + maxAccessCount);
        }
    }
    
    private static void getOriginalUrl() throws UrlNotFoundException, UrlExpiredException {
        System.out.print("🔑 Введите короткий код: ");
        String shortCode = scanner.nextLine();
        
        String originalUrl = service.getOriginalUrl(shortCode);
        System.out.println("\n🔗 Короткая ссылка: " + Config.getBaseUrl() + shortCode);
        System.out.println("🌐 Оригинальный URL: " + originalUrl);
        
        System.out.print("🖥️  Открыть в браузере? (y/n): ");
        String openInBrowser = scanner.nextLine();
        
        if (openInBrowser.equalsIgnoreCase("y")) {
            try {
                // ИСПРАВЛЕНИЕ: открываем оригинальный URL
                Desktop.getDesktop().browse(new URI(originalUrl));
                System.out.println("✅ Открываю оригинальный URL в браузере...");
            } catch (Exception e) {
                System.out.println("❌ Не удалось открыть в браузере: " + e.getMessage());
            }
        }
    }
    
    private static void deleteUrl() throws UrlNotFoundException {
        System.out.print("🔑 Введите короткий код для удаления: ");
        String shortCode = scanner.nextLine();
        service.deleteUrl(shortCode, currentUserId);
    }
    
    private static void searchUrls() {
        System.out.print("🔍 Введите поисковый запрос: ");
        String query = scanner.nextLine();
        List<ShortUrl> results = service.searchUrls(query, currentUserId);
        
        if (results.isEmpty()) {
            System.out.println("❌ Ничего не найдено");
        } else {
            System.out.println("\n✅ Найдено ссылок: " + results.size());
            for (ShortUrl result : results) {
                System.out.println(result);
            }
        }
    }
    
    private static void showTopUrls() {
        int n = getIntInput("📊 Сколько ссылок показать: ");
        service.showTopUrls(n);
    }
    
    private static void showRecentUrls() {
        int n = getIntInput("🕒 Сколько ссылок показать: ");
        service.showRecentUrls(n);
    }
    
    private static void updateUrlLimit() throws UrlNotFoundException {
        System.out.print("🔑 Введите короткий код: ");
        String shortCode = scanner.nextLine();
        
        System.out.print("🔢 Новый лимит переходов (оставьте пустым для снятия лимита): ");
        String limitInput = scanner.nextLine();
        Integer newMaxAccessCount = null;
        
        if (!limitInput.trim().isEmpty()) {
            try {
                newMaxAccessCount = Integer.parseInt(limitInput);
                if (newMaxAccessCount <= 0) {
                    System.out.println("❌ Лимит должен быть положительным числом");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Неверный формат числа");
                return;
            }
        }
        
        service.updateUrlLimit(shortCode, currentUserId, newMaxAccessCount);
    }
    
    private static void switchUser() {
        currentUserId = null;
        setupUser();
    }
    
    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("❌ Ошибка: введите число");
            }
        }
    }
}