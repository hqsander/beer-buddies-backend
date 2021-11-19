package com.hqsander.beerbuddies.enumeration;

import static com.hqsander.beerbuddies.constant.Authority.*;

public enum Role {
    ROLE_USER(BASICO_AUTHORITIES),
    ROLE_SUPERVISOR(INTERMEDIARIO1_AUTHORITIES),
    ROLE_GERENTE(INTERMEDIARIO2_AUTHORITIES),
    ROLE_ADMIN(ADMINISTRADOR_AUTHORITIES),
    ROLE_SUPER_ADMIN(SUPER_ADMINISTRADOR_AUTHORITIES);

    private String[] authorities;

    Role(String... authorities) {
        this.authorities = authorities;
    }

    public String[] getAuthorities() {
        return authorities;
    }
}
