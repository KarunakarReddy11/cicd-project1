package com.devops.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppTest {

    @Test
    void testGreetingContent() {

        String greeting = App.getGreeting();

        assertNotNull(greeting,
                "Greeting message must not be null");

        assertTrue(greeting.contains("Deployment Successful"),
                "Greeting must contain deployment confirmation");
    }
}