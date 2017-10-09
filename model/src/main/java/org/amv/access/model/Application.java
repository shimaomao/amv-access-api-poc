package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;

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
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "bigint")
    private Long id;

    @Column(name = "name")
    private String name;

    @JsonProperty("app_id")
    @Column(name = "app_id")
    private String appId;

    @JsonIgnore
    @Column(name = "api_key")
    private String apiKey;

    @Default
    @Column(name = "enabled")
    private boolean enabled = true;

    @Tolerate
    protected Application() {

    }

    @Override
    public String toString() {
        return String.format("User[id=%d, name='%s']", id, name);
    }
}
