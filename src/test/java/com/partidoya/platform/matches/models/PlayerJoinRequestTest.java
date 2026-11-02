package com.partidoya.platform.matches.models;

import com.partidoya.platform.users.models.UserId;
import com.partidoya.platform.matches.models.JoinRequestStatus;
import com.partidoya.platform.matches.models.MatchId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerJoinRequestTest {
    @Test
    void createsPendingProofRequest() {
        var request = request();

        assertThat(request.getStatus()).isEqualTo(JoinRequestStatus.PENDING_PAYMENT_VERIFICATION);
        assertThat(request.getAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void approvesProofWithReviewerDifferentFromPlayer() {
        var request = request();

        request.approve(new UserId(1L));

        assertThat(request.getStatus()).isEqualTo(JoinRequestStatus.CONFIRMED);
        assertThat(request.getReviewedBy().value()).isEqualTo(1L);
    }

    @Test
    void rejectsAndAllowsReplacingProof() {
        var request = request();
        request.reject(new UserId(1L), "Imagen borrosa");

        request.replaceProof("new-proof.png", "new-proof.png", "image/png", new BigDecimal("20.00"));

        assertThat(request.getStatus()).isEqualTo(JoinRequestStatus.PENDING_PAYMENT_VERIFICATION);
        assertThat(request.getRejectionReason()).isNull();
        assertThat(request.getProofStorageKey()).isEqualTo("new-proof.png");
    }

    @Test
    void preventsPlayerApprovingOwnProof() {
        var request = request();

        assertThatThrownBy(() -> request.approve(new UserId(2L)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static PlayerJoinRequest request() {
        return new PlayerJoinRequest(new MatchId(10L), new UserId(2L), "proof.png",
                "proof.png", "image/png", new BigDecimal("20.00"));
    }
}
