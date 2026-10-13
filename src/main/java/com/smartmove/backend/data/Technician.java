package com.smartmove.backend.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "TECHNICIAN")
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", length = 25)
    private String phone;

    @Column(name = "email", length = 254)
    private String email;

    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "specialization", nullable = false, length = 100)
    private String specialization;

    protected Technician() { }

    public Technician(Long managerId, String name, String phone, String email,
                      String password, LocalDate hireDate, String specialization) {
        this.managerId = managerId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.hireDate = hireDate;
        this.specialization = specialization;
    }

    public Long getTechnicianId() { return technicianId; }
    public Long getManagerId() { return managerId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalDate getHireDate() { return hireDate; }
    public String getSpecialization() { return specialization; }
}
