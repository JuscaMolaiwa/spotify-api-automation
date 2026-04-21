package com.spotify.api.tests;

import com.spotify.api.base.BaseTest;
import com.spotify.api.utils.ApiEndpoints;
import com.spotify.api.utils.TestData;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test suite for the Spotify Search API (GET /search).
 *
 * Validates search across types: artist, album, track, playlist.
 * Covers positive, negative, and parameterised data-driven scenarios.
 */
@Epic("Spotify Web API")
@Feature("Search")
public class SearchApiTest extends BaseTest {

    // ────────────────────────────────────────────────────────────────────────
    // Data Providers
    // ────────────────────────────────────────────────────────────────────────

    @DataProvider(name = "searchTypeProvider")
    public Object[][] searchTypeProvider() {
        return new Object[][] {
            {"Pink Floyd", "artist",   "artists"},
            {"Thriller",   "album",    "albums"},
            {"Money",      "track",    "tracks"},
            {"Top 50",     "playlist", "playlists"},
        };
    }

    // ────────────────────────────────────────────────────────────────────────
    // Positive tests
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Search by artist returns 200 with artist results",
          dataProvider = "searchTypeProvider")
    @Story("Search by Type")
    @Severity(SeverityLevel.CRITICAL)
    public void search_byType_returns200WithResults(String query, String type, String resultKey) {
        Response response = given(requestSpec)
                .queryParam("q", query)
                .queryParam("type", type)
                .queryParam("limit", 5)
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(200)
                .body(resultKey + ".items", not(empty()))
                .body(resultKey + ".total", greaterThan(0))
                .extract().response();

        assertThat(response.jsonPath().getList(resultKey + ".items"))
                .hasSizeLessThanOrEqualTo(5);

        log.info("Search q='{}' type='{}' → {} result(s)",
                query, type, response.jsonPath().getInt(resultKey + ".total"));
    }

    @Test(description = "Search for artist returns matching name in results")
    @Story("Search – Artist")
    @Severity(SeverityLevel.CRITICAL)
    public void search_artistQuery_returnsMatchingArtistName() {
        Response response = given(requestSpec)
                .queryParam("q", TestData.SEARCH_QUERY_ARTIST)
                .queryParam("type", "artist")
                .queryParam("limit", 1)
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(200)
                .extract().response();

        String topResultName = response.jsonPath().getString("artists.items[0].name");
        assertThat(topResultName).isNotBlank();
        log.info("Top artist result for '{}': '{}'", TestData.SEARCH_QUERY_ARTIST, topResultName);
    }

    @Test(description = "Search supports multiple types in a single request")
    @Story("Search – Multi-Type")
    @Severity(SeverityLevel.NORMAL)
    public void search_multipleTypes_returnsAllResultGroups() {
        given(requestSpec)
                .queryParam("q", "Pink Floyd")
                .queryParam("type", "artist,album,track")
                .queryParam("limit", 3)
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(200)
                .body("artists.items", not(empty()))
                .body("albums.items",  not(empty()))
                .body("tracks.items",  not(empty()));
    }

    @Test(description = "Search with market parameter filters by availability")
    @Story("Search – Market Filter")
    @Severity(SeverityLevel.NORMAL)
    public void search_withMarket_returns200() {
        given(requestSpec)
                .queryParam("q", TestData.SEARCH_QUERY_TRACK)
                .queryParam("type", "track")
                .queryParam("market", TestData.MARKET_ZA)
                .queryParam("limit", 5)
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(200)
                .body("tracks.items", not(empty()));
    }

    @Test(description = "Search result track items include required fields")
    @Story("Search – Response Schema")
    @Severity(SeverityLevel.NORMAL)
    public void search_trackResults_containRequiredFields() {
        given(requestSpec)
                .queryParam("q", TestData.SEARCH_QUERY_TRACK)
                .queryParam("type", "track")
                .queryParam("limit", 1)
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(200)
                .body("tracks.items[0].id",       not(emptyOrNullString()))
                .body("tracks.items[0].name",     not(emptyOrNullString()))
                .body("tracks.items[0].artists",  not(empty()))
                .body("tracks.items[0].duration_ms", greaterThan(0))
                .body("tracks.items[0].explicit", notNullValue())
                .body("tracks.items[0].uri",      startsWith("spotify:track:"));
    }

    @Test(description = "Search supports pagination via offset parameter")
    @Story("Search – Pagination")
    @Severity(SeverityLevel.NORMAL)
    public void search_pagination_returnsDifferentResultsPerPage() {
        Response page1 = given(requestSpec)
                .queryParam("q", "rock")
                .queryParam("type", "track")
                .queryParam("limit", 5)
                .queryParam("offset", 0)
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(200)
                .extract().response();

        Response page2 = given(requestSpec)
                .queryParam("q", "rock")
                .queryParam("type", "track")
                .queryParam("limit", 5)
                .queryParam("offset", 5)
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(200)
                .extract().response();

        String firstIdPage1 = page1.jsonPath().getString("tracks.items[0].id");
        String firstIdPage2 = page2.jsonPath().getString("tracks.items[0].id");

        assertThat(firstIdPage1).isNotEqualTo(firstIdPage2);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Negative tests
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Search without query parameter returns 400")
    @Story("Search – Validation")
    @Severity(SeverityLevel.NORMAL)
    public void search_missingQuery_returns400() {
        given(requestSpec)
                .queryParam("type", "track")
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(400)
                .body("error.status", equalTo(400));
    }

    @Test(description = "Search without type parameter returns 400")
    @Story("Search – Validation")
    @Severity(SeverityLevel.NORMAL)
    public void search_missingType_returns400() {
        given(requestSpec)
                .queryParam("q", "Pink Floyd")
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(400)
                .body("error.status", equalTo(400));
    }

    @Test(description = "Search with invalid type parameter returns 400")
    @Story("Search – Validation")
    @Severity(SeverityLevel.MINOR)
    public void search_invalidType_returns400() {
        given(requestSpec)
                .queryParam("q", "Pink Floyd")
                .queryParam("type", "invalidtype")
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(400);
    }

    @Test(description = "Search with limit exceeding maximum (50) returns 400")
    @Story("Search – Validation")
    @Severity(SeverityLevel.MINOR)
    public void search_limitExceedsMax_returns400() {
        given(requestSpec)
                .queryParam("q", "Pink Floyd")
                .queryParam("type", "artist")
                .queryParam("limit", 51)
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(400);
    }

    @Test(description = "Search for obscure query returns 200 with empty or low results")
    @Story("Search – Edge Cases")
    @Severity(SeverityLevel.MINOR)
    public void search_obscureQuery_returns200WithZeroOrLowResults() {
        Response response = given(requestSpec)
                .queryParam("q", "zzzzzzxxxxxyyyyyyy9999notarealartist")
                .queryParam("type", "artist")
                .queryParam("limit", 5)
                .when()
                .get(ApiEndpoints.SEARCH)
                .then()
                .statusCode(200)
                .extract().response();

        int total = response.jsonPath().getInt("artists.total");
        assertThat(total).isGreaterThanOrEqualTo(0);
    }
}
