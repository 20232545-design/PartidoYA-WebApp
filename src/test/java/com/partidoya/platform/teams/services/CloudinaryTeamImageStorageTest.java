package com.partidoya.platform.teams.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partidoya.platform.teams.controllers.TeamExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CloudinaryTeamImageStorageTest {
    @Test
    void uploadsMultipartImageWithoutReactiveMultipartBuilder() {
        var builder=RestClient.builder();
        var server=MockRestServiceServer.bindTo(builder).build();
        var storage=new CloudinaryTeamImageStorage("demo","key","secret",builder.build(),new ObjectMapper());
        server.expect(once(),requestTo("https://api.cloudinary.com/v1_1/demo/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"public_id\":\"partidoya/teams/7/logo\",\"secure_url\":\"https://cdn.test/logo.jpg\"}",MediaType.APPLICATION_JSON));

        var result=storage.upload(new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff},"logo.jpg",7L);

        assertThat(result.publicId()).isEqualTo("partidoya/teams/7/logo");
        assertThat(result.secureUrl()).isEqualTo("https://cdn.test/logo.jpg");
        server.verify();
    }

    @Test
    void reportsMissingConfigurationAndProviderFailuresAsStorageErrors() {
        var unconfigured=new CloudinaryTeamImageStorage("","","",RestClient.create(),new ObjectMapper());
        assertThatThrownBy(()->unconfigured.upload(new byte[]{1},"logo.jpg",1L))
                .isInstanceOf(TeamImageStorageException.class)
                .hasMessageContaining("no está configurado");

        var builder=RestClient.builder();
        var server=MockRestServiceServer.bindTo(builder).build();
        var storage=new CloudinaryTeamImageStorage("demo","key","secret",builder.build(),new ObjectMapper());
        server.expect(requestTo("https://api.cloudinary.com/v1_1/demo/image/upload"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertThatThrownBy(()->storage.upload(new byte[]{1},"logo.jpg",1L))
                .isInstanceOf(TeamImageStorageException.class)
                .hasMessageContaining("No se pudo almacenar");
        server.verify();
    }

    @Test
    void storageFailureIsNotReportedAsBusinessConflict() {
        var response=new TeamExceptionHandler().handleImageStorage(
                new TeamImageStorageException("El almacenamiento no está disponible"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code()).isEqualTo("TEAM_IMAGE_STORAGE_UNAVAILABLE");
    }
}
