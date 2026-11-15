package com.medical.medcore.schedule.service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PeruHolidays {

    public record Holiday(LocalDate date, String name) {
    }

    private record Fixed(MonthDay day, String name, int sinceYear) {
    }

    private static final List<Fixed> FIXED = List.of(
            new Fixed(MonthDay.of(1, 1), "Año Nuevo", 0),
            new Fixed(MonthDay.of(5, 1), "Día del Trabajo", 0),
            new Fixed(MonthDay.of(6, 7), "Batalla de Arica y Día de la Bandera", 2022),
            new Fixed(MonthDay.of(6, 29), "San Pedro y San Pablo", 0),
            new Fixed(MonthDay.of(7, 23), "Día de la Fuerza Aérea del Perú", 2023),
            new Fixed(MonthDay.of(7, 28), "Fiestas Patrias", 0),
            new Fixed(MonthDay.of(7, 29), "Fiestas Patrias", 0),
            new Fixed(MonthDay.of(8, 6), "Batalla de Junín", 2022),
            new Fixed(MonthDay.of(8, 30), "Santa Rosa de Lima", 0),
            new Fixed(MonthDay.of(10, 8), "Combate de Angamos", 0),
            new Fixed(MonthDay.of(11, 1), "Día de Todos los Santos", 0),
            new Fixed(MonthDay.of(12, 8), "Inmaculada Concepción", 0),
            new Fixed(MonthDay.of(12, 9), "Batalla de Ayacucho", 2022),
            new Fixed(MonthDay.of(12, 25), "Navidad", 0));

    private PeruHolidays() {
    }

    public static List<Holiday> forYear(int year) {
        List<Holiday> holidays = new ArrayList<>();
        for (Fixed fixed : FIXED) {
            if (year >= fixed.sinceYear()) {
                holidays.add(new Holiday(fixed.day().atYear(year), fixed.name()));
            }
        }
        LocalDate easter = easterSunday(year);
        holidays.add(new Holiday(easter.minusDays(3), "Jueves Santo"));
        holidays.add(new Holiday(easter.minusDays(2), "Viernes Santo"));
        holidays.sort(Comparator.comparing(Holiday::date));
        return holidays;
    }

    static LocalDate easterSunday(int year) {
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
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
