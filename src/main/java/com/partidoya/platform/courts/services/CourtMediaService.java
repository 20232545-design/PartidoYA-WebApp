package com.partidoya.platform.courts.services;

import com.partidoya.platform.courts.dto.CourtMediaResource;
import com.partidoya.platform.courts.media.CourtImageStorage;
import com.partidoya.platform.courts.models.CourtId;
import com.partidoya.platform.courts.repositories.CourtMediaEntity;
import com.partidoya.platform.courts.repositories.CourtMediaRepository;
import com.partidoya.platform.courts.repositories.CourtRepository;
import com.partidoya.platform.shared.exceptions.ResourceNotFoundException;
import com.partidoya.platform.users.models.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class CourtMediaService {
    private static final long MAX_FILE_SIZE = 8L * 1024 * 1024;
    private final CourtMediaRepository mediaRepository;
    private final CourtRepository courtRepository;
    private final CourtAccessPolicy accessPolicy;
    private final CourtImageStorage imageStorage;

    public CourtMediaService(CourtMediaRepository mediaRepository, CourtRepository courtRepository,
                             CourtAccessPolicy accessPolicy, CourtImageStorage imageStorage) {
        this.mediaRepository = mediaRepository;
        this.courtRepository = courtRepository;
        this.accessPolicy = accessPolicy;
        this.imageStorage = imageStorage;
    }

    public CourtMediaResource upload(CourtId courtId, UserId ownerId, MultipartFile file, boolean primary) {
        var court = accessPolicy.requireOwnedCourt(courtId, ownerId);
        validate(file);
        var existing = mediaRepository.findByCourtIdOrderByDisplayOrderAsc(courtId.value());
        if (existing.size() >= 12) throw new IllegalArgumentException("Una cancha puede tener como máximo 12 imágenes");
        String uploadedPublicId = null;
        try {
            var shouldBePrimary = primary || existing.isEmpty();
            var folder = "partidoya/courts/" + courtId.value() + (shouldBePrimary ? "/main" : "/gallery");
            var uploaded = imageStorage.upload(file.getBytes(), file.getOriginalFilename(), folder);
            uploadedPublicId = uploaded.publicId();
            if (shouldBePrimary) {
                existing.stream().filter(CourtMediaEntity::isPrimaryAsset).forEach(item -> {
                    item.setPrimaryAsset(false);
                    mediaRepository.save(item);
                });
            }
            var media = new CourtMediaEntity();
            media.setCourtId(courtId.value());
            media.setSource("CLOUDINARY");
            media.setCloudinaryPublicId(uploaded.publicId());
            media.setSecureUrl(uploaded.secureUrl());
            media.setDisplayOrder(existing.size());
            media.setPrimaryAsset(shouldBePrimary);
            media = mediaRepository.save(media);
            court.addImage(uploaded.secureUrl(), shouldBePrimary);
            courtRepository.save(court);
            return toResource(media);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer la imagen seleccionada");
        } catch (RuntimeException exception) {
            if (uploadedPublicId != null) {
                try {
                    imageStorage.delete(uploadedPublicId);
                } catch (RuntimeException ignored) {
                }
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<CourtMediaResource> list(CourtId courtId) {
        return mediaRepository.findByCourtIdOrderByDisplayOrderAsc(courtId.value()).stream()
                .map(CourtMediaService::toResource)
                .toList();
    }

    public void delete(Long mediaId, UserId ownerId) {
        var media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Court media", mediaId.toString()));
        var court = accessPolicy.requireOwnedCourt(new CourtId(media.getCourtId()), ownerId);
        if ("CLOUDINARY".equals(media.getSource())) imageStorage.delete(media.getCloudinaryPublicId());
        mediaRepository.delete(media);
        court.removeImage(media.getSecureUrl());
        courtRepository.save(court);
    }

    public List<CourtMediaResource> reorder(CourtId courtId, UserId ownerId, List<Long> mediaIds) {
        accessPolicy.requireOwnedCourt(courtId, ownerId);
        var media = mediaRepository.findByCourtIdOrderByDisplayOrderAsc(courtId.value());
        if (mediaIds == null || mediaIds.size() != media.size()
                || !media.stream().map(CourtMediaEntity::getId).collect(java.util.stream.Collectors.toSet())
                .equals(new java.util.HashSet<>(mediaIds))) {
            throw new IllegalArgumentException("El orden de la galería no es válido");
        }
        for (int index = 0; index < mediaIds.size(); index += 1) {
            var id = mediaIds.get(index);
            var item = media.stream().filter(candidate -> candidate.getId().equals(id)).findFirst().orElseThrow();
            item.setDisplayOrder(index);
            mediaRepository.save(item);
        }
        return list(courtId);
    }

    public CourtMediaResource setPrimary(CourtId courtId, Long mediaId, UserId ownerId) {
        var court = accessPolicy.requireOwnedCourt(courtId, ownerId);
        var media = mediaRepository.findByCourtIdOrderByDisplayOrderAsc(courtId.value());
        var selected = media.stream().filter(item -> item.getId().equals(mediaId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Court media", mediaId.toString()));
        media.forEach(item -> {
            item.setPrimaryAsset(item.getId().equals(mediaId));
            mediaRepository.save(item);
        });
        court.setPrimaryImage(selected.getSecureUrl());
        courtRepository.save(court);
        return toResource(selected);
    }

    private static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecciona una imagen");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("La imagen no puede superar 8 MB");
        if (file.getContentType() == null || !file.getContentType().toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }
        try {
            var bytes = file.getBytes();
            var jpeg = bytes.length > 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8;
            var png = bytes.length > 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
            var webp = bytes.length > 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            if (!jpeg && !png && !webp) throw new IllegalArgumentException("Solo se permiten imágenes JPG, PNG o WebP válidas");
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo validar la imagen");
        }
    }

    private static CourtMediaResource toResource(CourtMediaEntity media) {
        return new CourtMediaResource(media.getId(), media.getCourtId(), media.getSource(), media.getSecureUrl(),
                media.getGooglePhotoName(), media.getAttribution(), media.getDisplayOrder(), media.isPrimaryAsset());
    }
}
