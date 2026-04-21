package com.spotify.api.tests;

import com.spotify.api.base.BaseTest;
import com.spotify.api.utils.ApiEndpoints;
import com.spotify.api.utils.TestData;
import com.spotify.api.utils.TokenManager;
import io.qameta.allure.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test suite for the Spotify Playlists API.
 *
 * NOTE: These tests require a USER access token with scopes:
 *   - playlist-read-public
 *   - playlist-read-collaborative
 *   - playlist-modify-public   (for POST / PUT / DELETE tests)
 *
 * Set USER_ACCESS_TOKEN in config.properties or as an environment variable.
 *
 * Covers:
 *   - GET  /playlists/{id}
 *   - GET  /playlists/{id}/tracks
 *   - POST /users/{user_id}/playlists   (create playlist)
 *   - PUT  /playlists/{id}              (update details)
 *   - POST /playlists/{id}/tracks       (add tracks)
 *   - DELETE /playlists/{id}/tracks     (remove tracks)
 */
@Epic("Spotify Web API")
@Feature("Playlists")
public class PlaylistsApiTest extends BaseTest {

    private String userAccessToken;
    /** Created playlist ID – shared across tests in this class */
    private static String createdPlaylistId;
    /** Assumes the authenticated user's ID is set in config or derived */
    private static final String TEST_USER_ID = System.getProperty("SPOTIFY_USER_ID", "spotify");

    @BeforeClass
    public void setupUserToken() {
        userAccessToken = TokenManager.getUserAccessToken();
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /playlists/{id}
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get public playlist returns 200 with playlist data")
    @Story("Get Playlist")
    @Severity(SeverityLevel.CRITICAL)
    public void getPlaylist_validPublicPlaylist_returns200() {
        given(requestSpec)
                .header("Authorization", "Bearer " + userAccessToken)
                .pathParam("playlist_id", TestData.PLAYLIST_ID_GLOBAL_TOP_50)
                .when()
                .get(ApiEndpoints.PLAYLIST)
                .then()
                .statusCode(200)
                .body("id", equalTo(TestData.PLAYLIST_ID_GLOBAL_TOP_50))
                .body("type", equalTo("playlist"))
                .body("name", not(emptyOrNullString()))
                .body("public", equalTo(true))
                .body("tracks.items", not(empty()));
    }

    @Test(description = "Get playlist with invalid ID returns 404")
    @Story("Get Playlist")
    @Severity(SeverityLevel.NORMAL)
    public void getPlaylist_invalidId_returns400Or404() {
        int status = given(requestSpec)
                .header("Authorization", "Bearer " + userAccessToken)
                .pathParam("playlist_id", TestData.INVALID_ID)
                .when()
                .get(ApiEndpoints.PLAYLIST)
                .then()
                .extract().response().getStatusCode();

        assertThat(status).isIn(400, 404);
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /playlists/{id}/tracks
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Get playlist tracks returns paginated track list")
    @Story("Get Playlist Tracks")
    @Severity(SeverityLevel.CRITICAL)
    public void getPlaylistTracks_validPlaylist_returnsPaginatedList() {
        Response response = given(requestSpec)
                .header("Authorization", "Bearer " + userAccessToken)
                .pathParam("playlist_id", TestData.PLAYLIST_ID_GLOBAL_TOP_50)
                .queryParam("limit", 10)
                .queryParam("offset", 0)
                .when()
                .get(ApiEndpoints.PLAYLIST_TRACKS)
                .then()
                .statusCode(200)
                .body("items", not(empty()))
                .body("total", greaterThan(0))
                .extract().response();

        assertThat(response.jsonPath().getList("items"))
                .hasSizeLessThanOrEqualTo(10);
    }

    @Test(description = "Playlist track items contain required track fields")
    @Story("Get Playlist Tracks")
    @Severity(SeverityLevel.NORMAL)
    public void getPlaylistTracks_responseContainsTrackFields() {
        given(requestSpec)
                .header("Authorization", "Bearer " + userAccessToken)
                .pathParam("playlist_id", TestData.PLAYLIST_ID_GLOBAL_TOP_50)
                .queryParam("limit", 1)
                .when()
                .get(ApiEndpoints.PLAYLIST_TRACKS)
                .then()
                .statusCode(200)
                .body("items[0].track.id",          not(emptyOrNullString()))
                .body("items[0].track.name",        not(emptyOrNullString()))
                .body("items[0].track.duration_ms", greaterThan(0))
                .body("items[0].added_at",          not(emptyOrNullString()));
    }

    // ────────────────────────────────────────────────────────────────────────
    // POST /users/{user_id}/playlists  – Create playlist
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Create new playlist returns 201 with playlist object",
          priority = 1)
    @Story("Create Playlist")
    @Severity(SeverityLevel.CRITICAL)
    public void createPlaylist_validPayload_returns201() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "QA Test Playlist - Automated");
        body.put("description", "Created by REST-assured automation framework");
        body.put("public", false);

