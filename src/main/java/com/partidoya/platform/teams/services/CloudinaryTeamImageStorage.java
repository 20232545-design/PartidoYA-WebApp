package com.partidoya.platform.teams.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
@Profile("!test & !migration-test")
public class CloudinaryTeamImageStorage implements TeamImageStorage {
    private final String cloudName; private final String apiKey; private final String apiSecret;
    private final RestClient client; private final ObjectMapper mapper;
    public CloudinaryTeamImageStorage(@Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey, @Value("${cloudinary.api-secret:}") String apiSecret) {
        this(cloudName,apiKey,apiSecret,RestClient.create(),new ObjectMapper());
    }
    CloudinaryTeamImageStorage(String cloudName,String apiKey,String apiSecret,RestClient client,ObjectMapper mapper) {
        this.cloudName=cloudName; this.apiKey=apiKey; this.apiSecret=apiSecret; this.client=client; this.mapper=mapper;
    }
    public UploadedTeamImage upload(byte[] content, String filename, Long teamId) {
        requireConfigured();
        var folder="partidoya/teams/"+teamId; var timestamp=Instant.now().getEpochSecond();
        var body=new LinkedMultiValueMap<String,Object>(); body.add("file",new Named(content,filename)); body.add("api_key",apiKey);
        body.add("timestamp",Long.toString(timestamp)); body.add("folder",folder);
        body.add("signature",sha1("folder="+folder+"&timestamp="+timestamp+apiSecret));
        try {
            var response=client.post().uri("https://api.cloudinary.com/v1_1/{cloud}/image/upload",cloudName)
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(String.class);
            var json=mapper.readTree(response);
            return new UploadedTeamImage(required(json.path("public_id").asText()),required(json.path("secure_url").asText()));
        } catch(TeamImageStorageException exception){
            throw exception;
        } catch(RestClientException exception){
            throw new TeamImageStorageException("No se pudo almacenar el escudo en este momento",exception);
        } catch(Exception exception){
            throw new TeamImageStorageException("Cloudinary devolvió una respuesta inválida",exception);
        }
    }
    public void delete(String publicId) { if(publicId==null||publicId.isBlank()) return; requireConfigured();
        var timestamp=Instant.now().getEpochSecond(); var body=new LinkedMultiValueMap<String,Object>(); body.add("public_id",publicId);
        body.add("api_key",apiKey); body.add("timestamp",Long.toString(timestamp)); body.add("signature",sha1("public_id="+publicId+"&timestamp="+timestamp+apiSecret));
        try { client.post().uri("https://api.cloudinary.com/v1_1/{cloud}/image/destroy",cloudName).contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().toBodilessEntity(); }
        catch(RestClientException exception){throw new TeamImageStorageException("No se pudo eliminar el escudo anterior",exception);} }
    private void requireConfigured(){if(cloudName.isBlank()||apiKey.isBlank()||apiSecret.isBlank())throw new TeamImageStorageException("El almacenamiento de escudos no está configurado");}
    private static String required(String value){if(value==null||value.isBlank())throw new TeamImageStorageException("Cloudinary devolvió una respuesta incompleta");return value;}
    private static String sha1(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new TeamImageStorageException("No se pudo firmar la solicitud de imagen",e);}}
    private static final class Named extends ByteArrayResource { private final String name; Named(byte[] b,String n){super(b);name=n;} public String getFilename(){return name==null?"team-logo":name;} }
}
