//package com.example.clocklike_portal.job_position;
//
//import com.example.clocklike_portal.appUser.AppUserService;
//import com.example.clocklike_portal.security.AuthorizationService;
//import com.example.clocklike_portal.security.JwtAuthorizationFilter;
//import com.example.clocklike_portal.security.JwtService;
//import com.example.clocklike_portal.security.SecurityConfig;
//import org.hamcrest.Matchers;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
//import org.springframework.security.test.context.support.WithMockUser;
//
//
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(SpringExtension.class)
//@WebMvcTest(JobPositionController.class)
//class JobPositionControllerTest {
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @MockBean
//    AppUserService appUserService;
//
//    @MockBean
//    JwtAuthorizationFilter jwtAuthorizationFilter;
//
//    @MockBean
//    JwtService jwtService;
//
//    @MockBean
//    AuthorizationService authorizationService;
//
//    @MockBean
//    JobPositionService jobPositionService;
//
//    @Test
//    @WithMockUser(username = "Test", authorities = {})
//    void get_all_positions_should_return_code_403_if_requested_by_user_with_no_admin_authority() throws Exception {
//        ResultActions perform = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/positions/all"));
//        perform.andExpect(MockMvcResultMatchers.status().isForbidden());
//        Mockito.verify(jobPositionService, Mockito.times(1)).getAll();
//    }
//
//    @Test
//    @WithMockUser(username = "administrator", authorities = {"admin"})
//    void test () throws Exception {
//        Mockito.when(jobPositionService.getAll()).thenReturn(Collections.emptyList());
//        ResultActions perform = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/positions/all"));
//        perform.andExpect(MockMvcResultMatchers.status().isOk());
//        Mockito.verify(jobPositionService, Mockito.times(1)).getAll();
//    }
//
//
//}