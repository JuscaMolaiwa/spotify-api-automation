# Spotify Web API – Automated Test Framework

> **Section 3: API Automation with Postman & REST-assured**  
> System Under Test: [Spotify Web API](https://developer.spotify.com/documentation/web-api)

---

## Table of Contents

1. [Framework Overview](#framework-overview)
2. [Project Structure](#project-structure)
3. [Technology Stack](#technology-stack)
4. [Prerequisites](#prerequisites)
5. [Configuration & Credentials](#configuration--credentials)
6. [Running the Tests](#running-the-tests)
7. [Test Coverage](#test-coverage)
8. [Postman Collection](#postman-collection)
9. [Reporting (Allure)](#reporting-allure)
10. [Framework Design Decisions](#framework-design-decisions)

---

## Framework Overview

This framework provides automated API test coverage for the Spotify Web API using two complementary tools:

| Tool                           | Purpose                                                         |
|--------------------------------|-----------------------------------------------------------------|
| **REST-assured (Java/TestNG)** | CI-ready, code-based automation; runs in a Maven pipeline       |
| **Postman Collection**         | Exploratory testing, collaboration, and manual/Newman execution |

Both suites share the same test design: positive happy-path tests, negative/boundary tests, and schema-validation tests across all major Spotify API resources.

---

## Project Structure

```
spotify-api-tests/
├── pom.xml                                  # Maven dependencies & plugins
├── postman/
│   ├── Spotify_API_QA_Collection.postman_collection.json
│   └── Spotify_API_QA.postman_environment.json
└── src/
    └── test/
        ├── java/com/spotify/api/
        │   ├── base/
        │   │   └── BaseTest.java            # Shared RequestSpec, ResponseSpec, setup
        │   ├── config/
        │   │   ├── FrameworkConfig.java     # Owner-based config interface
        │   │   └── ConfigManager.java       # Singleton config accessor
        │   ├── models/                      # (POJO models for request/response bodies)
        │   ├── tests/
        │   │   ├── AuthenticationTest.java  # OAuth2 token endpoint tests
        │   │   ├── AlbumsApiTest.java       # Albums endpoints
        │   │   ├── ArtistsApiTest.java      # Artists endpoints
        │   │   ├── TracksApiTest.java       # Tracks + Audio Features/Analysis
        │   │   ├── SearchApiTest.java       # Search endpoint (data-driven)
        │   │   └── PlaylistsApiTest.java    # Playlists CRUD (user token)
        │   └── utils/
        │       ├── TokenManager.java        # OAuth2 token caching & renewal
        │       ├── ApiEndpoints.java        # All endpoint path constants
        │       └── TestData.java            # Stable Spotify IDs & search terms
        └── resources/
            ├── testng.xml                   # TestNG suite definition
            ├── config.properties.template   # Config template (copy → config.properties)
            └── logback.xml                  # Logging configuration
```

---

## Technology Stack

| Component         | Library / Version         |
|-------------------|---------------------------|
| Language          | Java 17                   |
| Build Tool        | Maven 3.8+                |
| API Testing       | REST-assured 5.3.2        |
| Test Runner       | TestNG 7.8.0              |
| JSON Processing   | Jackson 2.15.2            |
| Assertions        | AssertJ 3.24.2 + Hamcrest |
| Reporting         | Allure 2.24.0             |
| Config Management | Owner 1.0.12              |
| Logging           | Logback / SLF4J           |
| Postman           | Postman v10+ / Newman     |

---

## Prerequisites

- **Java 11+** installed (`java -version`)
- **Maven 3.8+** installed (`mvn -version`)
- A Spotify Developer account with an app registered at [developer.spotify.com/dashboard](https://developer.spotify.com/dashboard)
- (For user-scoped tests) A valid user access token with the required OAuth scopes

---

## Configuration & Credentials

### Step 1 – Create your config file

```bash
cp src/test/resources/config.properties.template src/test/resources/config.properties
```

### Step 2 – Fill in credentials

```properties
# src/test/resources/config.properties

CLIENT_ID=your_client_id_here
CLIENT_SECRET=your_client_secret_here

# Only needed for Playlists tests
USER_ACCESS_TOKEN=your_user_access_token_here
```

> ⚠️ **Security:** `config.properties` is in `.gitignore`. Never commit real credentials.

### Alternative – Environment Variables / JVM Properties

```bash
export CLIENT_ID=your_client_id
export CLIENT_SECRET=your_client_secret

# or via Maven
mvn test -DCLIENT_ID=your_id -DCLIENT_SECRET=your_secret
```

### Obtaining a User Access Token

User-scoped endpoints (Playlists) require a token generated via the Authorization Code flow with these scopes:

- `playlist-read-public`
- `playlist-read-collaborative`
- `playlist-modify-public`
- `playlist-modify-private`

Use the [Spotify OAuth Playground](https://developer.spotify.com/documentation/web-api/tutorials/code-flow) or the Postman **Get Access Token** request as a starting point.

---

## Running the Tests

```bash
mvn clean install -U -DskipTests
```

### Run the full suite

```bash
mvn clean test
```

### Run a specific test class

```bash
mvn test -Dtest=AlbumsApiTest
```

### Run a specific test method

```bash
mvn test -Dtest=AlbumsApiTest#getAlbum_validId_returns200WithAlbumData
```

### Run only authentication and public API tests (no user token needed)

```bash
mvn test -Dgroups=public
```

### Skip playlist tests (if no user token available)

```bash
mvn test -Dexclude=PlaylistsApiTest
```

---

## Test Coverage

### Authentication (5 tests)
| Test                                     | Type     | Assertion                                         |
|------------------------------------------|----------|---------------------------------------------------|
| Client credentials -> 200 + access_token | Positive | Token present, token_type=Bearer, expires_in=3600 |
| Invalid credentials -> 400               | Negative | error field present                               |
| Missing grant_type -> 400                | Negative | Error response                                    |
| Invalid grant_type -> 400                | Negative | Error response                                    |
| Expired/invalid token -> 401             | Negative | error.status=401                                  |

### Albums (9 tests)
| Test                                     | Type       | Key Assertions                  |
|------------------------------------------|------------|---------------------------------|
| GET /albums/{id} – valid ID              | Positive   | id, name, artists, total_tracks |
| GET /albums/{id} – invalid ID            | Negative   | 400 + error.status              |
| GET /albums/{id} – no token              | Security   | 401 + error.status              |
| GET /albums/{id} – nonexistent           | Negative   | 404                             |
| GET /albums/{id} – URI format            | Schema     | Starts with `spotify:album:`    |
| GET /albums – 3 IDs                      | Positive   | Array size = 3                  |
| GET /albums – no IDs                     | Negative   | 400                             |
| GET /albums – >20 IDs                    | Boundary   | 400                             |
| GET /albums/{id}/tracks – pagination     | Positive   | limit, offset, items            |
| GET /albums/{id}/tracks – page 2 differs | Pagination | Items on p2 ≠ items on p1       |
| GET /albums/{id}/tracks – limit=0        | Edge case  | Graceful response               |

### Artists (9 tests)
Covers GET /artists/{id}, GET /artists, GET /artists/{id}/albums (with include_groups filter), GET /artists/{id}/top-tracks, GET /artists/{id}/related-artists.

### Tracks (8 tests)
Covers GET /tracks/{id}, GET /tracks (multiple), GET /audio-features/{id} (coefficient range validation), GET /audio-features (multiple), GET /audio-analysis/{id}.

### Search (10 tests)
Data-driven tests across 4 search types (artist, album, track, playlist), multi-type search, market filter, pagination, and 4 negative/boundary tests.

### Playlists (9 tests)
Full CRUD lifecycle: GET public playlist, GET playlist tracks, POST create, PUT update, POST add tracks, DELETE remove track. Plus negative tests for invalid IDs and missing fields.

**Total: ~50 automated test cases**

---

## Postman Collection

The collection in `postman/` mirrors the Java test suite and is importable directly into Postman.

### Import Instructions

1. Open Postman
2. Click **Import** → select both files:
   - `Spotify_API_QA_Collection.postman_collection.json`
   - `Spotify_API_QA.postman_environment.json`
3. Select the **Spotify API – QA Environment**
4. Set `CLIENT_ID`, `CLIENT_SECRET`, `SPOTIFY_USER_ID` in the environment
5. Run **0. Authentication → Get Access Token** first – the token auto-saves to `ACCESS_TOKEN`
6. Run other folders individually or use the **Collection Runner**

### Running with Newman (CLI)

```bash
# Install Newman
npm install -g newman newman-reporter-htmlextra

# Run collection with environment
newman run postman/Spotify_API_QA_Collection.postman_collection.json \
  --environment postman/Spotify_API_QA.postman_environment.json \
  --env-var CLIENT_ID=your_id \
  --env-var CLIENT_SECRET=your_secret \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export target/newman-report.html

# Run a specific folder only
newman run postman/Spotify_API_QA_Collection.postman_collection.json \
  --folder "1. Albums" \
  --environment postman/Spotify_API_QA.postman_environment.json
```

---

## Reporting (Allure)

### Generate and open the Allure report

```bash
# Run tests (results saved to target/allure-results)
mvn clean test
```

```bash
# Generate and serve the HTML report
allure serve target/allure-results
```

### Or generate a static report
```bash
mvn allure:report
# Report available at: target/site/allure-maven-plugin/index.html
```

The report provides:
- Test results grouped by Epic → Feature → Story
- Per-test HTTP request/response logs (via Allure REST-assured filter)
- Severity distribution (Blocker / Critical / Normal / Minor)
- Test history and trend charts

---

## Framework Design Decisions

### BaseTest + RequestSpecification pattern
All tests extend `BaseTest`, which builds a shared `RequestSpecification` in `@BeforeSuite`. This ensures consistent headers, base URI, logging filters, and the Allure filter are applied to every request without repetition.

### TokenManager with caching
The `TokenManager` fetches a client credentials token once per test suite and caches it until it nears expiry (with a 60-second buffer). This avoids hitting the Accounts Service on every test and prevents rate limiting.

### Owner library for configuration
Type-safe configuration properties with automatic environment variable and JVM property override support. Avoids raw `System.getenv()` calls scattered throughout the code.

### Centralised constants (ApiEndpoints, TestData)
All endpoint paths and test data IDs live in dedicated constants classes. A broken endpoint or renamed path is fixed in one place.

### AssertJ + Hamcrest together
- **Hamcrest** matchers are used inline in REST-assured chain (`body("field", equalTo(x))`) for concise schema-level assertions
- **AssertJ** is used in standalone assertions for richer fluent API (`.isBetween()`, `.containsExactlyInAnyOrder()`, `.isIn()`)

### Dependency chain in Playlists tests
Playlist CRUD tests use `dependsOnMethods` and shared state (`createdPlaylistId`) to model a realistic lifecycle: create → update → add tracks → remove tracks. The `priority` attribute ensures correct ordering within the class.
