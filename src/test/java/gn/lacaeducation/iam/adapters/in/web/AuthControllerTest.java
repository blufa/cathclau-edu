package gn.lacaeducation.iam.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import gn.lacaeducation.iam.application.port.in.AuthUseCase;
import gn.lacaeducation.iam.application.port.in.AuthUseCase.LoginResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // exercise the web layer without the security filter chain
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthUseCase authUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        when(authUseCase.authenticate(any())).thenReturn(
                new LoginResult("jwt.token", "refresh.token", 86400L, 1L, 10L, List.of("SCHOOL_ADMIN")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "admin@ecole.gn", "password", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token"))
                .andExpect(jsonPath("$.tenantId").value(10))
                .andExpect(jsonPath("$.roles[0]").value("SCHOOL_ADMIN"));
    }

    @Test
    void login_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "", "password", "secret123"))))
                .andExpect(status().isBadRequest());
    }
}
