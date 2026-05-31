package br.org.itaipuparquetec.common.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @deprecated Use {@link FindUsersByUsernameFromExternalPlatformService} instead.
 */
@Deprecated(forRemoval = true)
public interface ExternalUserService {

    Page<ExternalUserResponse> listExternalUsersByFilter(final String search, final Pageable pageable);

    record ExternalUserResponse(String username, String email, String name, String externalAuthenticationPlatformId) {
    }
}
