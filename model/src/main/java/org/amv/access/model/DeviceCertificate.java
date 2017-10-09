package org.amv.access.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
@Entity
@Table(
        name = "device_certificate",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"device_serial_number"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class DeviceCertificate {
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

    @Column(name = "certificate")
    private String certificate;

    @Column(name = "certificate_base64")
    private String certificateBase64;

    @Column(name = "signed_certificate_base64")
    private String signedCertificateBase64;

    @Column(name = "issuer_name")
    private String issuerName;

    @Column(name = "issuer_public_key_base64")
    private String issuerPublicKeyBase64;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_serial_number")
    private String deviceSerialNumber;

    @Tolerate
    protected DeviceCertificate() {

    }
}
