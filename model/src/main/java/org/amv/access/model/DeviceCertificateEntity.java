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

    @Column(name = "certificate_base64")
    private String certificateBase64;

    @Column(name = "certificate_signature_base64")
    private String certificateSignatureBase64;

    @Column(name = "full_certificate_base64")
    private String fullCertificateBase64;

    @Tolerate
    protected DeviceCertificateEntity() {

    }
}
