package com.nearkirana.backend.entity;

/**
 * The two kinds of NearKirana users.
 * Stored in the database as text ("CUSTOMER" / "SHOPKEEPER"), matching ck_users_role.
 */
public enum Role {
    CUSTOMER,
    SHOPKEEPER
}
