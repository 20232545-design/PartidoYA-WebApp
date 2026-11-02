package com.partidoya.platform.matches.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalPaymentProofStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void storesValidPngWithRandomName() throws Exception {
        var storage = new LocalPaymentProofStorage(tempDir.toString());
        var bytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

        var proof = storage.store("voucher.png", "image/png", bytes.length, new ByteArrayInputStream(bytes));

        assertThat(proof.storageKey()).endsWith(".png");
        assertThat(Files.exists(storage.resolve(proof.storageKey()))).isTrue();
    }

    @Test
    void rejectsInvalidExtension() {
        var storage = new LocalPaymentProofStorage(tempDir.toString());
        var bytes = new byte[]{1, 2, 3};

        assertThatThrownBy(() -> storage.store("voucher.pdf", "application/pdf", bytes.length, new ByteArrayInputStream(bytes)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCorruptImageContent() {
        var storage = new LocalPaymentProofStorage(tempDir.toString());
        var bytes = new byte[]{1, 2, 3};

        assertThatThrownBy(() -> storage.store("voucher.png", "image/png", bytes.length, new ByteArrayInputStream(bytes)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
