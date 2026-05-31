package br.org.itaipuparquetec.common.application.services;

public interface GetUserByUsernameFromExternalPlatform {

    UserOutput execute(final String usernameFilter);

    record UserOutput(String username, String name, String firstName, String lastName, String email) {
    }
}
