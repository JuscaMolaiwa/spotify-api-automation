package com.spotify.api.config;

import org.aeonbits.owner.Config;

@Config.Sources({
    "classpath:config.properties",
    "system:properties",
    "system:env"
})
public interface FrameworkConfig extends Config {

    @Key("BASE_URI")
    @DefaultValue("https://api.spotify.com")
    String baseUri();

    @Key("BASE_PATH")
    @DefaultValue("/v1")
    String basePath();

    @Key("ACCOUNTS_BASE_URI")
    @DefaultValue("https://accounts.spotify.com")
    String accountsBaseUri();

    @Key("CLIENT_ID")
    String clientId();

    @Key("CLIENT_SECRET")
    String clientSecret();

    @Key("GRANT_TYPE")
    @DefaultValue("client_credentials")
    String grantType();

    @Key("USER_ACCESS_TOKEN")
    String userAccessToken();

    @Key("LOG_REQUEST_RESPONSE")
    @DefaultValue("true")
    boolean logRequestResponse();

    @Key("REQUEST_TIMEOUT")
    @DefaultValue("30000")
    int requestTimeout();
}
