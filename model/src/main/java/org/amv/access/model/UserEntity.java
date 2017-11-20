package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.util.Date;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
@Entity
@Table(name = "user")
public class UserEntity {
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

    @Column(name = "name")
    private String name;

    @Column(name = "description", length = 1023)
    private String description;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password")
    @Convert(converter = CryptoConverter.class)
    private String password;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "salt")
    private String salt;

    @Default
    @Column(name = "enabled", columnDefinition = "integer DEFAULT 1")
    private boolean enabled = true;

    @Tolerate
    protected UserEntity() {

    }

    @Override
    public String toString() {
        return String.format("UserEntity[id=%d, name='%s']", id, name);
    }
}
