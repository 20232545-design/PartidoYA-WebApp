package com.partidoya.platform.users.models;

public enum Plan {
    JUGADOR_BASICO(Role.JUGADOR),
    JUGADOR_PLUS(Role.JUGADOR),
    CANCHA_SIN_PLAN(Role.ADMIN_CANCHA),
    CANCHA_EMPRENDEDOR(Role.ADMIN_CANCHA),
    CANCHA_BUSINESS(Role.ADMIN_CANCHA),
    ADMIN_GENERAL(Role.ADMIN_GENERAL);

    private final Role role;

    Plan(Role role) {
        this.role = role;
    }

    public boolean isCompatibleWith(Role role) {
        return this.role == role;
    }

    public static Plan defaultForRole(Role role) {
        if (role == Role.ADMIN_GENERAL) return ADMIN_GENERAL;
        return role == Role.ADMIN_CANCHA ? CANCHA_SIN_PLAN : JUGADOR_BASICO;
    }
}
