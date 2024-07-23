//package com.example.clocklike_portal.appUser;
//
//import com.example.clocklike_portal.ClocklikePortalApplication;
//import com.example.clocklike_portal.security.JwtAuthorizationFilter;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
//
////@ExtendWith(SpringExtension.class)
////@AutoConfigureMockMvc
////@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = ClocklikePortalApplication.class)
//@WebMvcTest(AppUserControllerTest.class)
//
////@SpringBootTest
////@AutoConfigureMockMvc
//class AppUserControllerTest {
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @MockBean
//    AppUserService appUserService;
//    @MockBean
//    JwtAuthorizationFilter jwtAuthorizationFilter;
//
//    private final static String USERS_URL = "/api/v1/users";
//
//
//    @Test
//    @WithMockUser(username = "user", authorities = {"admin"})
//    void test() throws Exception {
//        Mockito.when(appUserService.finishRegistration(Mockito.any())).thenReturn(new AppUserDto(null, null, null, null, null, false, false, false, null, null, null, null, 0, 0, 0, 0, 0, 0, 0, null, null, null));
//        ResultActions perform = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/finish-register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content("""
//                           {
//                           "appUserId": 1,
//                           "positionKey": null,
//                           "hireStart": null,
//                           "hireEnd": null,
//                           "ptoDaysTotal": null,
//                           "isStillHired": null,
//                           "supervisorId": null
//                           }
//                        """));
//
//        perform.andExpect(MockMvcResultMatchers.status().isOk());
//    }
//}