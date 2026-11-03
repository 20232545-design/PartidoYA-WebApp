package com.partidoya.platform.courts.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
@Profile("!test")
public class CloudinaryCourtImageStorage implements CourtImageStorage {
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CloudinaryCourtImageStorage(@Value("${cloudinary.cloud-name:}") String cloudName,
                                       @Value("${cloudinary.api-key:}") String apiKey,
                                       @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public UploadedImage upload(byte[] content, String originalFilename, String folder) {
        requireConfigured();
        var timestamp = Instant.now().getEpochSecond();
        if (folder == null || !folder.matches("partidoya/courts/\\d+/(main|gallery)")) {
            throw new IllegalArgumentException("La carpeta de imágenes no es válida");
        }
        var body = new MultipartBodyBuilder();
        body.part("file", new NamedByteArrayResource(content, originalFilename));
        body.part("api_key", apiKey);
        body.part("timestamp", Long.toString(timestamp));
        body.part("folder", folder);
        body.part("signature", sha1("folder=" + folder + "&timestamp=" + timestamp + apiSecret));
        var response = restClient.post()
                .uri("https://api.cloudinary.com/v1_1/{cloud}/image/upload", cloudName)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .body(String.class);
        try {
            var json = objectMapper.readTree(response);
            return new UploadedImage(required(json.path("public_id").asText(), "public_id"),
                    required(json.path("secure_url").asText(), "secure_url"));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Cloudinary devolvió una respuesta inválida");
        }
    }

    @Override
    public void delete(String publicId) {
        requireConfigured();
        if (publicId == null || publicId.isBlank()) return;
        var timestamp = Instant.now().getEpochSecond();
        var body = new MultipartBodyBuilder();
        body.part("public_id", publicId);
        body.part("api_key", apiKey);
        body.part("timestamp", Long.toString(timestamp));
        body.part("signature", sha1("public_id=" + publicId + "&timestamp=" + timestamp + apiSecret));
        restClient.post()
                .uri("https://api.cloudinary.com/v1_1/{cloud}/image/destroy", cloudName)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .toBodilessEntity();
    }

    private void requireConfigured() {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new IllegalStateException("Cloudinary no está configurado");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalStateException("Falta " + field + " en la respuesta de Cloudinary");
        return value;
    }

    private static String sha1(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar la solicitud a Cloudinary");
        }
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename == null || filename.isBlank() ? "court-image" : filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
