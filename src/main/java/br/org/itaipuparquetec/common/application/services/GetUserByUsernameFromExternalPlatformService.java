package br.org.itaipuparquetec.common.application.services;

public interface GetUserByUsernameFromExternalPlatformService {

    UserOutput execute(final String usernameFilter);

    record UserOutput(String username, String name, String firstName, String lastName, String email) {
    }
}
