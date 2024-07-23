package com.example.clocklike_portal.appUser;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(SpringExtension.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AppUserRepositoryTest {

    @Autowired
    TestEntityManager testEntityManager;

    @Autowired
    AppUserRepository appUserRepository;

    @ParameterizedTest
    @ValueSource(strings = {"test@test.com", "TEST@TEST.COM", "TeSt@tEsT.COm"})
    void findByUserEmailIgnoreCaseShouldReturnValidRecordIgnoringCase(String input) {
        AppUserEntity testAppUser = AppUserEntity.createTestAppUser("test", "test", "test@test.com");
        testEntityManager.persist(testAppUser);

        assertEquals(testAppUser, appUserRepository.findByUserEmailIgnoreCase(input).get());
    }
}