package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.appUser.AppUserEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

@Service
public class PtoTransformer {

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    PtoSummary createPtoSummary(AppUserEntity appUserEntity, List<HolidayOnSaturdayUserEntity> unusedHolidays) {
        final List<SaturdayHolidayDto> saturdayHolidayDtos = unusedHolidays.stream()
                .map(holiday -> new SaturdayHolidayDto(holiday.getHoliday().getId(), holiday.getHoliday().getDate().toString(), holiday.getHoliday().getNote(), holiday.getPto() != null ? holiday.getPto().getPtoStart().toString() : null))
                .toList();

        return new PtoSummary(
                appUserEntity.getPtoDaysAccruedLastYear(),
                appUserEntity.getPtoDaysAccruedCurrentYear(),
                appUserEntity.getPtoDaysLeftFromLastYear(),
                appUserEntity.getPtoDaysLeftCurrentYear(),
                appUserEntity.getPtoDaysTaken(),
                saturdayHolidayDtos
        );

    }

    PtoEntity ptoEntityFromNewRequest(String leaveType, LocalDate start, LocalDate end, AppUserEntity applier, AppUserEntity acceptor, int businessDays, int includingLastYearPool, String applierNotes) {

        return new PtoEntity(
                null,
                leaveType,
                false,
                applierNotes,
                "",
                "",
                LocalDateTime.now(),
                start,
                end,
                applier,
                acceptor,
                false,
                null,
                businessDays,
                includingLastYearPool,
                null,
                false
        );
    }

    TimeOffDto ptoEntityToDto(PtoEntity request) {
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
        String saturdayHolidayDate = request instanceof HolidayOnSaturdayPtoEntity ? ((HolidayOnSaturdayPtoEntity) request).getHoliday().getDate().toString() : null;

        return new TimeOffDto(
                request.getPtoRequestId(),
                request.getLeaveType(),
                request.isDemand(),
                request.getApplierNotes(),
                request.getAcceptorNotes(),
                request.getApplicationNotes(),
                isPending,
                request.isWasAccepted(),
                request.getRequestDateTime(),
                request.getPtoStart(),
                request.getPtoEnd(),
                applier.getAppUserId(),
                applier.getFirstName(),
                applier.getLastName(),
                applier.getUserEmail(),
                applier.isFreelancer(),
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
                occasionalDays,
                saturdayHolidayDate,
                request.isWasMarkedToWithdraw()
        );
    }
}

