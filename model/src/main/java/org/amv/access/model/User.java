package org.amv.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;

import javax.persistence.*;
import java.util.List;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id", columnDefinition = "bigint")
    @JsonProperty(value = "id", access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "name")
    private String name;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password")
    @Convert(converter = CryptoConverter.class)
    private String password;

    @Default
    @Column(name = "enabled")
    private boolean enabled = true;

    @Default
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> authorities = Lists.newArrayList();

    @Tolerate
    protected User() {

    }

    @Override
    public String toString() {
        return String.format("User[id=%d, name='%s']", id, name);
    }
}
