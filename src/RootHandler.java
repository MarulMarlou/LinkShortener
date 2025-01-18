import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RootHandler implements HttpHandler {
    private LinkDatabase db;

    public RootHandler(LinkDatabase db) {
        this.db = db;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String shortUrl = exchange.getRequestURI().getPath().substring(1);
        String longUrl = db.getLongUrl(shortUrl);

        if (longUrl != null) {
            if (db.isLinkAvailable(shortUrl)) {
                db.incrementClicks(shortUrl);
                exchange.getResponseHeaders().set("Location", longUrl);
                exchange.sendResponseHeaders(302, -1);
            } else {
                String response;
                LinkDatabase.LinkInfo info = db.links.get(shortUrl);
                if (info.getClicks() >= info.getMaxClicks()) {
                    response = "Ссылка недоступна. Лимит переходов исчерпан.";
                } else {
                    response = "Ссылка недоступна. Срок действия истёк.";
                }
                exchange.sendResponseHeaders(403, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            }
        } else {
            String response = "Ссылка не найдена.";
            exchange.sendResponseHeaders(404, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        }
    }
}
