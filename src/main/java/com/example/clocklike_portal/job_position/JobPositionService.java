package com.example.clocklike_portal.job_position;

import com.example.clocklike_portal.error.IllegalOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobPositionService {

    private final PositionRepository positionRepository;

    List<PositionEntity> getAll() {
        return positionRepository.findAll();
    }

    PositionEntity addNew(NewJobPositionRequest request) {
        Optional<PositionEntity> positionByKey = positionRepository.findByPositionKeyIgnoreCase(request.getPositionKey());
        if (positionByKey.isPresent()) {
            throw new IllegalOperationException("Job position already exists");
        }

        return positionRepository.save(new PositionEntity(
                request.getPositionKey(),
                request.getDisplayName()
        ));
    }
}
