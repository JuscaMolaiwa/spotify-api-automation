package com.spotify.api.base;

import com.spotify.api.config.ConfigManager;
import com.spotify.api.config.FrameworkConfig;
import com.spotify.api.utils.TokenManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Base test class providing shared RequestSpecification, ResponseSpecification,
 * and common setup/teardown for all Spotify API tests.
 */
public class BaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    protected static RequestSpecification requestSpec;
    protected static RequestSpecification accountsRequestSpec;
    protected static ResponseSpecification responseSpec;
    protected static FrameworkConfig config;
    protected static String accessToken;

    @BeforeSuite
    public void globalSetup() {
        config = ConfigManager.getConfig();
        accessToken = TokenManager.getAccessToken();

        // Build shared RequestSpecification for Spotify API
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
                .setBaseUri(config.baseUri())
                .setBasePath(config.basePath())
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addFilter(new AllureRestAssured());

        if (config.logRequestResponse()) {
            try {
                PrintStream logStream = new PrintStream(
                        new FileOutputStream(new File("target/rest-assured.log"), true));
                requestSpecBuilder
                        .addFilter(new RequestLoggingFilter(LogDetail.ALL, logStream))
                        .addFilter(new ResponseLoggingFilter(LogDetail.ALL, logStream));
            } catch (Exception e) {
                log.warn("Could not set up file logging, falling back to console", e);
                requestSpecBuilder.log(LogDetail.ALL);
            }
        }

        requestSpec = requestSpecBuilder.build();

        // Accounts API spec (for OAuth token endpoint)
        accountsRequestSpec = new RequestSpecBuilder()
                .setBaseUri(config.accountsBaseUri())
                .setContentType(ContentType.URLENC)
                .addFilter(new AllureRestAssured())
                .build();

        // Build shared ResponseSpecification – 2xx range
        responseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .build();

        log.info("=== Test Suite Initialized | Base URI: {} ===", config.baseUri());
    }
}
