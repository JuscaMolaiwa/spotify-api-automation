package com.spotify.api.tests;

import com.spotify.api.base.BaseTest;
import com.spotify.api.utils.TestData;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Tests for the Spotify Accounts Service OAuth2 token endpoint.
 *
 * Verifies:
 *   - Successful token generation (client_credentials)
 *   - Invalid credentials handling
 *   - Missing grant_type handling
 *   - Token response schema validation
 */
@Epic("Spotify Web API")
@Feature("Authentication")
public class AuthenticationTest extends BaseTest {

    private static final String TOKEN_ENDPOINT = "/api/token";

    @Test(description = "Client credentials flow returns 200 with access_token")
    @Story("OAuth2 Client Credentials")
    @Severity(SeverityLevel.BLOCKER)
    public void clientCredentials_validCredentials_returnsAccessToken() {
        String credentials = config.clientId() + ":" + config.clientSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        var response = given(accountsRequestSpec)
                .header("Authorization", "Basic " + encoded)
                .formParam("grant_type", "client_credentials")
                .when()
                .post(TOKEN_ENDPOINT)
                .then()
                .statusCode(200)
                .body("access_token",  not(emptyOrNullString()))
                .body("token_type",    equalTo("Bearer"))
                .body("expires_in",    greaterThan(0))
                .extract().response();

        assertThat(response.jsonPath().getString("access_token")).isNotBlank();
        assertThat(response.jsonPath().getInt("expires_in")).isEqualTo(3600);
    }

    @Test(description = "Invalid client credentials return 400 Bad Request")
    @Story("OAuth2 Client Credentials")
    @Severity(SeverityLevel.CRITICAL)
    public void clientCredentials_invalidCredentials_returns400() {
        String invalidCredentials = "invalid_client_id:invalid_client_secret";
        String encoded = Base64.getEncoder().encodeToString(invalidCredentials.getBytes());

        given(accountsRequestSpec)
                .header("Authorization", "Basic " + encoded)
                .formParam("grant_type", "client_credentials")
                .when()
                .post(TOKEN_ENDPOINT)
                .then()
                .statusCode(400)
                .body("error", not(emptyOrNullString()));
    }

    @Test(description = "Missing grant_type returns 400 Bad Request")
    @Story("OAuth2 Client Credentials")
    @Severity(SeverityLevel.NORMAL)
    public void clientCredentials_missingGrantType_returns400() {
        String credentials = config.clientId() + ":" + config.clientSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        given(accountsRequestSpec)
                .header("Authorization", "Basic " + encoded)
                .when()
                .post(TOKEN_ENDPOINT)
                .then()
                .statusCode(400)
                .body("error", not(emptyOrNullString()));
    }

    @Test(description = "Invalid grant_type value returns 400 Bad Request")
    @Story("OAuth2 Client Credentials")
    @Severity(SeverityLevel.NORMAL)
    public void clientCredentials_invalidGrantType_returns400() {
        String credentials = config.clientId() + ":" + config.clientSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        given(accountsRequestSpec)
                .header("Authorization", "Basic " + encoded)
                .formParam("grant_type", "password")   // not supported for this flow
                .when()
                .post(TOKEN_ENDPOINT)
                .then()
                .statusCode(400);
    }

    @Test(description = "Calling API with expired/invalid token returns 401")
    @Story("Token Validation")
    @Severity(SeverityLevel.CRITICAL)
    public void apiCall_expiredToken_returns401() {
        given()
                .baseUri(config.baseUri())
                .basePath(config.basePath())
                .header("Authorization", "Bearer INVALID_OR_EXPIRED_TOKEN_12345")
                .pathParam("id", TestData.ALBUM_ID_DARK_SIDE)
                .when()
                .get("/albums/{id}")
                .then()
                .statusCode(401)
                .body("error.status",  equalTo(401))
                .body("error.message", not(emptyOrNullString()));
    }
}
