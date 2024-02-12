package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class PtoTransformer {

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    PtoSummary createPtoSummary(AppUserEntity appUserEntity, List<PtoEntity> lastRequests) {
        final List<PtoDto> ptoRequests = lastRequests.stream().map(this::ptoEntityToDto).collect(Collectors.toList());

        return new PtoSummary(
                appUserEntity.getPtoDaysAccruedLastYear(),
                appUserEntity.getPtoDaysAccruedCurrentYear(),
                appUserEntity.getPtoDaysLeftFromLastYear(),
                appUserEntity.getPtoDaysLeftCurrentYear(),
                appUserEntity.getPtoDaysTaken(),
                ptoRequests
        );

    }

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
                applier.getPtoDaysLeftCurrentYear() + applier.getPtoDaysLeftFromLastYear(),
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
