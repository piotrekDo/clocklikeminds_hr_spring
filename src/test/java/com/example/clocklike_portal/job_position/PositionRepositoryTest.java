package com.example.clocklike_portal.job_position;

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
class PositionRepositoryTest {

    @Autowired
    TestEntityManager testEntityManager;

    @Autowired
    PositionRepository positionRepository;

    @ParameterizedTest
    @ValueSource(strings = {"java_dev", "JAVA_DEV", "Java_Dev"})
    void findByPositionKeyIgnoreCaseShouldReturnValidRecordsIgnoringCase(String input) {
        PositionEntity javaDevPosition = new PositionEntity("java_dev", "Java Developer");
        testEntityManager.persist(javaDevPosition);

        assertEquals(javaDevPosition, positionRepository.findByPositionKeyIgnoreCase(input).get());
    }

}