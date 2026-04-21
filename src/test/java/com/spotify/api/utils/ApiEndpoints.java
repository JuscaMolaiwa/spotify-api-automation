package com.spotify.api.utils;

/**
 * Centralised store of all Spotify API endpoint paths.
 * Using constants prevents typos and makes refactoring easy.
 */
public final class ApiEndpoints {

    private ApiEndpoints() {}

    // ── Albums ────────────────────────────────────────────────────────────────
    public static final String ALBUM            = "/albums/{id}";
    public static final String ALBUMS_MULTIPLE  = "/albums";
    public static final String ALBUM_TRACKS     = "/albums/{id}/tracks";

    // ── Artists ───────────────────────────────────────────────────────────────
    public static final String ARTIST                  = "/artists/{id}";
    public static final String ARTISTS_MULTIPLE        = "/artists";
    public static final String ARTIST_ALBUMS           = "/artists/{id}/albums";
    public static final String ARTIST_TOP_TRACKS       = "/artists/{id}/top-tracks";
    public static final String ARTIST_RELATED_ARTISTS  = "/artists/{id}/related-artists";

    // ── Tracks ────────────────────────────────────────────────────────────────
    public static final String TRACK             = "/tracks/{id}";
    public static final String TRACKS_MULTIPLE   = "/tracks";
    public static final String TRACK_FEATURES    = "/audio-features/{id}";
    public static final String TRACKS_FEATURES   = "/audio-features";
    public static final String TRACK_ANALYSIS    = "/audio-analysis/{id}";

    // ── Search ────────────────────────────────────────────────────────────────
    public static final String SEARCH = "/search";

    // ── Playlists (requires user token) ──────────────────────────────────────
    public static final String PLAYLIST             = "/playlists/{playlist_id}";
    public static final String PLAYLIST_TRACKS      = "/playlists/{playlist_id}/tracks";
    public static final String USER_PLAYLISTS       = "/users/{user_id}/playlists";
    public static final String MY_PLAYLISTS         = "/me/playlists";
    public static final String CREATE_PLAYLIST      = "/users/{user_id}/playlists";

    // ── Users ─────────────────────────────────────────────────────────────────
    public static final String CURRENT_USER         = "/me";
    public static final String USER_PROFILE         = "/users/{user_id}";

    // ── Browse / Recommendations ─────────────────────────────────────────────
    public static final String CATEGORIES           = "/browse/categories";
    public static final String CATEGORY             = "/browse/categories/{category_id}";
    public static final String FEATURED_PLAYLISTS   = "/browse/featured-playlists";
    public static final String NEW_RELEASES         = "/browse/new-releases";
    public static final String RECOMMENDATIONS      = "/recommendations";

    // ── Markets ───────────────────────────────────────────────────────────────
    public static final String MARKETS = "/markets";
}
