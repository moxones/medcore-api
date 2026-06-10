package com.medical.medcore.report.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ReportFormatter {

    private static final Locale PE = Locale.forLanguageTag("es-PE");
    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(PE);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ReportFormatter() {
    }

    public static String currency(BigDecimal amount) {
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        DecimalFormat df = new DecimalFormat("#,##0.##", SYMBOLS);
        return "S/ " + df.format(value);
    }

    public static String currency(double amount) {
        return currency(BigDecimal.valueOf(amount));
    }

    public static String number(long value) {
        DecimalFormat df = new DecimalFormat("#,##0", SYMBOLS);
        return df.format(value);
    }

    public static String number(double value) {
        DecimalFormat df = new DecimalFormat("#,##0.##", SYMBOLS);
        return df.format(value);
    }

    public static String percent(double ratio0to100) {
        DecimalFormat df = new DecimalFormat("#,##0", SYMBOLS);
        return df.format(ratio0to100) + "%";
    }

    public static String percent1(double ratio0to100) {
        DecimalFormat df = new DecimalFormat("#,##0.0", SYMBOLS);
        return df.format(ratio0to100) + "%";
    }

    public static double ratio(long part, long total) {
        if (total == 0) {
            return 0d;
        }
        return BigDecimal.valueOf((double) part / total * 100)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static String minutes(long minutes) {
        return number(minutes) + " min";
    }

    public static String dateTime(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME) : "";
    }
}
