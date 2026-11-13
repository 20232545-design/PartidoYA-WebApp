package com.partidoya.platform.courts.models;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Una entrada del horario de una cancha. Conviven dos formatos guardados:
 * <ul>
 *   <li>Turno fijo (formato original): {@code "18:00-19:00"}, válido todos los días.</li>
 *   <li>Plantilla semanal (asistente de canchas): {@code "MONDAY|08:00|22:00|60"}, es decir
 *       día, apertura, cierre y minutos por turno.</li>
 * </ul>
 */
public sealed interface CourtSchedule {
    int MAX_SLOT_MINUTES = 24 * 60;

    /** Turnos reservables que esta entrada aporta en la fecha indicada. */
    List<TimeSlot> slotsOn(LocalDate date);

    /** Interpreta una entrada guardada; vacío si el texto no tiene ningún formato conocido. */
    static Optional<CourtSchedule> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            var text = value.trim();
            return text.contains("|") ? WeeklyOpeningHours.parse(text) : FixedSlot.parse(text);
        } catch (DateTimeParseException | IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    record TimeSlot(LocalTime start, LocalTime end) {
        public TimeSlot {
            if (start == null || end == null || !start.isBefore(end)) {
                throw new IllegalArgumentException("slot start must be before its end");
            }
        }

        /** Mismo formato que usan el API y las reservas: {@code "08:00-09:00"}. */
        public String label() {
            return start + "-" + end;
        }
    }

    record FixedSlot(TimeSlot slot) implements CourtSchedule {
        private static Optional<CourtSchedule> parse(String text) {
            var parts = text.split("-");
            if (parts.length != 2) return Optional.empty();
            return Optional.of(new FixedSlot(new TimeSlot(LocalTime.parse(parts[0].trim()), LocalTime.parse(parts[1].trim()))));
        }

        @Override
        public List<TimeSlot> slotsOn(LocalDate date) {
            return List.of(slot);
        }
    }

    record WeeklyOpeningHours(DayOfWeek day, LocalTime opensAt, LocalTime closesAt, int slotMinutes)
            implements CourtSchedule {
        private static final Pattern FORMAT = Pattern.compile("^([A-Z]+)\\|(\\d{2}:\\d{2})\\|(\\d{2}:\\d{2})\\|(\\d{1,4})$");

        public WeeklyOpeningHours {
            if (!opensAt.isBefore(closesAt)) {
                throw new IllegalArgumentException("opening time must be before closing time");
            }
            if (slotMinutes <= 0 || slotMinutes > MAX_SLOT_MINUTES) {
                throw new IllegalArgumentException("slot duration must be between 1 and %d minutes".formatted(MAX_SLOT_MINUTES));
            }
        }

        private static Optional<CourtSchedule> parse(String text) {
            var matcher = FORMAT.matcher(text.toUpperCase());
            if (!matcher.matches()) return Optional.empty();
            return Optional.of(new WeeklyOpeningHours(
                    DayOfWeek.valueOf(matcher.group(1)),
                    LocalTime.parse(matcher.group(2)),
                    LocalTime.parse(matcher.group(3)),
                    Integer.parseInt(matcher.group(4))));
        }

        /** Turnos completos entre apertura y cierre; un último turno que no alcanza a completarse se descarta. */
        @Override
        public List<TimeSlot> slotsOn(LocalDate date) {
            if (date.getDayOfWeek() != day) return List.of();
            var slots = new ArrayList<TimeSlot>();
            var cursor = opensAt;
            while (true) {
                var next = cursor.plusMinutes(slotMinutes);
                // Sin pasar de medianoche: LocalTime daría la vuelta al día siguiente.
                if (!next.isAfter(cursor) || next.isAfter(closesAt)) break;
                slots.add(new TimeSlot(cursor, next));
                cursor = next;
            }
            return slots;
        }
    }
}
