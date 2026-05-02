package com.medical.medcore.dto.response;

import java.time.LocalDate;

public class PersonResponse {

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String gender;
    private String phone;
    private String documentTypeCode;
    private String documentNumber;

    public PersonResponse(String firstName,
                          String lastName,
                          LocalDate birthDate,
                          String gender,
                          String phone,
                          String documentTypeCode,
                          String documentNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.phone = phone;
        this.documentTypeCode = documentTypeCode;
        this.documentNumber = documentNumber;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getDocumentTypeCode() { return documentTypeCode; }
    public String getDocumentNumber() { return documentNumber; }
}