package br.org.itaipuparquetec.common.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FindUsersByUsernameFromExternalPlatformService {

    Page<UserOutput> listExternalUsersByFilter(final String usernameFilter, final Pageable pageable);

    record UserOutput(String username, String email, String name, String firstName,
                      String lastName, String externalAuthenticationPlatformId) {
    }
}
