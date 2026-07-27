package br.org.itaipuparquetec.common.application.services;

public interface GetUserByUsernameFromExternalPlatformService {

    ExternalUserResponse execute(String usernameFilter);

    record ExternalUserResponse(String username, String name, String firstName, String lastName, String email) {
    }
}
