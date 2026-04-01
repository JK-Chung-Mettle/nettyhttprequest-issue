package com.example;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Controller(TestController.PATH)
public class TestController {

    public static final String PATH = "/test";

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @Post
    @Status(HttpStatus.OK)
    Map<String, Object> echoBody(@Body final Map<String, Object> body) {
        log.info("Echoing back body {}", body);
        return body;
    }

}
