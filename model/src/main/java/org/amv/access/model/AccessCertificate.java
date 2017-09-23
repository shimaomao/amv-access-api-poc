package org.amv.access.model;

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
public class AccessCertificate {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "bigint")
    private Long id;

    @CreatedDate
    @Column(name = "created", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "app_id")
    private String appId;

    @Column(name = "device_serial_number")
    private String deviceSerialNumber;

    @Column(name = "vehicle_serial_number")
    private String vehicleSerialNumber;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "vehicle_access_certificate")
    private String vehicleCertificate;

    @Column(name = "signed_vehicle_access_certificate_base64")
    private String signedVehicleCertificateBase64;

    @Column(name = "device_access_certificate")
    private String deviceCertificate;

    @Column(name = "signed_access_certificate_base64")
    private String signedDeviceCertificateBase64;


    @Tolerate
    protected AccessCertificate() {

    }
}
