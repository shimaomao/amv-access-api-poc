package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.amv.access.util.MoreBase64;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder", toBuilder = true)
@Entity
@Table(name = "access_certificate")
@EntityListeners(AuditingEntityListener.class)
public class AccessCertificateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigint", updatable = false, nullable = false)
    @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
    private Long id;

    //@GeneratedValue(generator = "uuid2")
    //@GenericGenerator(name = "uuid2", strategy = "uuid2")
    //@Column(columnDefinition = "BINARY(16)")
    //private UUID uuid;
    @Column(name = "uuid")
    private String uuid;

    @CreatedDate
    @Column(name = "created_at", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @JsonProperty(value = "created_at")
    private Date createdAt;

    @Column(name = "issuer_id")
    private long issuerId;

    @Column(name = "application_id")
    private long applicationId;

    @Column(name = "device_id")
    private long deviceId;

    @Column(name = "vehicle_id")
    private long vehicleId;

    @Column(name = "valid_from")
    private Date validFrom;

    @Column(name = "valid_until")
    private Date validUntil;

    @Column(name = "vehicle_access_certificate_base64")
    private String vehicleAccessCertificateBase64;

    @Column(name = "device_access_certificate_base64")
    private String deviceAccessCertificateBase64;

    @Column(name = "vehicle_access_certificate_signature_base64")
    private String vehicleAccessCertificateSignatureBase64;

    @Column(name = "device_access_certificate_signature_base64")
    private String deviceAccessCertificateSignatureBase64;

    @Tolerate
    protected AccessCertificateEntity() {

    }

    @JsonIgnore
    public boolean isExpired() {
        return Optional.ofNullable(this.getValidUntil())
                .map(Date::toInstant)
                .map(i -> i.atZone(ZoneOffset.UTC))
                .map(i -> i.isBefore(ZonedDateTime.now(ZoneOffset.UTC)))
                .orElse(true);
    }

    public Optional<String> getVehicleAccessCertificateSignatureBase64() {
        return Optional.ofNullable(vehicleAccessCertificateSignatureBase64);
    }

    public Optional<String> getSignedVehicleAccessCertificateBase64() {
        return getVehicleAccessCertificateSignatureBase64()
                .map(MoreBase64::decodeBase64AsHex)
                .map(signatureHex -> MoreBase64.decodeBase64AsHex(vehicleAccessCertificateBase64) + signatureHex)
                .map(MoreBase64::encodeHexAsBase64);
    }

    public Optional<String> getDeviceAccessCertificateSignatureBase64() {
        return Optional.ofNullable(deviceAccessCertificateSignatureBase64);
    }

    public Optional<String> getSignedDeviceAccessCertificateBase64() {
        return getDeviceAccessCertificateSignatureBase64()
                .map(MoreBase64::decodeBase64AsHex)
                .map(signatureHex -> MoreBase64.decodeBase64AsHex(deviceAccessCertificateBase64) + signatureHex)
                .map(MoreBase64::encodeHexAsBase64);
    }
}
