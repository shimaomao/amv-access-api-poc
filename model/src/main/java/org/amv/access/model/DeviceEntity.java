package org.amv.access.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.amv.access.core.Device;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
@Entity
@Table(
        name = "device",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"serial_number"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class DeviceEntity implements Device {
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

    @Column(name = "device_name")
    private String name;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "public_key")
    private String publicKey;

    @Tolerate
    protected DeviceEntity() {

    }

    @Override
    public String toString() {
        return String.format("DeviceEntity[id=%d, name='%s', serialNumber='%s']", id, name, serialNumber);
    }
}
