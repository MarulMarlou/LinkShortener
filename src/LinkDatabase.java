import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

public class LinkDatabase {
    public Map<String, LinkInfo> links = new HashMap<>();
    public Map<String, String> userLinks = new HashMap<>();

    public void saveLink(String longUrl, String shortUrl, String userId, int maxClicks, int daysToExpire) {
        Instant expirationTime = Instant.now().plusSeconds(daysToExpire * 24 * 60 * 60);
        links.put(shortUrl, new LinkInfo(longUrl, maxClicks, expirationTime, userId));
        userLinks.put(shortUrl, userId);
    }

    public String getLongUrl(String shortUrl) {
        LinkInfo info = links.get(shortUrl);
        if (info != null) {
            return info.getLongUrl();
        }
        return null;
    }

    public boolean isShortUrlExists(String shortUrl) {
        return links.containsKey(shortUrl);
    }

    public boolean isLinkAvailable(String shortUrl) {
        LinkInfo info = links.get(shortUrl);
        if (info != null) {
            return info.getExpirationTime().isAfter(Instant.now()) && info.getClicks() < info.getMaxClicks();
        }
        return false;
    }

    public boolean isUserOwner(String shortUrl, String userId) {
        String ownerId = userLinks.get(shortUrl);
        return ownerId != null && ownerId.equals(userId);
    }


    public void incrementClicks(String shortUrl) {
        LinkInfo info = links.get(shortUrl);
        if (info != null) {
            info.incrementClicks();
        }
    }

    public void removeExpiredLinks() {
        Instant now = Instant.now();
        links.entrySet().removeIf(entry -> entry.getValue().getExpirationTime().isBefore(now));
    }

    public void removeLink(String shortUrl) {
        links.remove(shortUrl);
        userLinks.remove(shortUrl);
    }

    public static class LinkInfo {
        private String longUrl;
        private int maxClicks;
        private Instant expirationTime;
        private String userId;
        private int clicks;

        public LinkInfo(String longUrl, int maxClicks, Instant expirationTime, String userId) {
            this.longUrl = longUrl;
            this.maxClicks = maxClicks;
            this.expirationTime = expirationTime;
            this.userId = userId;
            this.clicks = 0;
        }

        public String getLongUrl() {
            return longUrl;
        }

        public int getMaxClicks() {
            return maxClicks;
        }

        public Instant getExpirationTime() {
            return expirationTime;
        }

        public String getUserId() {
            return userId;
        }

        public int getClicks() {
            return clicks;
        }

        public void incrementClicks() {
            clicks++;
        }
    }
}
