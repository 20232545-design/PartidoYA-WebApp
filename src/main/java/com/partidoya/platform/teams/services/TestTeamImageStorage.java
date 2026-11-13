package com.partidoya.platform.teams.services;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "migration-test"})
public class TestTeamImageStorage implements TeamImageStorage {
    public UploadedTeamImage upload(byte[] content, String filename, Long teamId) {
        var safeName=filename==null||filename.isBlank()?"logo":filename.replaceAll("[^A-Za-z0-9._-]","-");
        return new UploadedTeamImage("test-team-" + teamId + "-" + safeName,
                "https://example.test/team-" + teamId + "-" + safeName);
    }
    public void delete(String publicId) {}
}
