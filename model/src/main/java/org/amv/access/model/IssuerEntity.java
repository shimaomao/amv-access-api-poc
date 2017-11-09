package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
@Entity
@Table(
        name = "issuer"
)
public class IssuerEntity implements Issuer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "bigint")
    @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", insertable = true, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @JsonProperty(value = "created_at")
    private Date createdAt;

    @Column(name = "name", length = 4)
    private String name;

    @Column(name = "public_key_base64")
    private String publicKeyBase64;

    @JsonIgnore
    @Column(name = "private_key_base64")
    @Convert(converter = CryptoConverter.class)
    private String privateKeyBase64;

    @Default
    @Column(name = "enabled")
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
    public String toString() {
        return String.format("IssuerEntity[id=%d, name='%s']", id, name);
    }
}
