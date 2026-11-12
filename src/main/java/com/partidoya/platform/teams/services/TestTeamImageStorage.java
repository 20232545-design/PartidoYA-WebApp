package com.partidoya.platform.teams.services;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"test", "migration-test"})
public class TestTeamImageStorage implements TeamImageStorage {
    public UploadedTeamImage upload(byte[] content, String filename, Long teamId) {
        return new UploadedTeamImage("test-team-" + teamId, "https://example.test/team-" + teamId + ".png");
    }
    public void delete(String publicId) {}
}
