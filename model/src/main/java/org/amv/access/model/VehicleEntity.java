package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.amv.access.core.Vehicle;
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
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "bigint")
    @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @CreatedDate
    @Column(name = "created", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @JsonProperty(value = "created", access = JsonProperty.Access.READ_ONLY)
    private Date created;

    @Column(name = "issuer_id")
    @JsonProperty(value = "issuer_id", access = JsonProperty.Access.READ_ONLY)
    private long issuerId;

    @Column(name = "name", length = 63)
    private String name;

    @Column(name = "serial_number"/*, length = 18*/)
    @JsonProperty(value = "serial_number", access = JsonProperty.Access.READ_ONLY)
    private String serialNumber;

    @Column(name = "public_key_base64")
    @JsonProperty(value = "public_key_base64", access = JsonProperty.Access.READ_ONLY)
    private String publicKeyBase64;

    @Tolerate
    protected VehicleEntity() {

    }

    @Override
    public String toString() {
        return String.format("VehicleEntity[id=%d, name='%s', serialNumber='%s']", id, name, serialNumber);
    }
}
