package br.org.itaipuparquetec.common.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FindUsersByUsernameFromExternalPlatformService {

    Page<ExternalUserResponse> listExternalUsersByFilter(String usernameFilter, Pageable pageable);

    record ExternalUserResponse(String username, String email, String name, String firstName,
                                String lastName, String externalAuthenticationPlatformId) {
    }
}
