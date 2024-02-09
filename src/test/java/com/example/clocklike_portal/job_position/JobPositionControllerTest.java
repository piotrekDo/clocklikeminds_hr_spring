package com.example.clocklike_portal.job_position;

import com.example.clocklike_portal.appUser.AppUserService;
import com.example.clocklike_portal.security.AuthorizationService;
import com.example.clocklike_portal.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.security.test.context.support.WithMockUser;


import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(JobPositionController.class)
class JobPositionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AppUserService appUserService;

    @MockBean
    JwtService jwtService;

    @MockBean
    AuthorizationService authorizationService;

    @MockBean
    JobPositionService jobPositionService;

    @Test
    @WithMockUser(username = "Test", authorities = {"user"})
    void get_all_positions_should_return_code_403_if_requested_by_user_with_no_admin_authority() throws Exception {
        ResultActions perform = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/positions/all"));
        perform.andExpect(MockMvcResultMatchers.status().isForbidden());
    }

}