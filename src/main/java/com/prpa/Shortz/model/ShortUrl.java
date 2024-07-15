package com.prpa.Shortz.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.net.URL;
import java.time.Instant;

@Entity @Data
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(
        name = "ShortUrl",
        uniqueConstraints = @UniqueConstraint(name = "slug_url_constraint", columnNames = { "owner", "slug", "url" })
)
public class ShortUrl {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @NotNull @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", updatable = false, referencedColumnName = "id")
    private ShortzUser owner;

    @CreationTimestamp
    @Column(name = "creation_timestamp", updatable = false)
    private Instant creationTimestamp;

    @NotNull
    @Column(name = "hit")
    private Integer hit;

    @NotNull @NotBlank
    @Column(name = "slug", updatable = false, unique = true)
    private String slug;

    @NotNull
    @Column(name = "url", updatable = false)
    private URL url;

}
