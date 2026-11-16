package com.nearkirana.backend.repository;

import com.nearkirana.backend.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data writes the SQL from the method names, for example:
 * findByIdAndOwnerId -> SELECT ... FROM shops WHERE id = ? AND owner_id = ?
 */
public interface ShopRepository extends JpaRepository<Shop, Long> {

    /** All shops of one shopkeeper, oldest first. */
    List<Shop> findByOwnerIdOrderByCreatedAtAsc(Long ownerId);

    /** A shop ONLY if it belongs to this owner. Other people's shops are "not found". */
    Optional<Shop> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);
}
