package com.partidoya.platform.matches.models;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.CreateMatchCommand;
import com.partidoya.platform.matches.models.Location;
import com.partidoya.platform.matches.models.MatchDate;
import com.partidoya.platform.matches.models.MatchDescription;
import com.partidoya.platform.matches.models.MatchId;
import com.partidoya.platform.matches.models.MatchPrice;
import com.partidoya.platform.matches.models.MatchStatus;
import com.partidoya.platform.matches.models.MatchTitle;
import com.partidoya.platform.matches.models.TotalSlots;
import lombok.Getter;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
public class Match {
    private static final Pattern PERU_YAPE_PHONE = Pattern.compile("^9\\d{8}$");

    private MatchId id;
    private UserId organizerId;
    private Long courtReservationId;
    private String sport;
    private MatchTitle title;
    private MatchDescription description;
    private Location address;
    private MatchDate matchDate;
    private TotalSlots totalSlots;
    private MatchPrice price;
    private Double latitude;
    private Double longitude;
    private MatchStatus status;
    private List<UserId> participants;
    private boolean requiresPlayerPayment;
    private String yapePhone;
    private MatchPrice playerPaymentAmount;

    protected Match() {
    }

    public Match(UserId organizerId, String sport, MatchTitle title, MatchDescription description,
                 Location address, MatchDate matchDate, TotalSlots totalSlots, MatchPrice price,
                 Double latitude, Double longitude) {
        this.organizerId = Objects.requireNonNull(organizerId, "organizerId must not be null");
        this.sport = Objects.requireNonNull(sport, "sport must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = description;
        this.address = Objects.requireNonNull(address, "address must not be null");
        this.matchDate = Objects.requireNonNull(matchDate, "matchDate must not be null");
        this.totalSlots = Objects.requireNonNull(totalSlots, "totalSlots must not be null");
        this.price = price;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = MatchStatus.OPEN;
        this.participants = new ArrayList<>();
        this.requiresPlayerPayment = false;
    }

    public Match(CreateMatchCommand command) {
        this(command.organizerId(), command.sport(), command.title(), command.description(),
                command.address(), command.matchDate(), command.totalSlots(), command.price(),
                command.latitude(), command.longitude());
        this.courtReservationId = command.courtReservationId();
        configurePlayerPayment(command.requiresPlayerPayment(), command.yapePhone());
    }

    public Match(MatchId id, UserId organizerId, String sport, MatchTitle title, MatchDescription description,
                 Location address, MatchDate matchDate, TotalSlots totalSlots, MatchPrice price,
                 Double latitude, Double longitude, MatchStatus status, List<UserId> participants) {
        this(organizerId, sport, title, description, address, matchDate, totalSlots, price, latitude, longitude);
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.status = status;
        this.participants = participants != null ? new ArrayList<>(participants) : new ArrayList<>();
    }

    public Match(MatchId id, UserId organizerId, Long courtReservationId, String sport, MatchTitle title,
                 MatchDescription description, Location address, MatchDate matchDate, TotalSlots totalSlots,
                 MatchPrice price, Double latitude, Double longitude, MatchStatus status, List<UserId> participants,
                 boolean requiresPlayerPayment, String yapePhone, MatchPrice playerPaymentAmount) {
        this(id, organizerId, sport, title, description, address, matchDate, totalSlots, price, latitude, longitude,
                status, participants);
        this.courtReservationId = courtReservationId;
        this.requiresPlayerPayment = requiresPlayerPayment;
        this.yapePhone = normalizePhone(yapePhone);
        this.playerPaymentAmount = playerPaymentAmount;
    }

    public void join(UserId userId) {
        if (requiresPlayerPayment) {
            throw new IllegalStateException("This match requires payment proof before joining");
        }
        confirmParticipant(userId);
    }

    public void confirmParticipant(UserId userId) {
        if (status == MatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot join a cancelled match");
        }
        if (status == MatchStatus.FULL) {
            throw new IllegalStateException("No available slots");
        }
        if (participants.stream().anyMatch(p -> p.value().equals(userId.value()))) {
            throw new IllegalStateException("User already joined this match");
        }
        participants.add(userId);
        if (participants.size() >= totalSlots.value()) {
            status = MatchStatus.FULL;
        }
    }

    public void leave(UserId userId) {
        if (status == MatchStatus.CANCELLED) {
            throw new IllegalStateException("Cannot leave a cancelled match");
        }
        if (organizerId.value().equals(userId.value())) {
            throw new IllegalStateException("The organizer cannot leave the match");
        }
        boolean removed = participants.removeIf(p -> p.value().equals(userId.value()));
        if (!removed) {
            throw new IllegalStateException("User is not a participant of this match");
        }
        if (status == MatchStatus.FULL) {
            status = MatchStatus.OPEN;
        }
    }

    public void cancel(UserId requesterId) {
        if (!organizerId.value().equals(requesterId.value())) {
            throw new IllegalStateException("Only the organizer can cancel the match");
        }
        if (status == MatchStatus.CANCELLED) {
            throw new IllegalStateException("Match is already cancelled");
        }
        if (matchDate.value().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot cancel a match that has already occurred");
        }
        status = MatchStatus.CANCELLED;
    }

    public List<UserId> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    /** Indica si el usuario forma parte del partido, ya sea como organizador o como participante. */
    public boolean involves(UserId userId) {
        if (userId == null) return false;
        return organizerId.value().equals(userId.value())
                || participants.stream().anyMatch(participant -> participant.value().equals(userId.value()));
    }

    public int getAvailableSlots() {
        return totalSlots.value() - participants.size();
    }

    public void configurePlayerPayment(boolean required, String phone) {
        this.requiresPlayerPayment = required;
        if (!required) {
            this.yapePhone = null;
            this.playerPaymentAmount = null;
            return;
        }
        var normalizedPhone = normalizePhone(phone);
        if (!PERU_YAPE_PHONE.matcher(normalizedPhone).matches()) {
            throw new IllegalArgumentException("Yape phone must be a valid Peruvian mobile number");
        }
        if (price == null || price.value() == null || price.value().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Court price is required to request player payments");
        }
        this.yapePhone = normalizedPhone;
        this.playerPaymentAmount = new MatchPrice(price.value()
                .divide(BigDecimal.valueOf(totalSlots.value()), 2, RoundingMode.HALF_UP));
    }

    public boolean acceptsPaymentProofs() {
        return requiresPlayerPayment && status == MatchStatus.OPEN && getAvailableSlots() > 0;
    }

    private static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }
}
