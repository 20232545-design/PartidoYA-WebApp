package com.partidoya.platform.matches.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface PaymentProofStorage {
    StoredPaymentProof store(String originalFileName, String contentType, long size, InputStream inputStream) throws IOException;
    Path resolve(String storageKey);
}
