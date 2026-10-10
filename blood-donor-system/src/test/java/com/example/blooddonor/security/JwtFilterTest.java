package com.example.blooddonor.security;

import com.example.blooddonor.service.JwtService;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class JwtFilterTest {

    private JwtService jwtService;
    private JwtFilter jwtFilter;

    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;


    @BeforeEach
    void setUp() {

        jwtService = new JwtService();

        jwtFilter = new JwtFilter(jwtService);

        filterChain = mock(FilterChain.class);

        request = new MockHttpServletRequest();

        response = new MockHttpServletResponse();

        SecurityContextHolder.clearContext();
    }


    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();
    }


    // =========================================================
    // 1. NO AUTHORIZATION HEADER
    // =========================================================

    @Test
    void doFilter_shouldContinue_whenAuthorizationHeaderMissing()
            throws Exception {

        jwtFilter.doFilter(
                request,
                response,
                filterChain
        );


        verify(filterChain).doFilter(
                request,
                response
        );


        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );
    }


    // =========================================================
    // 2. VALID JWT
    // =========================================================

    @Test
    void doFilter_shouldSetAuthentication_whenTokenIsValid()
            throws Exception {

        String token =
                jwtService.generateToken(
                        9L,
                        "DONOR"
                );

        request.addHeader(
                "Authorization",
                "Bearer " + token
        );


        jwtFilter.doFilter(
                request,
                response,
                filterChain
        );


        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        assertNotNull(authentication);

        assertEquals(
                9L,
                authentication.getPrincipal()
        );


        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_DONOR")
                        )
        );


        verify(filterChain).doFilter(
                request,
                response
        );


        assertEquals(
                200,
                response.getStatus()
        );
    }


    // =========================================================
    // 3. VALID HOSPITAL JWT
    // =========================================================

    @Test
    void doFilter_shouldSetHospitalRole_whenHospitalTokenIsValid()
            throws Exception {

        String token =
                jwtService.generateToken(
                        8L,
                        "HOSPITAL"
                );

        request.addHeader(
                "Authorization",
                "Bearer " + token
        );


        jwtFilter.doFilter(
                request,
                response,
                filterChain
        );


        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();


        assertNotNull(authentication);

        assertEquals(
                8L,
                authentication.getPrincipal()
        );


        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_HOSPITAL")
                        )
        );


        verify(filterChain).doFilter(
                request,
                response
        );
    }


    // =========================================================
    // 4. INVALID JWT
    // =========================================================

    @Test
    void doFilter_shouldReturn401_whenTokenIsInvalid()
            throws Exception {

        request.addHeader(
                "Authorization",
                "Bearer invalid-token"
        );


        jwtFilter.doFilter(
                request,
                response,
                filterChain
        );


        assertEquals(
                401,
                response.getStatus()
        );


        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );


        verify(
                filterChain,
                never()
        ).doFilter(
                request,
                response
        );
    }


    // =========================================================
    // 5. TAMPERED JWT
    // =========================================================

    @Test
    void doFilter_shouldReturn401_whenTokenIsTampered()
            throws Exception {

        String token =
                jwtService.generateToken(
                        9L,
                        "DONOR"
                );


        String tamperedToken =
                token.substring(
                        0,
                        token.length() - 1
                ) + "X";


        request.addHeader(
                "Authorization",
                "Bearer " + tamperedToken
        );


        jwtFilter.doFilter(
                request,
                response,
                filterChain
        );


        assertEquals(
                401,
                response.getStatus()
        );


        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );


        verify(
                filterChain,
                never()
        ).doFilter(
                request,
                response
        );
    }


    // =========================================================
    // 6. SWAGGER PATH
    // =========================================================

    @Test
    void doFilter_shouldBypassJwt_whenSwaggerPath()
            throws Exception {

        request.setServletPath(
                "/swagger-ui/index.html"
        );


        jwtFilter.doFilter(
                request,
                response,
                filterChain
        );


        verify(filterChain).doFilter(
                request,
                response
        );


        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );
    }
}