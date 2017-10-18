package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;
import org.amv.access.core.Application;

import javax.persistence.*;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
@Entity
@Table(
        name = "application",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"app_id"}),
                @UniqueConstraint(columnNames = {"api_key"})
        }
)
public class ApplicationEntity implements Application {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "bigint")
    @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "name", length = 63)
    private String name;

    @JsonProperty("app_id")
    @Column(name = "app_id", length = 24)
    private String appId;

    @JsonProperty(value = "api_key", access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "api_key")
    private String apiKey;

    @Default
    @Column(name = "enabled")
    private boolean enabled = true;

    @Tolerate
    protected ApplicationEntity() {

    }

    @Override
    public String toString() {
        return String.format("ApplicationEntity[id=%d, name='%s']", id, name);
    }
}
