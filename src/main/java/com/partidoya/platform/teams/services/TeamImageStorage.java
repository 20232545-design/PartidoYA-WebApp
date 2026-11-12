package com.partidoya.platform.teams.services;

public interface TeamImageStorage {
    UploadedTeamImage upload(byte[] content, String filename, Long teamId);
    void delete(String publicId);
    record UploadedTeamImage(String publicId, String secureUrl) {}
}
