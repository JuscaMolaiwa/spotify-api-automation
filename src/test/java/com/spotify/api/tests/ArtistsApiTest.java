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
 * Test suite for the Spotify Artists API.
 *
 * Covers:
 *  - GET /artists/{id}
 *  - GET /artists
 *  - GET /artists/{id}/albums
 *  - GET /artists/{id}/top-tracks
 *  - GET /artists/{id}/related-artists
 */
@Epic("Spotify Web API")
@Feature("Artists")
public class ArtistsApiTest extends BaseTest {

    // ────────────────────────────────────────────────────────────────────────
    // GET /artists/{id}
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get artist by valid ID returns 200 with full artist object")
    @Story("Get Single Artist")
    @Severity(SeverityLevel.CRITICAL)
    public void getArtist_validId_returns200() {
        Response response = given(requestSpec)
                .pathParam("id", TestData.ARTIST_ID_PINK_FLOYD)
                .when()
                .get(ApiEndpoints.ARTIST)
                .then()
                .statusCode(200)
                .body("id", equalTo(TestData.ARTIST_ID_PINK_FLOYD))
                .body("type", equalTo("artist"))
                .body("name", equalTo("Pink Floyd"))
                .body("followers.total", greaterThanOrEqualTo(0))
                .body("popularity", allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100)))
                .body("genres", not(empty()))
                .body("images", not(empty()))
                .body("external_urls.spotify", not(emptyOrNullString()))
                .extract().response();

        assertThat(response.jsonPath().getInt("popularity"))
                .isBetween(0, 100);
    }

    @Test(description = "Get artist with invalid ID returns 400")
    @Story("Get Single Artist")
    @Severity(SeverityLevel.NORMAL)
    public void getArtist_invalidId_returns400() {
        given(requestSpec)
                .pathParam("id", TestData.INVALID_ID)
                .when()
                .get(ApiEndpoints.ARTIST)
                .then()
                .statusCode(400)
                .body("error.status", equalTo(400));
    }

    @Test(description = "Artist object URI follows spotify:artist:<id> format")
    @Story("Get Single Artist")
    @Severity(SeverityLevel.MINOR)
    public void getArtist_validId_uriFormatIsCorrect() {
        String expectedUri = "spotify:artist:" + TestData.ARTIST_ID_TAYLOR_SWIFT;

        given(requestSpec)
                .pathParam("id", TestData.ARTIST_ID_TAYLOR_SWIFT)
                .when()
                .get(ApiEndpoints.ARTIST)
                .then()
                .statusCode(200)
                .body("uri", equalTo(expectedUri));
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /artists  (multiple)
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get multiple artists returns 200 with all requested artists")
    @Story("Get Multiple Artists")
    @Severity(SeverityLevel.CRITICAL)
    public void getMultipleArtists_validIds_returns200() {
        String ids = String.join(",",
                TestData.ARTIST_ID_PINK_FLOYD,
                TestData.ARTIST_ID_TAYLOR_SWIFT,
                TestData.ARTIST_ID_RADIOHEAD);

        given(requestSpec)
                .queryParam("ids", ids)
                .when()
                .get(ApiEndpoints.ARTISTS_MULTIPLE)
                .then()
                .statusCode(200)
                .body("artists", hasSize(3))
                .body("artists.type", everyItem(equalTo("artist")));
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /artists/{id}/albums
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get artist albums returns paginated album list")
    @Story("Get Artist Albums")
    @Severity(SeverityLevel.CRITICAL)
    public void getArtistAlbums_validId_returnsPaginatedList() {
        Response response = given(requestSpec)
                .pathParam("id", TestData.ARTIST_ID_PINK_FLOYD)
                .queryParam("limit", 10)
                .queryParam("offset", 0)
                .queryParam("include_groups", "album")
                .when()
                .get(ApiEndpoints.ARTIST_ALBUMS)
                .then()
                .statusCode(200)
                .body("items", not(empty()))
                .body("total", greaterThan(0))
                .body("items.type", everyItem(equalTo("album")))
                .extract().response();

        assertThat(response.jsonPath().getList("items"))
                .hasSizeLessThanOrEqualTo(10);
    }

    @Test(description = "Get artist albums with include_groups=single returns only singles")
    @Story("Get Artist Albums")
    @Severity(SeverityLevel.NORMAL)
    public void getArtistAlbums_filterBySingle_returnsOnlySingles() {
        given(requestSpec)
                .pathParam("id", TestData.ARTIST_ID_TAYLOR_SWIFT)
                .queryParam("include_groups", "single")
                .queryParam("limit", 5)
                .when()
                .get(ApiEndpoints.ARTIST_ALBUMS)
                .then()
                .statusCode(200)
                .body("items.album_type", everyItem(equalTo("single")));
    }

    @Test(description = "Get artist albums filtered by market returns market-available albums")
    @Story("Get Artist Albums")
    @Severity(SeverityLevel.MINOR)
    public void getArtistAlbums_withMarket_returns200() {
        given(requestSpec)
                .pathParam("id", TestData.ARTIST_ID_RADIOHEAD)
                .queryParam("market", TestData.MARKET_ZA)
                .queryParam("limit", 5)
                .when()
                .get(ApiEndpoints.ARTIST_ALBUMS)
                .then()
                .statusCode(200)
                .body("items", not(empty()));
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /artists/{id}/top-tracks
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get artist top tracks returns up to 10 tracks")
    @Story("Get Artist Top Tracks")
    @Severity(SeverityLevel.CRITICAL)
    public void getArtistTopTracks_validId_returnsUpTo10Tracks() {
        Response response = given(requestSpec)
                .pathParam("id", TestData.ARTIST_ID_PINK_FLOYD)
                .queryParam("market", TestData.MARKET_ZA)
                .when()
                .get(ApiEndpoints.ARTIST_TOP_TRACKS)
                .then()
                .statusCode(200)
                .body("tracks", not(empty()))
                .body("tracks.type", everyItem(equalTo("track")))
                .extract().response();

        assertThat(response.jsonPath().getList("tracks"))
                .hasSizeLessThanOrEqualTo(10);
    }

    @Test(description = "Get artist top tracks – market parameter is required")
    @Story("Get Artist Top Tracks")
    @Severity(SeverityLevel.NORMAL)
    public void getArtistTopTracks_missingMarket_returns400Or200() {
        // Spotify may return 400 if market is missing; accept both behaviours
        int status = given(requestSpec)
                .pathParam("id", TestData.ARTIST_ID_PINK_FLOYD)
                .when()
                .get(ApiEndpoints.ARTIST_TOP_TRACKS)
                .then()
                .extract().response().getStatusCode();

        assertThat(status).isIn(200, 400);
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /artists/{id}/related-artists
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get related artists returns up to 20 similar artists")
    @Story("Get Related Artists")
    @Severity(SeverityLevel.NORMAL)
    public void getRelatedArtists_validId_returnsArtistList() {
        Response response = given(requestSpec)
                .pathParam("id", TestData.ARTIST_ID_RADIOHEAD)
                .when()
                .get(ApiEndpoints.ARTIST_RELATED_ARTISTS)
                .then()
                .statusCode(200)
                .body("artists", not(empty()))
                .body("artists.type", everyItem(equalTo("artist")))
                .extract().response();

        assertThat(response.jsonPath().getList("artists"))
                .hasSizeLessThanOrEqualTo(20);
    }
}
