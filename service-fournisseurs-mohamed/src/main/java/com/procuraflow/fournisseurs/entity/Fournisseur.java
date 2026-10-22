package com.procuraflow.fournisseurs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Entity
@Table(
        name = "fournisseurs",
        schema = "schema_fournisseurs",
        uniqueConstraints = {
                @UniqueConstraint(name = "fournisseurs_email_key", columnNames = "email")
        }
)
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "nom", length = 150, nullable = false)
    private String nom;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @Column(name = "adresse", columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "actif", nullable = false)
    private Boolean actif = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Fournisseur() {
    }

    public Fournisseur(String nom, String email, String telephone, String adresse) {
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.adresse = adresse;
        this.actif = Boolean.TRUE;
    }

    @PrePersist
    public void onPrePersist() {
        normalizeFields();
        if (this.actif == null) {
            this.actif = Boolean.TRUE;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    public void onPreUpdate() {
        normalizeFields();
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void normalizeFields() {
        if (this.nom != null) {
            this.nom = this.nom.trim();
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase(Locale.ROOT);
        }
        if (this.telephone != null) {
            String trimmedPhone = this.telephone.trim();
            this.telephone = trimmedPhone.isEmpty() ? null : trimmedPhone;
        }
        if (this.adresse != null) {
            String trimmedAddress = this.adresse.trim();
            this.adresse = trimmedAddress.isEmpty() ? null : trimmedAddress;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
