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

    PtoEntity ptoEntityFromNewRequest(String leaveType, boolean isDemand, String notes, LocalDate start, LocalDate end, AppUserEntity applier, AppUserEntity acceptor, int businessDays, int includingLastYearPool) {
        return new PtoEntity(
                null,
                leaveType,
                isDemand,
                notes,
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
        final boolean isOccasionalLeave = request instanceof OccasionalLeaveEntity;
        String leaveReason = isOccasionalLeave ? ((OccasionalLeaveEntity) request).getLeaveReason() : null;
        Integer occasionalTypeId = isOccasionalLeave ? ((OccasionalLeaveEntity) request).getOccasionalType().getId() : null;
        String occasionalLeaveType = isOccasionalLeave ? ((OccasionalLeaveEntity) request).getOccasionalType().getOccasionalType() : null;
        String occasionalLeaveDescPolish = isOccasionalLeave ? ((OccasionalLeaveEntity) request).getOccasionalType().getDescriptionPolish() : null;
        Integer occasionalDays = isOccasionalLeave ? ((OccasionalLeaveEntity) request).getOccasionalType().getDays() : null;

        return new PtoDto(
                request.getPtoRequestId(),
                request.getLeaveType(),
                request.isDemand(),
                request.getNotes(),
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
                applier.getImageUrl(),
                acceptor.getAppUserId(),
                acceptor.getFirstName(),
                acceptor.getLastName(),
                acceptor.getUserEmail(),
                request.getDecisionDateTime(),
                totalDays,
                request.getBusinessDays(),
                request.getIncludingLastYearPool(),
                request.getDeclineReason(),
                leaveReason,
                occasionalTypeId,
                occasionalLeaveType,
                occasionalLeaveDescPolish,
                occasionalDays
        );
    }
}

