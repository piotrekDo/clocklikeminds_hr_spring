package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class PtoTransformer {

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    PtoEntity ptoEntityFromNewRequest(LocalDate start, LocalDate end, AppUserEntity applier, AppUserEntity acceptor, int businessDays, int includingLastYearPool) {
        return new PtoEntity(
                null,
                LocalDateTime.now(),
                start,
                end,
                applier,
                acceptor,
                false,
                null,
                businessDays,
                includingLastYearPool,
                null
        );
    }

    PtoDto ptoEntityToDto(PtoEntity request) {
        AppUserEntity applier = request.getApplier();
        AppUserEntity acceptor = request.getAcceptor();
        final boolean isPending = !request.isWasAccepted() && request.getDecisionDateTime() == null;
        long totalDays = DAYS.between(request.getPtoStart(), request.getPtoEnd()) + 1;
        return new PtoDto(
                request.getPtoRequestId(),
                isPending,
                request.isWasAccepted(),
                request.getRequestDateTime(),
                request.getPtoStart(),
                request.getPtoEnd(),
                applier.getAppUserId(),
                applier.getFirstName(),
                applier.getLastName(),
                applier.getUserEmail(),
                applier.getPtoDaysCurrentYear(),
                applier.getPtoDaysTaken(),
                acceptor.getAppUserId(),
                acceptor.getFirstName(),
                acceptor.getLastName(),
                acceptor.getUserEmail(),
                request.getDecisionDateTime(),
                totalDays,
                request.getBusinessDays(),
                request.getIncludingLastYearPool(),
                request.getDeclineReason()
        );
    }
}
