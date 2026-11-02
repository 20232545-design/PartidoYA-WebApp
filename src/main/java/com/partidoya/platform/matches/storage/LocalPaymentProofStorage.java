package com.partidoya.platform.matches.storage;

import com.partidoya.platform.matches.storage.PaymentProofStorage;
import com.partidoya.platform.matches.storage.StoredPaymentProof;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class LocalPaymentProofStorage implements PaymentProofStorage {
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Map<String, String> MIME_BY_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp");

    private final Path root;

    public LocalPaymentProofStorage(@Value("${partidoya.payment-proofs-dir:target/payment-proofs}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public StoredPaymentProof store(String originalFileName, String contentType, long size, InputStream inputStream) throws IOException {
        if (size <= 0) throw new IllegalArgumentException("payment proof file is empty");
        if (size > MAX_SIZE) throw new IllegalArgumentException("payment proof file is too large");

        var extension = extensionOf(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw new IllegalArgumentException("file extension is not allowed");

        var expectedMime = MIME_BY_EXTENSION.get(extension);
        if (!expectedMime.equalsIgnoreCase(contentType)) throw new IllegalArgumentException("file content type is not allowed");

        var bytes = inputStream.readAllBytes();
        if (bytes.length == 0 || bytes.length != size) throw new IllegalArgumentException("payment proof file is invalid");
        validateMagic(bytes, expectedMime);

        Files.createDirectories(root);
        var storageKey = UUID.randomUUID() + "." + extension;
        Files.write(root.resolve(storageKey), bytes);
        return new StoredPaymentProof(storageKey, sanitizeOriginalName(originalFileName), expectedMime);
    }

    @Override
    public Path resolve(String storageKey) {
        var path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("invalid proof storage key");
        return path;
    }

    private static String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitizeOriginalName(String fileName) {
        if (fileName == null || fileName.isBlank()) return "proof";
        return Path.of(fileName).getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static void validateMagic(byte[] bytes, String mime) {
        var valid = switch (mime) {
            case "image/jpeg" -> bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
            case "image/png" -> bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50
                    && bytes[2] == 0x4E && bytes[3] == 0x47;
            case "image/webp" -> bytes.length > 12 && bytes[0] == 0x52 && bytes[1] == 0x49
                    && bytes[2] == 0x46 && bytes[3] == 0x46 && bytes[8] == 0x57
                    && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("file content does not match declared type");
    }
}
