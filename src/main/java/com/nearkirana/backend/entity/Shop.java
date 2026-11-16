package com.nearkirana.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row of the "shops" table: a Kirana shop owned by one SHOPKEEPER.
 *
 * location is a PostGIS GEOGRAPHY point. In Java it is a JTS "Point"
 * (hibernate-spatial converts between the two).
 * IMPORTANT: a Point stores X = longitude, Y = latitude (not the other way round).
 */
@Entity
@Table(name = "shops")
public class Shop {

    /** SRID 4326 = WGS 84, the latitude/longitude system used by GPS and Google Maps. */
    public static final int WGS84_SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), WGS84_SRID);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The shopkeeper's user id. Set from the JWT on create, never from the request body. */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "contact_number", nullable = false, length = 15)
    private String contactNumber;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(name = "village_or_city", nullable = false, length = 100)
    private String villageOrCity;

    @Column(length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 6)
    private String pincode;

    // columnDefinition tells Hibernate the real column type (geography, not geometry)
    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "is_open", nullable = false)
    private boolean open = true;

    @Column(name = "delivery_available", nullable = false)
    private boolean deliveryAvailable;

    @Column(name = "delivery_radius_km", precision = 5, scale = 2)
    private BigDecimal deliveryRadiusKm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. Not for our own code. */
    protected Shop() {
    }

    public Shop(Long ownerId) {
        this.ownerId = ownerId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Copies all editable details in one go (used by create and update). */
    public void updateDetails(String name, String description, String contactNumber,
                              String addressLine, String villageOrCity, String district,
                              String state, String pincode, double latitude, double longitude,
                              boolean deliveryAvailable, BigDecimal deliveryRadiusKm) {
        this.name = name;
        this.description = description;
        this.contactNumber = contactNumber;
        this.addressLine = addressLine;
        this.villageOrCity = villageOrCity;
        this.district = district;
        this.state = state;
        this.pincode = pincode;
        this.location = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude)); // X=lng, Y=lat
        this.deliveryAvailable = deliveryAvailable;
        this.deliveryRadiusKm = deliveryRadiusKm;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    /** radius must already be checked by the service (null when delivery is off). */
    public void setDelivery(boolean deliveryAvailable, BigDecimal deliveryRadiusKm) {
        this.deliveryAvailable = deliveryAvailable;
        this.deliveryRadiusKm = deliveryRadiusKm;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getVillageOrCity() {
        return villageOrCity;
    }

    public String getDistrict() {
        return district;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    public double getLatitude() {
        return location.getY();
    }

    public double getLongitude() {
        return location.getX();
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isDeliveryAvailable() {
        return deliveryAvailable;
    }

    public BigDecimal getDeliveryRadiusKm() {
        return deliveryRadiusKm;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
