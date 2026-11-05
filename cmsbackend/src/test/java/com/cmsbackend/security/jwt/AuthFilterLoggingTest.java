package com.cmsbackend.security.jwt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.servlet.FilterChain;

class AuthFilterLoggingTest {
    @Test
    void publicPhoneLocationTokenNeverAppearsInFilterOutput() throws Exception {
        AuthFilter filter = new AuthFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", mock(JwtUtils.class));
        String marker = "one-time-location-token-must-remain-private";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/location-sharing/" + marker + "/location");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream output = new PrintStream(captured)) {
            System.setOut(output);
            filter.doFilter(request, response, chain);
        } finally {
            System.setOut(original);
        }
        assertFalse(captured.toString().contains(marker));
        verify(chain).doFilter(request, response);
    }
}
