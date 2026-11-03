package com.partidoya.platform.courts.media;

public interface CourtImageStorage {
    UploadedImage upload(byte[] content, String originalFilename, String folder);
    void delete(String publicId);
}
