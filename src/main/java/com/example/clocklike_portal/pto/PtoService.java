package com.example.clocklike_portal.pto;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.dates_calculations.DateChecker;
import com.example.clocklike_portal.dates_calculations.HolidayService;
import com.example.clocklike_portal.error.IllegalOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class PtoService {
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PtoRepository ptoRequestsRepository;
    private final AppUserRepository appUserRepository;
    private final PtoTransformer ptoTransformer;
    private final HolidayService holidayService;
    private final DateChecker dateChecker;

    Page<PtoDto> getPtoRequests(Long userId, Integer page, Integer size) {
        page = page == null || page < 0 ? 0 : page;
        size = size == null || size < 1 ? 1000 : size;

        Page<PtoEntity> result = ptoRequestsRepository.findAllByApplier_AppUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestDateTime")));
        return result == null ? Page.empty() : result.map(ptoTransformer::ptoEntityToDto);
    }

    PtoSummary getUserPtoSummary(long userId) {
        AppUserEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("No user found for applier with ID: " + userId));
        Page<PtoEntity> result = ptoRequestsRepository.findAllByApplier_AppUserId(userId, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "requestDateTime")));

        return ptoTransformer.createPtoSummary(user, result.getContent());
    }

    PtoDto requestPto(NewPtoRequest dto) {
        AppUserEntity applier = appUserRepository.findById(dto.getApplierId())
                .orElseThrow(() -> new NoSuchElementException("No user found for applier with ID: " + dto.getApplierId()));

        if (!applier.isActive()) {
            throw new IllegalOperationException("User account is not active!");
        }

        AppUserEntity acceptor = appUserRepository.findById(dto.getAcceptorId())
                .orElseThrow(() -> new NoSuchElementException("No user found for acceptor with ID: " + dto.getAcceptorId()));

        boolean isAcceptorAdmin = acceptor.getUserRoles().stream().filter(r -> r.getRoleName().equals("admin")).toList().size() == 1;

        if (!isAcceptorAdmin) {
            throw new IllegalOperationException("Selected acceptor has no authorities to accept pto requests");
        }

        LocalDate startDate = LocalDate.parse(dto.getPtoStart(), dateFormatter);
        LocalDate toDate = LocalDate.parse(dto.getPtoEnd(), dateFormatter);
        boolean isPtoRangeValid = dateChecker.checkIfDatesRangeIsValid(startDate, toDate);
        if (!isPtoRangeValid) {
            throw new IllegalOperationException("End date cannot be before start date");
        }

        List<PtoEntity> collidingRequests = ptoRequestsRepository
                .findAllOverlappingRequests(applier, toDate, startDate);
        if (collidingRequests.size() > 0) {
            throw new IllegalOperationException("Request colliding with other pto request");
        }

        int businessDays = holidayService.calculateBusinessDays(startDate, toDate);
        int ptoDaysFromLastYear = applier.getPtoDaysLeftFromLastYear();
        int ptoDaysCurrentYear = applier.getPtoDaysLeftCurrentYear();
        int ptoDaysTaken = applier.getPtoDaysTaken();

        if ((ptoDaysCurrentYear + ptoDaysFromLastYear) < businessDays) {
            throw new IllegalOperationException("Insufficient pto days left");
        }

        int subtractedFromLastYearPool = ptoDaysFromLastYear == 0 ? 0 : Math.min(ptoDaysFromLastYear, businessDays);
        int subtractedFromCurrentYearPool = (businessDays - subtractedFromLastYearPool);


        PtoEntity ptoEntity = ptoRequestsRepository
                .save(ptoTransformer.ptoEntityFromNewRequest(startDate, toDate, applier, acceptor, businessDays, subtractedFromLastYearPool));
        applier.setPtoDaysLeftFromLastYear(ptoDaysFromLastYear - subtractedFromLastYearPool);
        applier.setPtoDaysLeftCurrentYear(ptoDaysCurrentYear - subtractedFromCurrentYearPool);
        applier.setPtoDaysTaken(ptoDaysTaken + businessDays);
        applier.getPtoRequests().add(ptoEntity);
        acceptor.getPtoAcceptor().add(ptoEntity);
        appUserRepository.saveAll(List.of(applier, acceptor));

        return ptoTransformer.ptoEntityToDto(ptoEntity);
    }

    PtoDto resolveRequest(ResolvePtoRequest dto) {
        PtoEntity ptoRequest = ptoRequestsRepository.findById(dto.getPtoRequestId())
                .orElseThrow(() -> new NoSuchElementException("No such PTO request found"));

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUserEntity applier = ptoRequest.getApplier();
        AppUserEntity acceptor = ptoRequest.getAcceptor();

        if (!acceptor.getUserEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new IllegalOperationException("You are not authorized to resolve this PTO request!");
        }

        Boolean isRequestAccepted = dto.getIsAccepted();
        if (!isRequestAccepted) {
            ptoRequest.setDeclineReason(dto.getDeclineReason());
            int requestBusinessDays = ptoRequest.getBusinessDays();
            int includingLastYearPool = ptoRequest.getIncludingLastYearPool();
            applier.setPtoDaysLeftFromLastYear(applier.getPtoDaysLeftFromLastYear() + includingLastYearPool);
            applier.setPtoDaysLeftCurrentYear(applier.getPtoDaysLeftCurrentYear() + (requestBusinessDays - includingLastYearPool));
            applier.setPtoDaysTaken(applier.getPtoDaysTaken() - requestBusinessDays);
        }

        ptoRequest.setWasAccepted(isRequestAccepted);
        ptoRequest.setDecisionDateTime(LocalDateTime.now());
        PtoEntity updatedPtoRequest = ptoRequestsRepository.save(ptoRequest);

        return ptoTransformer.ptoEntityToDto(updatedPtoRequest);
    }

    List<PtoDto> findAllRequestsToAcceptByAcceptId(long id) {
        return ptoRequestsRepository.findAllByDecisionDateTimeIsNullAndAcceptor_appUserId(id).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    public List<PtoDto> getRequestsForUserForYear(Integer year, Long userId) {
        return ptoRequestsRepository.findRequestsForYear(year, userId).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }

    public List<PtoDto> getRequestsForYearForAllUsers(Integer year) {
        return ptoRequestsRepository.findRequestsForYear(year).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList();
    }
}
