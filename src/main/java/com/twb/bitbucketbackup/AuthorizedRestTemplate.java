package com.twb.bitbucketbackup;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

public class AuthorizedRestTemplate extends RestTemplate {
    private static Logger log = LoggerFactory.getLogger(AuthorizedRestTemplate.class);

    private String username;
    private String password;

    public AuthorizedRestTemplate(String username, String password) {
        this.username = username;
        this.password = password;
        setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().isError();
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                log.error(response.getStatusText());
            }
        });
    }

    public String getForObject(String url, Object... urlVariables) {
        HttpEntity<String> request = getRequest();
        ResponseEntity<String> entity = exchange(url, HttpMethod.GET, request, String.class, urlVariables);
        return entity.getBody();
    }

    private HttpEntity<String> getRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + getBase64Credentials());
        return new HttpEntity<>(headers);
    }

    private String getBase64Credentials() {
        String plainCreds = username + ":" + password;
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        return new String(base64CredsBytes);
    }
}