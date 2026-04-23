package com.medical.medcore.dto.response;

public class PersonResponse {

    private String firstName;
    private String lastName;
    private String phone;

    public PersonResponse(String firstName,
                          String lastName,
                          String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
}