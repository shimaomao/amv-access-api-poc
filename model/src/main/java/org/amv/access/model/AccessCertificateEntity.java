package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
@Entity
@Table(name = "access_certificate")
@EntityListeners(AuditingEntityListener.class)
public class AccessCertificateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "bigint")
    @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
    private Long id;

    //@GeneratedValue(generator = "uuid2")
    //@GenericGenerator(name = "uuid2", strategy = "uuid2")
    //@Column(columnDefinition = "BINARY(16)")
    //private UUID uuid;
    @Column(name = "uuid")
    private String uuid;

    @CreatedDate
    @Column(name = "created", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "issuer_id")
    private long issuerId;

    @Column(name = "application_id")
    private long applicationId;

    @Column(name = "device_id")
    private long deviceId;

    @Column(name = "vehicle_id")
    private long vehicleId;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "signed_vehicle_access_certificate_base64")
    private String signedVehicleAccessCertificateBase64;

    @Column(name = "signed_access_certificate_base64")
    private String signedDeviceAccessCertificateBase64;

    @Tolerate
    protected AccessCertificateEntity() {

    }
}
