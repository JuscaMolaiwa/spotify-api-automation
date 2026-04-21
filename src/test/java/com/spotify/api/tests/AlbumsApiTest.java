package com.spotify.api.tests;

import com.spotify.api.base.BaseTest;
import com.spotify.api.utils.ApiEndpoints;
import com.spotify.api.utils.TestData;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test suite covering the Spotify Albums API endpoints.
 *
 * Covers:
 *  - GET /albums/{id}           – single album retrieval
 *  - GET /albums                – multiple albums in one request
 *  - GET /albums/{id}/tracks    – album track listing
 *
 * Includes positive, negative, and boundary/edge-case scenarios.
 */
@Epic("Spotify Web API")
@Feature("Albums")
public class AlbumsApiTest extends BaseTest {

    // ────────────────────────────────────────────────────────────────────────
    // GET /albums/{id}
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get album by valid ID returns 200 with correct album data")
    @Story("Get Single Album")
    @Severity(SeverityLevel.CRITICAL)
    public void getAlbum_validId_returns200WithAlbumData() {
        Response response = given(requestSpec)
                .pathParam("id", TestData.ALBUM_ID_DARK_SIDE)
                .queryParam("market", TestData.MARKET_ZA)
                .when()
                .get(ApiEndpoints.ALBUM)
                .then()
                .statusCode(200)
                .body("id", equalTo(TestData.ALBUM_ID_DARK_SIDE))
                .body("type", equalTo("album"))
                .body("name", not(emptyOrNullString()))
                .body("artists", not(empty()))
                .body("tracks.items", not(empty()))
                .body("release_date", not(emptyOrNullString()))
                .body("images", not(empty()))
                .extract().response();

        assertThat(response.jsonPath().getString("name"))
                .isEqualTo("The Dark Side of the Moon");
        assertThat(response.jsonPath().getInt("total_tracks"))
                .isGreaterThan(0);

        log.info("GET /albums/{} → 200 OK | Album: '{}'",
                TestData.ALBUM_ID_DARK_SIDE, response.jsonPath().getString("name"));
    }

    @Test(description = "Get album with invalid ID returns 400 Bad Request")
    @Story("Get Single Album")
    @Severity(SeverityLevel.NORMAL)
    public void getAlbum_invalidId_returns400() {
        given(requestSpec)
                .pathParam("id", TestData.INVALID_ID)
                .when()
                .get(ApiEndpoints.ALBUM)
                .then()
                .statusCode(400)
                .body("error.status", equalTo(400))
                .body("error.message", not(emptyOrNullString()));

        log.info("GET /albums/{} → 400 Bad Request (expected)", TestData.INVALID_ID);
    }

    @Test(description = "Get album without auth token returns 401 Unauthorized")
    @Story("Get Single Album")
    @Severity(SeverityLevel.CRITICAL)
    public void getAlbum_noToken_returns401() {
        given()
                .baseUri(config.baseUri())
                .basePath(config.basePath())
                .pathParam("id", TestData.ALBUM_ID_DARK_SIDE)
                .when()
                .get(ApiEndpoints.ALBUM)
                .then()
                .statusCode(401)
                .body("error.status", equalTo(401));

        log.info("GET /albums/{} with no token → 401 Unauthorized (expected)",
                TestData.ALBUM_ID_DARK_SIDE);
    }

    @Test(description = "Get album with nonexistent ID returns 404 Not Found")
    @Story("Get Single Album")
    @Severity(SeverityLevel.NORMAL)
    public void getAlbum_nonexistentId_returns404() {
        given(requestSpec)
                .pathParam("id", TestData.NONEXISTENT_ID)
                .when()
                .get(ApiEndpoints.ALBUM)
                .then()
                .statusCode(404)
                .body("error.status", equalTo(404));
    }

