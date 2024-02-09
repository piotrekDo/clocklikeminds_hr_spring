package com.example.clocklike_portal.job_position;

import com.example.clocklike_portal.error.IllegalOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class JobPositionServiceTest {

    @Autowired
    JobPositionService positionService;

    @MockBean
    PositionRepository positionRepository;

    @TestConfiguration
    static class JobPositionServiceTestConfiguration {
        @Bean
        JobPositionService jobPositionService(PositionRepository positionRepository) {
            return new JobPositionService(positionRepository);
        }
    }

    @Test
    void adding_new_job_position_with_existing_key_should_throw_an_exception() {
        PositionEntity existingPosition = new PositionEntity(1L, "java_dev", "Java Developer");
        NewJobPositionRequest request = new NewJobPositionRequest("java_dev", "Junior Java Developer");
        Mockito.when(positionRepository.findByPositionKeyIgnoreCase(request.getPositionKey())).thenReturn(Optional.of(existingPosition));

        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> positionService.addNew(request));
        assertEquals("Job position already exists", exception.getMessage());
        Mockito.verify(positionRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void adding_valid_new_job_position_should_return_new_entity() {
        NewJobPositionRequest request = new NewJobPositionRequest("java_junior_dev", "Junior Java Developer");
        Mockito.when(positionRepository.findByPositionKeyIgnoreCase(request.getPositionKey())).thenReturn(Optional.empty());
        PositionEntity expectedResult = new PositionEntity(1L, "java_junior_dev", "Junior Java Developer");
        Mockito.when(positionRepository.save(new PositionEntity("java_junior_dev", "Junior Java Developer"))).thenReturn(expectedResult);

        PositionEntity result = positionService.addNew(request);

        assertEquals(expectedResult, result);

    }

}