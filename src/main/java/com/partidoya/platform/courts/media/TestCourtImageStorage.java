package com.partidoya.platform.courts.media;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestCourtImageStorage implements CourtImageStorage {
    @Override
    public UploadedImage upload(byte[] content, String originalFilename, String folder) {
        return new UploadedImage("test/court-image", "https://res.cloudinary.com/test/image/upload/court-image.jpg");
    }

    @Override
    public void delete(String publicId) {
    }
}