    @Test(description = "Get album response contains required HREF and external URLs")
    @Story("Get Single Album")
    @Severity(SeverityLevel.MINOR)
    public void getAlbum_validId_containsLinks() {
        given(requestSpec)
                .pathParam("id", TestData.ALBUM_ID_THRILLER)
                .when()
                .get(ApiEndpoints.ALBUM)
                .then()
                .statusCode(200)
                .body("href", not(emptyOrNullString()))
                .body("external_urls.spotify", not(emptyOrNullString()))
                .body("uri", startsWith("spotify:album:"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /albums  (multiple albums)
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get multiple albums with valid IDs returns 200")
    @Story("Get Multiple Albums")
    @Severity(SeverityLevel.CRITICAL)
    public void getMultipleAlbums_validIds_returns200() {
        String ids = String.join(",",
                TestData.ALBUM_ID_DARK_SIDE,
                TestData.ALBUM_ID_THRILLER,
                TestData.ALBUM_ID_ABBEY_ROAD);

        Response response = given(requestSpec)
                .queryParam("ids", ids)
                .when()
                .get(ApiEndpoints.ALBUMS_MULTIPLE)
                .then()
                .statusCode(200)
                .body("albums", hasSize(3))
                .body("albums[0].id", equalTo(TestData.ALBUM_ID_DARK_SIDE))
                .extract().response();

        assertThat(response.jsonPath().getList("albums.id"))
                .containsExactlyInAnyOrder(
                        TestData.ALBUM_ID_DARK_SIDE,
                        TestData.ALBUM_ID_THRILLER,
                        TestData.ALBUM_ID_ABBEY_ROAD);
    }

    @Test(description = "Get multiple albums with no IDs returns 400")
    @Story("Get Multiple Albums")
    @Severity(SeverityLevel.NORMAL)
    public void getMultipleAlbums_noIds_returns400() {
        given(requestSpec)
                .when()
                .get(ApiEndpoints.ALBUMS_MULTIPLE)
                .then()
                .statusCode(400)
                .body("error.status", equalTo(400));
    }

    @Test(description = "Get multiple albums – max 20 IDs enforced")
    @Story("Get Multiple Albums")
    @Severity(SeverityLevel.MINOR)
    public void getMultipleAlbums_exceedsMaxIds_returns400() {
        // Build a comma-separated list of 21 (duplicate) IDs
        String ids = (TestData.ALBUM_ID_DARK_SIDE + ",").repeat(21).replaceAll(",$", "");

        given(requestSpec)
                .queryParam("ids", ids)
                .when()
                .get(ApiEndpoints.ALBUMS_MULTIPLE)
                .then()
                .statusCode(400);
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /albums/{id}/tracks
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get album tracks returns 200 with paginated track list")
    @Story("Get Album Tracks")
    @Severity(SeverityLevel.CRITICAL)
    public void getAlbumTracks_validId_returns200WithTracks() {
        Response response = given(requestSpec)
                .pathParam("id", TestData.ALBUM_ID_DARK_SIDE)
                .queryParam("limit", 5)
                .queryParam("offset", 0)
                .when()
                .get(ApiEndpoints.ALBUM_TRACKS)
                .then()
                .statusCode(200)
                .body("items", not(empty()))
                .body("total", greaterThan(0))
                .body("limit", equalTo(5))
                .body("offset", equalTo(0))
                .extract().response();

        assertThat(response.jsonPath().getList("items"))
                .hasSizeLessThanOrEqualTo(5);
        assertThat(response.jsonPath().getInt("total"))
                .isGreaterThan(0);
    }

    @Test(description = "Get album tracks – pagination offset works correctly")
    @Story("Get Album Tracks")
    @Severity(SeverityLevel.NORMAL)
    public void getAlbumTracks_withOffset_returnsDifferentPage() {
        Response firstPage = given(requestSpec)
                .pathParam("id", TestData.ALBUM_ID_DARK_SIDE)
                .queryParam("limit", 3)
                .queryParam("offset", 0)
                .when()
                .get(ApiEndpoints.ALBUM_TRACKS)
                .then()
                .statusCode(200)
                .extract().response();

        Response secondPage = given(requestSpec)
                .pathParam("id", TestData.ALBUM_ID_DARK_SIDE)
                .queryParam("limit", 3)
                .queryParam("offset", 3)
                .when()
                .get(ApiEndpoints.ALBUM_TRACKS)
                .then()
                .statusCode(200)
                .extract().response();

        String firstTrackId  = firstPage.jsonPath().getString("items[0].id");
        String secondTrackId = secondPage.jsonPath().getString("items[0].id");

        assertThat(firstTrackId).isNotEqualTo(secondTrackId);
    }

    @Test(description = "Get album tracks with limit=0 returns 400 or empty items")
    @Story("Get Album Tracks")
    @Severity(SeverityLevel.MINOR)
    public void getAlbumTracks_limitZero_handledGracefully() {
        int statusCode = given(requestSpec)
                .pathParam("id", TestData.ALBUM_ID_DARK_SIDE)
                .queryParam("limit", 0)
                .when()
                .get(ApiEndpoints.ALBUM_TRACKS)
                .then()
                .extract().response().getStatusCode();

        assertThat(statusCode).isIn(200, 400);
    }
}
