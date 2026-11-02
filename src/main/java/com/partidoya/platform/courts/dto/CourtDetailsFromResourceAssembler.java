package com.partidoya.platform.courts.dto;

import com.partidoya.platform.courts.dto.CreateCourtResource;
import com.partidoya.platform.courts.dto.UpdateCourtResource;
import com.partidoya.platform.courts.services.CourtDetails;

public final class CourtDetailsFromResourceAssembler {
    private CourtDetailsFromResourceAssembler() {
    }

    public static CourtDetails from(CreateCourtResource resource) {
        return new CourtDetails(resource.name(), resource.complexName(), resource.description(), resource.address(),
                resource.district(), resource.latitude(), resource.longitude(), resource.pricePerHour(),
                resource.sports(), resource.schedules(), resource.mainImageUrl(), resource.imageUrls(),
                resource.services(), resource.features());
    }

    public static CourtDetails from(UpdateCourtResource resource) {
        return new CourtDetails(resource.name(), resource.complexName(), resource.description(), resource.address(),
                resource.district(), resource.latitude(), resource.longitude(), resource.pricePerHour(),
                resource.sports(), resource.schedules(), resource.mainImageUrl(), resource.imageUrls(),
                resource.services(), resource.features());
    }
}
