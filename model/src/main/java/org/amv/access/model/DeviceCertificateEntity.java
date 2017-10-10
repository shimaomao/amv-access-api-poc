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
                @UniqueConstraint(columnNames = {"device_id"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class DeviceCertificateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "bigint")
    private Long id;

    @CreatedDate
    @Column(name = "created", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "application_id")
    private long applicationId;

    @Column(name = "issuer_id")
    private long issuerId;

    @Column(name = "device_id")
    private long deviceId;

    @Column(name = "certificate_base64")
    private String certificateBase64;

    @Column(name = "signed_certificate_base64")
    private String signedCertificateBase64;

    @Tolerate
    protected DeviceCertificateEntity() {

    }
}
