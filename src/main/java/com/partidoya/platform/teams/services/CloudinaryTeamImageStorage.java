package com.partidoya.platform.teams.services;

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
@Profile("!test & !migration-test")
public class CloudinaryTeamImageStorage implements TeamImageStorage {
    private final String cloudName; private final String apiKey; private final String apiSecret;
    private final RestClient client = RestClient.create(); private final ObjectMapper mapper = new ObjectMapper();
    public CloudinaryTeamImageStorage(@Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey, @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudName=cloudName; this.apiKey=apiKey; this.apiSecret=apiSecret;
    }
    public UploadedTeamImage upload(byte[] content, String filename, Long teamId) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) throw new IllegalStateException("Cloudinary no está configurado");
        var folder="partidoya/teams/"+teamId; var timestamp=Instant.now().getEpochSecond();
        var body=new MultipartBodyBuilder(); body.part("file",new Named(content,filename)); body.part("api_key",apiKey);
        body.part("timestamp",Long.toString(timestamp)); body.part("folder",folder);
        body.part("signature",sha1("folder="+folder+"&timestamp="+timestamp+apiSecret));
        var response=client.post().uri("https://api.cloudinary.com/v1_1/{cloud}/image/upload",cloudName)
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body.build()).retrieve().body(String.class);
        try { var json=mapper.readTree(response); return new UploadedTeamImage(json.path("public_id").asText(),json.path("secure_url").asText()); }
        catch(Exception e){ throw new IllegalStateException("Cloudinary devolvió una respuesta inválida"); }
    }
    public void delete(String publicId) { if(publicId==null||publicId.isBlank()||cloudName.isBlank()) return;
        var timestamp=Instant.now().getEpochSecond(); var body=new MultipartBodyBuilder(); body.part("public_id",publicId);
        body.part("api_key",apiKey); body.part("timestamp",Long.toString(timestamp)); body.part("signature",sha1("public_id="+publicId+"&timestamp="+timestamp+apiSecret));
        client.post().uri("https://api.cloudinary.com/v1_1/{cloud}/image/destroy",cloudName).contentType(MediaType.MULTIPART_FORM_DATA).body(body.build()).retrieve().toBodilessEntity(); }
    private static String sha1(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("No se pudo firmar la imagen");}}
    private static final class Named extends ByteArrayResource { private final String name; Named(byte[] b,String n){super(b);name=n;} public String getFilename(){return name==null?"team-logo":name;} }
}
