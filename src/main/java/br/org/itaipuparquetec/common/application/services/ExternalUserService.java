package br.org.itaipuparquetec.common.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 *
 * @author cristian.souza
 * @version 1.0.0
 * @since 14/10/2025
 */
public interface ExternalUserService {

    Page<ExternalUserResponse> listExternalUsersByFilter(final String search, final Pageable pageable);

    record ExternalUserResponse(String username, String email, String name, String externalAuthenticationPlatformId) {
    }
}
