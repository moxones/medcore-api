package com.medical.medcore.identity.domain;

import com.medical.medcore.shared.audit.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Table(name = "persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "gender")
    private String gender;

    @Column(name = "phone")
    private String phone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "address")
    private String address;

    @Column(name = "district", length = 100)
    private String district;

    @Builder.Default
    @Column(name = "profile_completed", nullable = false)
    private Boolean profileCompleted = false;

    @Builder.Default
    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PersonDocument> documents = new ArrayList<>();

    public String fullName() {
        String joined = Stream.of(firstName, lastName)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        return joined.isEmpty() ? null : joined;
    }

    public PersonDocument primaryDocument() {
        return documents == null || documents.isEmpty() ? null : documents.get(0);
    }

    public void refreshProfileCompleted() {
        profileCompleted = hasText(firstName) && hasText(lastName) && hasText(phone) && hasText(gender)
                && birthDate != null && hasText(contactEmail) && primaryDocument() != null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
