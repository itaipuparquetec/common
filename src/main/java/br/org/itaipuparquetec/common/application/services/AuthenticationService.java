package br.org.itaipuparquetec.common.application.services;

import java.util.stream.Stream;

public interface AuthenticationService {

    /**
     *
     * @return the sub from token. Sub is a unique and immutable identifier of the user.
     */
    String getSub();

    /**
     *
     * @return the name of an authenticated account from a token.
     */
    String getName();

    /**
     *
     * @return the email of an authenticated account from a token.
     */
    String getEmail();

    /**
     *
     * @return same of getPreferredUsername() of an authenticated account from a token.
     */
    String getUsername();

    /**
     *
     * @return the given name of an authenticated account from a token.
     */
    String getGivenName();

    /**
     *
     * @return the family name of an authenticated account from a token.
     */
    String getFamilyName();

    /**
     *
     * @return the preferred username of an authenticated account from a token.
     */
    String getPreferredUsername();

    /**
     *
     * @return the scope of an authenticated account from a token.
     */
    Stream<String> getScope();

}
