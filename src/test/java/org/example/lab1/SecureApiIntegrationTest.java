package org.example.lab1;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.example.lab1.model.User;
import org.example.lab1.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecureApiIntegrationTest {
    private static final String TEST_USERNAME = "student";
    private static final String TEST_PASSWORD = "ValidPass_12345!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepareData() {
        userRepository.deleteAll();
        userRepository.save(new User(TEST_USERNAME, passwordEncoder.encode(TEST_PASSWORD)));
    }

    @Test
    void protectedUsersEndpointRejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(containsString("Authentication required")));
    }

    @Test
    void assignmentEndpointAlsoReturnsUsersAndRequiresToken() throws Exception {
        mockMvc.perform(get("/api/data")).andExpect(status().isUnauthorized());
        String token = loginAndGetToken();
        mockMvc.perform(get("/api/data").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(TEST_USERNAME))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void invalidCredentialsAreRejected() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanLogInAndReadAuthenticatedUsersList() throws Exception {
        String token = loginAndGetToken();
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(TEST_USERNAME))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void registrationStoresOnlyBcryptHashAndDoesNotReturnIt() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new.student\",\"password\":\"NewPassword_123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new.student"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User saved = userRepository.findByUsername("new.student").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("NewPassword_123!", saved.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches(
                "NewPassword_123!", saved.getPasswordHash()));
    }

    @Test
    void registrationNormalizesUsernameAndRejectsDuplicateCaseInsensitively() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"STUDENT\",\"password\":\"AnotherPass_123!\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationRejectsMarkupInUsername() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"<script>\",\"password\":\"NewPassword_123!\"}"))
                .andExpect(status().isBadRequest());
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + TEST_USERNAME + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        Matcher matcher = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"")
                .matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("Login response did not contain accessToken");
        }
        return matcher.group(1);
    }
}
