package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.amv.access.core.Issuer;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.util.Date;
import java.util.Optional;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
@Entity
@Table(
        name = "issuer"
)
public class IssuerEntity implements Issuer {
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

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name", length = 4)
    private String name;

    @Column(name = "description", length = 1023)
    private String description;

    @Column(name = "public_key_base64")
    @JsonProperty(value = "public_key_base64")
    private String publicKeyBase64;

    /**
     * Only used for demo issuer
     */
    @JsonProperty(value = "private_key_base64", access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "private_key_base64")
    @Convert(converter = CryptoConverter.class)
    private String privateKeyBase64;

    @Default
    @Column(name = "enabled", columnDefinition = "integer DEFAULT 1")
    private boolean enabled = true;

    @Tolerate
    protected IssuerEntity() {

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    @Override
    public Optional<String> getPrivateKeyBase64() {
        return Optional.ofNullable(privateKeyBase64);
    }

    @Override
    public String toString() {
        return String.format("IssuerEntity[id=%d, name='%s']", id, name);
    }
}
