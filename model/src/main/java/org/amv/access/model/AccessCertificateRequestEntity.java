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
@Table(name = "access_certificate_request")
@EntityListeners(AuditingEntityListener.class)
public class AccessCertificateRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "bigint")
    @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @CreatedDate
    @Column(name = "created", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "valid_from", updatable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", updatable = false)
    private LocalDateTime validUntil;

    @Column(name = "app_id", updatable = false/*, length = 24*/)
    private String appId;

    @Column(name = "device_serial_number", updatable = false/*, length = 18*/)
    private String deviceSerialNumber;

    @Column(name = "vehicle_serial_number", updatable = false/*, length = 18*/)
    private String vehicleSerialNumber;

    @Tolerate
    protected AccessCertificateRequestEntity() {

    }
}
