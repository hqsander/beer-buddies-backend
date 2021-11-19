package com.hqsander.beerbuddies.constant;

public class Authority {
    public static final String[] BASICO_AUTHORITIES = {"user:read"};
    public static final String[] INTERMEDIARIO1_AUTHORITIES = {"user:read", "user:update"};
    public static final String[] INTERMEDIARIO2_AUTHORITIES = {"user:read", "user:update"};
    public static final String[] ADMINISTRADOR_AUTHORITIES = {"user:read", "user:create", "user:update"};
    public static final String[] SUPER_ADMINISTRADOR_AUTHORITIES = {"user:read", "user:create", "user:update", "user:delete"};
}
