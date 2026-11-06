package com.donation;

import com.donation.dto.request.ApprovalRequest;
import com.donation.dto.request.CreateTransactionRequest;
import com.donation.dto.request.LoginRequest;
import com.donation.enums.PaymentMethod;
import com.donation.enums.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCannotApproveIncome() throws Exception {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setComments("Trying to approve as admin");

        mockMvc.perform(post("/api/transactions/6/approve")
                .cookie(login("admin1@donation.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
            .andExpect(status().isForbidden());
    }

    @Test
    void transactionCreationRequiresBillMultipartRequest() throws Exception {
        mockMvc.perform(post("/api/transactions")
                .cookie(login("admin1@donation.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void userCreationRequiresAnInitialPassword() throws Exception {
        mockMvc.perform(post("/api/users")
                .cookie(login("superadmin@donation.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New User\",\"email\":\"newuser@example.org\",\"roleId\":1}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithInvalidCredentialsFails() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("superadmin@donation.com");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void superAdminCannotUpdateApprovedTransaction() throws Exception {
        CreateTransactionRequest updateReq = new CreateTransactionRequest();
        updateReq.setType(TransactionType.INCOME);
        updateReq.setAmount(new BigDecimal("1000.00"));
        updateReq.setTransactionDate(LocalDate.now());
        updateReq.setPaymentMethod(PaymentMethod.CASH);
        updateReq.setIncomeCategoryId(1L);

        mockMvc.perform(put("/api/transactions/1")
                .cookie(login("superadmin@donation.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
            .andExpect(status().isConflict());
    }

    private Cookie login(String email) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("Admin@123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        Cookie accessToken = result.getResponse().getCookie("access_token");
        assertNotNull(accessToken, "Login should set an access-token cookie");
        return accessToken;
    }
}
