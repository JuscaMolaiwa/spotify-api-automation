package com.spotify.api.utils;

import com.spotify.api.config.ConfigManager;
import com.spotify.api.config.FrameworkConfig;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Base64;

import static io.restassured.RestAssured.given;

/**
 * Manages Spotify OAuth2 access tokens.
 * Implements client_credentials flow for public endpoints and
 * supports pre-configured user tokens for user-scoped endpoints.
 *
 * Token caching prevents unnecessary re-authentication on every test.
 */
public class TokenManager {

    private static final Logger log = LoggerFactory.getLogger(TokenManager.class);
    private static final FrameworkConfig CONFIG = ConfigManager.getConfig();

    private static String cachedToken;
    private static Instant tokenExpiry;

    private TokenManager() {}

    /**
     * Returns a valid access token. Re-fetches if expired or missing.
     */
    public static synchronized String getAccessToken() {
        if (isTokenValid()) {
            return cachedToken;
        }
        return fetchNewToken();
    }

    private static boolean isTokenValid() {
        return cachedToken != null
                && tokenExpiry != null
                && Instant.now().isBefore(tokenExpiry.minusSeconds(60));
    }

    private static String fetchNewToken() {
        log.info("Fetching new Spotify access token via client_credentials flow...");

        String credentials = CONFIG.clientId() + ":" + CONFIG.clientSecret();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        Response response = given()
                .baseUri(CONFIG.accountsBaseUri())
                .contentType("application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + encodedCredentials)
                .formParam("grant_type", CONFIG.grantType())
                .when()
                .post("/api/token")
                .then()
                .statusCode(200)
                .extract()
                .response();

        cachedToken = response.jsonPath().getString("access_token");
        int expiresIn = response.jsonPath().getInt("expires_in");
        tokenExpiry = Instant.now().plusSeconds(expiresIn);

        log.info("Token obtained successfully. Expires in {} seconds.", expiresIn);
        return cachedToken;
    }

    /**
     * For user-scoped endpoints (playlists, player, library, etc.)
     * a user access token must be pre-generated and set in config.
     */
    public static String getUserAccessToken() {
        String userToken = CONFIG.userAccessToken();
        if (userToken == null || userToken.isBlank()) {
            throw new IllegalStateException(
                "USER_ACCESS_TOKEN is not configured. " +
                "Generate one via the Authorization Code flow and set it in config.properties or as an env variable.");
        }
        return userToken;
    }

    /** Force token refresh (useful for negative testing) */
    public static synchronized void invalidateToken() {
        cachedToken = null;
        tokenExpiry = null;
        log.info("Token cache invalidated.");
    }
}
