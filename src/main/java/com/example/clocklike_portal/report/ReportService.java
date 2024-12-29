package com.example.clocklike_portal.report;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserDetailsAdapter;
import com.example.clocklike_portal.dates_calculations.HolidayService;
import com.example.clocklike_portal.error.IllegalOperationException;
import com.example.clocklike_portal.timeoff.TimeOffDto;
import com.example.clocklike_portal.timeoff.TimeOffService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AppUserRepository appUserRepository;
    private final TimeOffService timeOffService;
    private final HolidayService holidayService;

    private static UserDetailsAdapter getUserDetails() {
        SecurityContext context = SecurityContextHolder.getContext();
        return (UserDetailsAdapter) context.getAuthentication().getPrincipal();
    }

    byte[] generateCreativeWorkReportTemplate(int monthIndex, int year) {
        long userId = getUserDetails().getUserId();
        AppUserEntity appUserEntity = appUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("No user found with id: " + userId));

        LocalDate start = LocalDate.of(year, monthIndex, 1);
        LocalDate end = LocalDate.of(year, monthIndex, 1).with(TemporalAdjusters.lastDayOfMonth());
        int lastDayValue = end.getDayOfMonth();
        List<TimeOffDto> acceptedRequests = timeOffService.getAcceptedUserRequestsInTimeFrame(userId, start, end);
        int workingHours = timeOffService.calculateBusinessDaysInMonth(start, end) * 8;
        int daysOnHolidays = timeOffService.calculateDaysOnHolidays(acceptedRequests, start.getMonthValue(), start.getYear());
        int totalHoursOnHolidays = daysOnHolidays * 8;
        int workedHours = workingHours - totalHoursOnHolidays;

        return createCreativeWorkTemplate(appUserEntity, lastDayValue, start, workingHours, workedHours, acceptedRequests);
    }

    byte[] createCreativeWorkTemplate(AppUserEntity appUser, int lastDayValue, LocalDate start, int workingHours, int workedHours, List<TimeOffDto> acceptedRequests) {
        try (FileInputStream file = new FileInputStream(new File("src/main/resources/template.xlsx"));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            int daysToAdd = lastDayValue;
            Workbook workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);

            fillEmployee(sheet, appUser.getFirstName(), appUser.getLastName());
            fillMonth(sheet, start);
            fillWorkingHoursInMonth(sheet, workingHours);
            fillWorkedHoursInMonth(sheet, workedHours);
            addDayRow(sheet, daysToAdd);
            fillDates(sheet, daysToAdd, start, acceptedRequests);
            int summaryCellRow = addDaysSummary(sheet, daysToAdd);
            addTimeOnProjectsCellPointer(sheet, summaryCellRow);

            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalOperationException("Could not generate template");
        }
    }

    void addTimeOnProjectsCellPointer(Sheet sheet, int summaryCellRow) {
        Row row = sheet.getRow(6);
        Cell cell = row.getCell(3);
        cell.setCellFormula(String.format("H%s", summaryCellRow));
    }

    int addDaysSummary(Sheet sheet, int days) {
        Row summaryRow = sheet.createRow(10 + days);
        for (int i = 0; i < 10; i++) {
            summaryRow.createCell(i);
        }
        Cell descririptionCell = summaryRow.getCell(6);
        Cell summaryCell = summaryRow.getCell(7);
        descririptionCell.setCellValue("Suma godzin:");
        summaryCell.setCellFormula(String.format("SUM(H11:H%d)", 10 + days));
        return 10 + days + 1;
    }

    void fillDates(Sheet sheet, int days, LocalDate start, List<TimeOffDto> acceptedRequests) {
        Row row;

        HashSet<LocalDate> daysOnHoliday = new HashSet<>();
        acceptedRequests.forEach(r -> {
            LocalDate ptoStart = r.getPtoStart();
            LocalDate ptoEnd = r.getPtoEnd();
            daysOnHoliday.addAll(
                    ptoStart.datesUntil(ptoEnd.plusDays(1))
                    .toList());
        });

        for (int i = 0; i < days; i++) {
            row = sheet.getRow(10 + i);
            Cell cell = row.getCell(2);
            LocalDate localDate = start.plusDays(i);
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(date);

            if (localDate.getDayOfWeek() == DayOfWeek.SATURDAY || localDate.getDayOfWeek() == DayOfWeek.SUNDAY || holidayService.checkIfHoliday(localDate)) {
                for (int i1 = 0; i1 < 8; i1++) {
                    Cell cell1 = row.getCell(2 + i1);
                    CellStyle originalStyle = cell1.getCellStyle();
                    Workbook workbook = sheet.getWorkbook();
                    CellStyle newStyle = workbook.createCellStyle();
                    newStyle.cloneStyleFrom(originalStyle);
                    newStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    newStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    cell1.setCellStyle(newStyle);
                }
            }
            if (daysOnHoliday.contains(localDate)) {
                for (int i1 = 0; i1 < 4; i1++) {
                    Cell cell1 = row.getCell(3 + i1);
                    cell1.setCellValue("URLOP");
                }
            }
        }
    }

    void addDayRow(Sheet sheet, int daysToAdd) {
        Row sourceRow = sheet.getRow(10);
        for (int i = 0; i < daysToAdd - 1; i++) {
            Row newRow = sheet.createRow(11 + i);
            for (int j = 0; j < 10; j++) {
                Cell sourceCell = sourceRow.getCell(j);
                if (sourceCell != null) {
                    Cell targetCell = newRow.createCell(j);
                    targetCell.setCellStyle(sourceCell.getCellStyle());
                }
            }
        }
    }

    void fillWorkedHoursInMonth(Sheet sheet, int value) {
        Row row = sheet.getRow(5);
        Cell cell = row.getCell(3);
        cell.setCellValue(value);
    }

    void fillWorkingHoursInMonth(Sheet sheet, int value) {
        Row row = sheet.getRow(4);
        Cell cell = row.getCell(3);
        cell.setCellValue(value);
    }

    void fillMonth(Sheet sheet, LocalDate date) {
        Row row = sheet.getRow(3);
        Cell cell = row.getCell(3);
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("pl"));
        cell.setCellValue(monthName + " " + date.getYear());
    }

    void fillEmployee(Sheet sheet, String firstName, String lastName) {
        Row row = sheet.getRow(2);
        Cell cell = row.getCell(3);
        cell.setCellValue(firstName + " " + lastName);
    }

}
