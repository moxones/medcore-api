package com.medical.medcore.identity.service;

import com.medical.medcore.catalog.domain.DocumentType;
import com.medical.medcore.catalog.repository.DocumentTypeRepository;
import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.PersonDocument;
import com.medical.medcore.identity.repository.PersonDocumentRepository;
import com.medical.medcore.identity.repository.PersonRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PersonRegistry {

    private static final String CODE_DOCUMENT_TAKEN = "DOCUMENT_ALREADY_REGISTERED";
    private static final String DNI = "DNI";
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern GENERIC_DOCUMENT_PATTERN = Pattern.compile("^[A-Za-z0-9-]{5,20}$");
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_AGE_YEARS = 120;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?\\(?[0-9][0-9 ()-]{5,19}$");
    private static final Map<String, String> GENDER_ALIASES = Map.ofEntries(
            Map.entry("M", "M"), Map.entry("MALE", "M"), Map.entry("MASCULINO", "M"),
            Map.entry("F", "F"), Map.entry("FEMALE", "F"), Map.entry("FEMENINO", "F"),
            Map.entry("OTHER", "OTHER"), Map.entry("OTRO", "OTHER"), Map.entry("O", "OTHER"));

    private final PersonRepository personRepository;
    private final PersonDocumentRepository personDocumentRepository;
    private final DocumentTypeRepository documentTypeRepository;

    @Transactional(readOnly = true)
    public Optional<Person> findByDocument(Long tenantId, String documentTypeCode, String documentNumber) {
        DocumentType type = requireDocumentType(documentTypeCode);
        String number = normalizeDocumentNumber(type, documentNumber);
        return personDocumentRepository
                .findByTenantIdAndDocumentType_IdAndDocumentNumber(tenantId, type.getId(), number)
                .map(PersonDocument::getPerson);
    }

    @Transactional
    public Person create(Long tenantId, PersonData data) {
        if (!data.hasDocument()) {
            throw new BadRequestException("El tipo y número de documento son obligatorios");
        }
        DocumentType type = requireDocumentType(data.documentTypeCode());
        String number = normalizeDocumentNumber(type, data.documentNumber());
        assertDocumentAvailable(tenantId, type, number, null);

        Person person = Person.builder()
                .tenantId(tenantId)
                .firstName(cleanName(data.firstName()))
                .lastName(cleanName(data.lastName()))
                .birthDate(validBirthDate(data.birthDate()))
                .gender(normalizeGender(data.gender()))
                .phone(validPhone(data.phone()))
                .contactEmail(normalizeEmail(data.contactEmail()))
                .address(clean(data.address()))
                .district(clean(data.district()))
                .build();
        person = personRepository.save(person);

        PersonDocument document = personDocumentRepository.save(PersonDocument.builder()
                .tenantId(tenantId)
                .person(person)
                .documentType(type)
                .documentNumber(number)
                .build());
        person.getDocuments().add(document);
        person.refreshProfileCompleted();
        return personRepository.save(person);
    }

    @Transactional
    public void applyChanges(Person person, PersonData data) {
        if (data.firstName() != null) {
            person.setFirstName(requireName(data.firstName(), "El nombre"));
        }
        if (data.lastName() != null) {
            person.setLastName(requireName(data.lastName(), "El apellido"));
        }
        if (data.birthDate() != null) {
            person.setBirthDate(validBirthDate(data.birthDate()));
        }
        if (data.gender() != null) {
            person.setGender(normalizeGender(data.gender()));
        }
        if (data.phone() != null) {
            person.setPhone(validPhone(data.phone()));
        }
        if (data.contactEmail() != null) {
            person.setContactEmail(normalizeEmail(data.contactEmail()));
        }
        if (data.address() != null) {
            person.setAddress(clean(data.address()));
        }
        if (data.district() != null) {
            person.setDistrict(clean(data.district()));
        }
        if (data.hasDocument()) {
            applyDocument(person, data.documentTypeCode(), data.documentNumber());
        }
        person.refreshProfileCompleted();
        personRepository.save(person);
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String requireName(String value, String label) {
        String cleaned = cleanName(value);
        if (cleaned == null) {
            throw new BadRequestException(label + " es obligatorio");
        }
        return cleaned;
    }

    private void applyDocument(Person person, String documentTypeCode, String documentNumber) {
        DocumentType type = requireDocumentType(documentTypeCode);
        String number = normalizeDocumentNumber(type, documentNumber);
        assertDocumentAvailable(person.getTenantId(), type, number, person.getId());

        PersonDocument current = person.primaryDocument();
        if (current == null) {
            PersonDocument created = personDocumentRepository.save(PersonDocument.builder()
                    .tenantId(person.getTenantId())
                    .person(person)
                    .documentType(type)
                    .documentNumber(number)
                    .build());
            person.getDocuments().add(created);
            return;
        }
        current.setDocumentType(type);
        current.setDocumentNumber(number);
        personDocumentRepository.save(current);
    }

    private void assertDocumentAvailable(Long tenantId, DocumentType type, String number, Long ownerPersonId) {
        personDocumentRepository
                .findByTenantIdAndDocumentType_IdAndDocumentNumber(tenantId, type.getId(), number)
                .filter(existing -> !existing.getPerson().getId().equals(ownerPersonId))
                .ifPresent(existing -> {
                    throw new ConflictException(CODE_DOCUMENT_TAKEN, "El documento ya se encuentra registrado");
                });
    }

    private DocumentType requireDocumentType(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("El tipo de documento es obligatorio");
        }
        return documentTypeRepository.findByCode(code.trim().toUpperCase())
                .filter(type -> !Boolean.FALSE.equals(type.getIsActive()))
                .orElseThrow(() -> new BadRequestException("Tipo de documento inválido"));
    }

    private String normalizeDocumentNumber(DocumentType type, String rawNumber) {
        if (rawNumber == null || rawNumber.isBlank()) {
            throw new BadRequestException("El número de documento es obligatorio");
        }
        String number = rawNumber.trim().toUpperCase();
        Pattern pattern = DNI.equals(type.getCode()) ? DNI_PATTERN : GENERIC_DOCUMENT_PATTERN;
        if (!pattern.matcher(number).matches()) {
            throw new BadRequestException(DNI.equals(type.getCode())
                    ? "El DNI debe tener 8 dígitos"
                    : "El número de documento debe tener entre 5 y 20 caracteres alfanuméricos");
        }
        return number;
    }

    private LocalDate validBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            throw new BadRequestException("La fecha de nacimiento no puede ser futura");
        }
        if (birthDate.isBefore(today.minusYears(MAX_AGE_YEARS))) {
            throw new BadRequestException("La fecha de nacimiento no es válida");
        }
        return birthDate;
    }

    private String normalizeGender(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        String normalized = GENDER_ALIASES.get(cleaned.toUpperCase());
        if (normalized == null) {
            throw new BadRequestException("INVALID_GENDER", "El sexo debe ser M, F u OTHER");
        }
        return normalized;
    }

    private String validPhone(String value) {
        String cleaned = clean(value);
        if (cleaned != null && !PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new BadRequestException("INVALID_PHONE", "El teléfono debe tener entre 6 y 20 dígitos");
        }
        return cleaned;
    }

    private String cleanName(String value) {
        String cleaned = clean(value);
        if (cleaned != null && cleaned.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException("El nombre o apellido supera los " + MAX_NAME_LENGTH + " caracteres");
        }
        return cleaned;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
