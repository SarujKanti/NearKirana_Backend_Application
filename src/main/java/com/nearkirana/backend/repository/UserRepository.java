package com.nearkirana.backend.repository;

import com.nearkirana.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository = the layer that talks to the database.
 * Spring Data writes the implementation for us at startup, based on the
 * method names (findByMobileNumber -> SELECT ... WHERE mobile_number = ?).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMobileNumber(String mobileNumber);
}
