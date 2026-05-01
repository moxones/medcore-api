package com.medical.medcore.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "person_documents",
        uniqueConstraints = @UniqueConstraint(name = "uq_pd_document",
                columnNames = {"document_type_id", "document_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;
}