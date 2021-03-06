package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.amv.access.core.Key;
import org.amv.access.core.SerialNumber;
import org.amv.access.core.Vehicle;
import org.amv.access.core.impl.KeyImpl;
import org.amv.access.core.impl.SerialNumberImpl;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
@Entity
@Table(
        name = "vehicle",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"serial_number"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class VehicleEntity implements Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigint", updatable = false, nullable = false)
    @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @JsonProperty(value = "created_at")
    private Date createdAt;

    @Column(name = "issuer_id")
    @JsonProperty(value = "issuer_id")
    private long issuerId;

    @Column(name = "name", length = 63)
    private String name;

    @Column(name = "description", length = 1023)
    private String description;

    @Column(name = "serial_number"/*, length = 18*/)
    @JsonProperty(value = "serial_number")
    private String serialNumber;

    @Column(name = "public_key_base64")
    @JsonProperty(value = "public_key_base64")
    private String publicKeyBase64;

    @Default
    @Column(name = "enabled", columnDefinition = "integer DEFAULT 1")
    private boolean enabled = true;

    @Tolerate
    protected VehicleEntity() {

    }

    @Override
    public SerialNumber getSerialNumber() {
        return SerialNumberImpl.fromHex(serialNumber);
    }

    @Override
    public Key getPublicKey() {
        return KeyImpl.fromBase64(publicKeyBase64);
    }

    @Override
    public String toString() {
        return String.format("VehicleEntity[id=%d, name='%s', serialNumber='%s']", id, name, serialNumber);
    }
}
