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
 * Test suite for Spotify Tracks and Audio Features/Analysis endpoints.
 *
 * Covers:
 *   - GET /tracks/{id}
 *   - GET /tracks
 *   - GET /audio-features/{id}
 *   - GET /audio-features
 *   - GET /audio-analysis/{id}
 */
@Epic("Spotify Web API")
@Feature("Tracks")
public class TracksApiTest extends BaseTest {

    // ────────────────────────────────────────────────────────────────────────
    // GET /tracks/{id}
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get track by valid ID returns 200 with full track object")
    @Story("Get Single Track")
    @Severity(SeverityLevel.CRITICAL)
    public void getTrack_validId_returns200WithFullObject() {
        Response response = given(requestSpec)
                .pathParam("id", TestData.TRACK_ID_MONEY)
                .queryParam("market", TestData.MARKET_ZA)
                .when()
                .get(ApiEndpoints.TRACK)
                .then()
                .statusCode(200)
                .body("id",           equalTo(TestData.TRACK_ID_MONEY))
                .body("type",         equalTo("track"))
                .body("name",         not(emptyOrNullString()))
                .body("duration_ms",  greaterThan(0))
                .body("artists",      not(empty()))
                .body("album",        notNullValue())
                .body("track_number", greaterThan(0))
                .body("uri",          startsWith("spotify:track:"))
                .extract().response();

        assertThat(response.jsonPath().getBoolean("is_local")).isFalse();
        assertThat(response.jsonPath().getInt("disc_number")).isGreaterThan(0);
    }

    @Test(description = "Get track with invalid ID returns 400")
    @Story("Get Single Track")
    @Severity(SeverityLevel.NORMAL)
    public void getTrack_invalidId_returns400() {
        given(requestSpec)
                .pathParam("id", TestData.INVALID_ID)
                .when()
                .get(ApiEndpoints.TRACK)
                .then()
                .statusCode(400)
                .body("error.status", equalTo(400));
    }

    @Test(description = "Get track – popularity is a value between 0 and 100")
    @Story("Get Single Track")
    @Severity(SeverityLevel.MINOR)
    public void getTrack_popularityIsInValidRange() {
        int popularity = given(requestSpec)
                .pathParam("id", TestData.TRACK_ID_BOHEMIAN)
                .when()
                .get(ApiEndpoints.TRACK)
                .then()
                .statusCode(200)
                .extract().jsonPath().getInt("popularity");

        assertThat(popularity).isBetween(0, 100);
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /tracks  (multiple tracks)
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get multiple tracks with valid IDs returns 200")
    @Story("Get Multiple Tracks")
    @Severity(SeverityLevel.CRITICAL)
    public void getMultipleTracks_validIds_returns200() {
        String ids = String.join(",",
                TestData.TRACK_ID_MONEY,
                TestData.TRACK_ID_BOHEMIAN,
                TestData.TRACK_ID_STAIRWAY);

        given(requestSpec)
                .queryParam("ids", ids)
                .when()
                .get(ApiEndpoints.TRACKS_MULTIPLE)
                .then()
                .statusCode(200)
                .body("tracks", hasSize(3))
                .body("tracks.type", everyItem(equalTo("track")));
    }

    @Test(description = "Get multiple tracks – nonexistent ID returned as null in array")
    @Story("Get Multiple Tracks")
    @Severity(SeverityLevel.MINOR)
    public void getMultipleTracks_nonexistentId_returnsNullInArray() {
        String ids = TestData.TRACK_ID_MONEY + "," + TestData.NONEXISTENT_ID;

        Response response = given(requestSpec)
                .queryParam("ids", ids)
                .when()
                .get(ApiEndpoints.TRACKS_MULTIPLE)
                .then()
                .statusCode(200)
                .extract().response();

        // First track should be present; second (nonexistent) should be null
        assertThat(response.jsonPath().getString("tracks[0].id"))
                .isEqualTo(TestData.TRACK_ID_MONEY);
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /audio-features/{id}
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get audio features returns 200 with all feature fields")
    @Story("Audio Features")
    @Severity(SeverityLevel.CRITICAL)
    public void getAudioFeatures_validId_returns200WithAllFields() {
        Response response = given(requestSpec)
                .pathParam("id", TestData.TRACK_ID_MONEY)
                .when()
                .get(ApiEndpoints.TRACK_FEATURES)
                .then()
                .statusCode(200)
                .body("id",             equalTo(TestData.TRACK_ID_MONEY))
                .body("type",           equalTo("audio_features"))
                .body("danceability",   notNullValue())
                .body("energy",         notNullValue())
                .body("key",            notNullValue())
                .body("loudness",       notNullValue())
                .body("mode",           notNullValue())
                .body("speechiness",    notNullValue())
                .body("acousticness",   notNullValue())
                .body("instrumentalness", notNullValue())
                .body("liveness",       notNullValue())
                .body("valence",        notNullValue())
                .body("tempo",          notNullValue())
                .body("duration_ms",    greaterThan(0))
                .body("time_signature", notNullValue())
                .extract().response();

        // All coefficient features must be between 0.0 and 1.0
        float danceability = response.jsonPath().getFloat("danceability");
        float energy       = response.jsonPath().getFloat("energy");
        float valence      = response.jsonPath().getFloat("valence");

        assertThat(danceability).isBetween(0f, 1f);
        assertThat(energy).isBetween(0f, 1f);
        assertThat(valence).isBetween(0f, 1f);
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /audio-features  (multiple)
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get audio features for multiple tracks returns 200")
    @Story("Audio Features")
    @Severity(SeverityLevel.NORMAL)
    public void getAudioFeaturesMultiple_validIds_returns200() {
        String ids = String.join(",",
                TestData.TRACK_ID_MONEY,
                TestData.TRACK_ID_BOHEMIAN,
                TestData.TRACK_ID_STAIRWAY);

        given(requestSpec)
                .queryParam("ids", ids)
                .when()
                .get(ApiEndpoints.TRACKS_FEATURES)
                .then()
                .statusCode(200)
                .body("audio_features", hasSize(3))
                .body("audio_features.type", everyItem(equalTo("audio_features")));
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /audio-analysis/{id}
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get audio analysis returns 200 with segments and beats")
    @Story("Audio Analysis")
    @Severity(SeverityLevel.NORMAL)
    public void getAudioAnalysis_validId_returns200WithAnalysisData() {
        given(requestSpec)
                .pathParam("id", TestData.TRACK_ID_MONEY)
                .when()
                .get(ApiEndpoints.TRACK_ANALYSIS)
                .then()
                .statusCode(200)
                .body("meta",     notNullValue())
                .body("track",    notNullValue())
                .body("bars",     not(empty()))
                .body("beats",    not(empty()))
                .body("sections", not(empty()))
                .body("segments", not(empty()))
                .body("tatums",   not(empty()));
    }
}
