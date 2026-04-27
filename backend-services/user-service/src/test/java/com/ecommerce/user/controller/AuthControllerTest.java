package com.ecommerce.user.controller;

import com.ecommerce.user.dto.AuthResponse;
import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({TestSecurityConfig.class, com.ecommerce.common.exception.GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private AuthService authService;

    private final AuthResponse fakeAuthResponse =
        new AuthResponse("access.token.here", "refresh-token-here", "alice", "USER");

    @Test
    void registerReturns201WithTokens() throws Exception {
        when(authService.register(any())).thenReturn(fakeAuthResponse);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("alice", "alice@example.com", "password123"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.accessToken").value("access.token.here"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-here"))
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void registerReturns400WhenEmailMissing() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"alice","password":"password123"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RegisterRequest("alice", "alice@example.com", "short"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturns200WithTokens() throws Exception {
        when(authService.login(any())).thenReturn(fakeAuthResponse);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("alice", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("access.token.here"))
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loginReturns401WhenCredentialsInvalid() throws Exception {
        when(authService.login(any()))
            .thenThrow(new com.ecommerce.common.exception.ServiceException(
                "Invalid credentials", "INVALID_CREDENTIALS",
                org.springframework.http.HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new LoginRequest("alice", "wrongpass"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshReturns200WithNewAccessToken() throws Exception {
        when(authService.refresh("valid-refresh-token")).thenReturn("new.access.token");

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"valid-refresh-token"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("new.access.token"));
    }

    @Test
    void logoutReturns204() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"valid-refresh-token"}
                    """))
            .andExpect(status().isNoContent());
    }
}
