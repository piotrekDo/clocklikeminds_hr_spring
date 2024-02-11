package com.example.clocklike_portal.dates_calculations;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class HolidayService {
    private int year = LocalDate.now().getYear();
    private Map<LocalDate, String> selectedYearHolidays = getHolidaysPoland(year);

    public int calculateBusinessDays(LocalDate from, LocalDate to) {
        return from.datesUntil(to.plusDays(1))
                .filter(localDate -> !checkIfHoliday(localDate))
                .filter(localDate -> localDate.getDayOfWeek() != DayOfWeek.SATURDAY && localDate.getDayOfWeek() != DayOfWeek.SUNDAY)
                .toList().size();
    }

    boolean checkIfHoliday(LocalDate date) {
        if (year != date.getYear()) {
            this.year = date.getYear();
            setYear(date.getYear());
        }

        return selectedYearHolidays.get(date) != null;
    }

    void setYear(int year) {
        selectedYearHolidays = getHolidaysPoland(year);
    }

    LocalDate calculateEasterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;

        return LocalDate.of(year, month, day);
    }

    Map<LocalDate, String> getHolidaysPoland(int year) {
        LocalDate easterSunday = calculateEasterSunday(year);
        LocalDate easterMonday = easterSunday.plusDays(1);
        LocalDate whitsun = easterSunday.plusDays(49);
        LocalDate corpusChristi = easterSunday.plusDays(60);

        Map<LocalDate, String> holidays = new HashMap<>();
        holidays.put(LocalDate.of(year, 1, 1), "Nowy Rok");
        holidays.put(LocalDate.of(year, 1, 6), "Trzech Króli");
        holidays.put(easterSunday, "Niedziela Wielkanocna");
        holidays.put(easterMonday, "Poniedziałek Wielkanocny");
        holidays.put(LocalDate.of(year, 5, 1), "Święto Pracy");
        holidays.put(LocalDate.of(year, 5, 3), "Święto Konstytucji 3maja");
        holidays.put(whitsun, "Zesłanie Ducha Świętego (Zielone Świątki)");
        holidays.put(corpusChristi, "Boże Ciało");
        holidays.put(LocalDate.of(year, 8, 15), "Wniebowzięcie Najświętszej Maryi Panny (Święto Matki Bożej Zielnej)");
        holidays.put(LocalDate.of(year, 11, 1), "Wszystkich Świętych");
        holidays.put(LocalDate.of(year, 11, 11), "Narodowe Święto Niepodległości");
        holidays.put(LocalDate.of(year, 12, 25), "Boże Narodzenie 1-szy dzień");
        holidays.put(LocalDate.of(year, 12, 26), "Boże Narodzenie 2-gi dzień");
        System.out.println(holidays);
        return holidays;
    }

}
