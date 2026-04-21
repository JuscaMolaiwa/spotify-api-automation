package com.spotify.api.utils;

/**
 * Static test data – known stable Spotify IDs used across test suites.
 */
public final class TestData {

    private TestData() {}

    // ── Albums ─────────────────────────────────────────────────────────────
    /** The Dark Side of the Moon – Pink Floyd */
    public static final String ALBUM_ID_DARK_SIDE   = "4LH4d3cOWNNsVw41Gqt2kv";
    /** Thriller – Michael Jackson */
    public static final String ALBUM_ID_THRILLER    = "2ANVost0y2y52ema1E9xAZ";
    /** Abbey Road – The Beatles */
    public static final String ALBUM_ID_ABBEY_ROAD  = "0ETFjACtuP2ADo6LFhL6HN";

    // ── Artists ────────────────────────────────────────────────────────────
    /** Pink Floyd */
    public static final String ARTIST_ID_PINK_FLOYD = "0k17h0D3J5VfsdmQ1iZtE9";
    /** Taylor Swift */
    public static final String ARTIST_ID_TAYLOR_SWIFT = "06HL4z0CvFAxyc27GXpf02";
    /** Radiohead */
    public static final String ARTIST_ID_RADIOHEAD  = "4Z8W4fKeB5YxbusRsdQVPb";

    // ── Tracks ─────────────────────────────────────────────────────────────
    /** Money – Pink Floyd */
    public static final String TRACK_ID_MONEY       = "0vFAbKhyBCsFk43sGbHBGR";
    /** Bohemian Rhapsody – Queen */
    public static final String TRACK_ID_BOHEMIAN    = "7tFiyTwD0nx5a1eklYtX2J";
    /** Stairway to Heaven – Led Zeppelin */
    public static final String TRACK_ID_STAIRWAY    = "5CQ30WqJwcep0pYcV4AMNc";

    // ── Playlists ──────────────────────────────────────────────────────────
    /** Global Top 50 (official Spotify playlist) */
    public static final String PLAYLIST_ID_GLOBAL_TOP_50 = "37i9dQZEVXbMDoHDwVN2tF";

    // ── Invalid / negative test IDs ────────────────────────────────────────
    public static final String INVALID_ID          = "INVALID_ID_FOR_TESTING";
    public static final String NONEXISTENT_ID      = "0000000000000000000000";

    // ── Search terms ───────────────────────────────────────────────────────
    public static final String SEARCH_QUERY_ARTIST  = "Pink Floyd";
    public static final String SEARCH_QUERY_TRACK   = "Comfortably Numb";
    public static final String SEARCH_QUERY_ALBUM   = "Dark Side of the Moon";
    public static final String SEARCH_QUERY_EMPTY   = "";

    // ── Markets ────────────────────────────────────────────────────────────
    public static final String MARKET_ZA  = "ZA";
    public static final String MARKET_US  = "US";
    public static final String MARKET_GB  = "GB";
}
