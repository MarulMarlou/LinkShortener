import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LinkShortener {
    private static final String BASE_URL = "http://localhost:8000/";
    private static final int PORT = 8000;
    private static Map<String, String> userSessions = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        LinkDatabase db = new LinkDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new RootHandler(db));
        server.setExecutor(null);
        server.start();

        System.out.println("Сервер запущен на порту " + PORT);

        // удаление просроченных ссылок
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> db.removeExpiredLinks(), 0, 1, TimeUnit.MINUTES);

        while (true) {
            System.out.println("1. Создать короткую ссылку");
            System.out.println("2. Просмотреть все ссылки");
            System.out.println("3. Удалить ссылку");
            System.out.println("4. Выход");
            System.out.print("Выберите действие: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    createShortLink(scanner, db);
                    break;
                case 2:
                    viewAllLinks(db);
                    break;
                case 3:
                    removeLink(scanner, db);
                    break;
                case 4:
                    server.stop(0);
                    System.exit(0);
                    break;
                default:
                    System.out.println("Недопустимый выбор");
            }
        }
    }

    private static void openInBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                System.out.println("Ошибка при открытии ссылки в браузере: " + e.getMessage());
            }
        } else {
            System.out.println("Невозможно открыть ссылку в браузере. Пожалуйста, скопируйте и вставьте вручную.");
        }
    }

    private static void createShortLink(Scanner scanner, LinkDatabase db) {
        System.out.print("Введите длинный URL: ");
        String longUrl = scanner.nextLine();

        int daysToExpire = 1;

        String userId = getUserId(scanner);
        String shortUrl = generateShortUrl();
        db.saveLink(longUrl, shortUrl, userId, 1, daysToExpire);
        System.out.println("Короткая ссылка: " + BASE_URL + shortUrl);

        System.out.print("Открыть ссылку в браузере? (да/нет): ");
        String answer = scanner.nextLine();
        if (answer.equalsIgnoreCase("да")) {
            openInBrowser(BASE_URL + shortUrl);
        }
    }

    private static String getUserId(Scanner scanner) {
        if (userSessions.containsKey("current")) {
            return userSessions.get("current");
        } else {
            String userId = UUID.randomUUID().toString();
            userSessions.put("current", userId);
            System.out.println("Пользователь зарегистрирован с ID: " + userId);
            return userId;
        }
    }

    private static String generateShortUrl() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    private static void viewAllLinks(LinkDatabase db) {
        System.out.println("Список всех ссылок:");
        for (Map.Entry<String, LinkDatabase.LinkInfo> entry : db.links.entrySet()) {
            System.out.println("Короткая ссылка: " + BASE_URL + entry.getKey());
            System.out.println("Длинная ссылка: " + entry.getValue().getLongUrl());
            System.out.println("Лимит переходов: " + entry.getValue().getMaxClicks());
            System.out.println("Срок действия: " + entry.getValue().getExpirationTime());
            if (entry.getValue().getClicks() >= entry.getValue().getMaxClicks()) {
                System.out.println("Статус: Лимит переходов исчерпан");
            } else if (entry.getValue().getExpirationTime().isBefore(Instant.now())) {
                System.out.println("Статус: Срок действия истёк");
            } else {
                System.out.println("Статус: Доступна");
            }
            System.out.println();
        }
    }

    private static void removeLink(Scanner scanner, LinkDatabase db) {
        System.out.print("Введите короткую ссылку для удаления: ");
        String fullShortUrl = scanner.nextLine();
        String shortUrl = fullShortUrl.substring(fullShortUrl.lastIndexOf('/') + 1);

        String userId = getUserId(scanner);
        if (db.isUserOwner(shortUrl, userId)) {
            db.removeLink(shortUrl);
            System.out.println("Ссылка удалена успешно");
        } else {
            System.out.println("Вы не являетесь владельцем этой ссылки");
        }
    }

}