        Response response = given(requestSpec)
                .header("Authorization", "Bearer " + userAccessToken)
                .pathParam("user_id", TEST_USER_ID)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(ApiEndpoints.CREATE_PLAYLIST)
                .then()
                .statusCode(201)
                .body("id",          not(emptyOrNullString()))
                .body("name",        equalTo("QA Test Playlist - Automated"))
                .body("public",      equalTo(false))
                .body("type",        equalTo("playlist"))
                .extract().response();

        createdPlaylistId = response.jsonPath().getString("id");
        log.info("Created playlist with ID: {}", createdPlaylistId);
        assertThat(createdPlaylistId).isNotBlank();
    }

    @Test(description = "Create playlist without name returns 400",
          priority = 1)
    @Story("Create Playlist")
    @Severity(SeverityLevel.NORMAL)
    public void createPlaylist_missingName_returns400() {
        Map<String, Object> body = new HashMap<>();
        body.put("description", "Missing name field");

        given(requestSpec)
                .header("Authorization", "Bearer " + userAccessToken)
                .pathParam("user_id", TEST_USER_ID)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(ApiEndpoints.CREATE_PLAYLIST)
                .then()
                .statusCode(400)
                .body("error.status", equalTo(400));
    }

    // ────────────────────────────────────────────────────────────────────────
    // PUT /playlists/{id}  – Update playlist details
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Update playlist name and description returns 200",
          priority = 2,
          dependsOnMethods = "createPlaylist_validPayload_returns201")
    @Story("Update Playlist")
    @Severity(SeverityLevel.NORMAL)
    public void updatePlaylist_validPayload_returns200() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "QA Test Playlist - Updated");
        body.put("description", "Updated via REST-assured PUT test");

        given(requestSpec)
                .header("Authorization", "Bearer " + userAccessToken)
                .pathParam("playlist_id", createdPlaylistId)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put(ApiEndpoints.PLAYLIST)
                .then()
                .statusCode(200);
    }

    // ────────────────────────────────────────────────────────────────────────
    // POST /playlists/{id}/tracks  – Add tracks
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Add tracks to playlist returns 201 with snapshot_id",
          priority = 3,
          dependsOnMethods = "createPlaylist_validPayload_returns201")
    @Story("Add Tracks to Playlist")
    @Severity(SeverityLevel.CRITICAL)
    public void addTracksToPlaylist_validUris_returns201() {
        Map<String, Object> body = new HashMap<>();
        body.put("uris", List.of(
                "spotify:track:" + TestData.TRACK_ID_MONEY,
                "spotify:track:" + TestData.TRACK_ID_BOHEMIAN
        ));

        given(requestSpec)
                .header("Authorization", "Bearer " + userAccessToken)
                .pathParam("playlist_id", createdPlaylistId)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(ApiEndpoints.PLAYLIST_TRACKS)
                .then()
                .statusCode(201)
                .body("snapshot_id", not(emptyOrNullString()));
    }

    // ────────────────────────────────────────────────────────────────────────
    // DELETE /playlists/{id}/tracks  – Remove tracks
    // ────────────────────────────────────────────────────────────────────────

    @Test(description = "Remove track from playlist returns 200 with updated snapshot_id",
          priority = 4,
          dependsOnMethods = "addTracksToPlaylist_validUris_returns201")
    @Story("Remove Tracks from Playlist")
    @Severity(SeverityLevel.NORMAL)
    public void removeTrackFromPlaylist_validUri_returns200() {
        Map<String, Object> trackEntry = new HashMap<>();
        trackEntry.put("uri", "spotify:track:" + TestData.TRACK_ID_MONEY);

        Map<String, Object> body = new HashMap<>();
        body.put("tracks", List.of(trackEntry));

        given(requestSpec)
                .header("Authorization", "Bearer " + userAccessToken)
                .pathParam("playlist_id", createdPlaylistId)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .delete(ApiEndpoints.PLAYLIST_TRACKS)
                .then()
                .statusCode(200)
                .body("snapshot_id", not(emptyOrNullString()));
    }
}
