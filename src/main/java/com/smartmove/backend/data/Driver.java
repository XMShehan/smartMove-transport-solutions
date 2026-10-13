package com.smartmove.backend.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Locale;

@Entity
@Table(name = "DRIVER")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "nic", nullable = false, length = 20)
    private String nic;

    @Column(name = "license_no", nullable = false, length = 40)
    private String licenseNo;

    @Column(name = "phone", length = 25)
    private String phone;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    protected Driver() { }

    public Driver(Long managerId, String name, String password, String nic,
                  String licenseNo, String phone, String email,
                  LocalDate hireDate, String status) {
        this.managerId = managerId;
        this.name = name;
        this.password = password;
        this.nic = nic;
        this.licenseNo = licenseNo;
        this.phone = phone;
        this.email = email;
        this.hireDate = hireDate;
        this.status = (status == null || status.isBlank())
                ? "ACTIVE" : status.trim().toUpperCase(Locale.ROOT);
    }

    public Long getDriverId() { return driverId; }
    public Long getManagerId() { return managerId; }
    public String getName() { return name; }
    public String getNic() { return nic; }
    public String getLicenseNo() { return licenseNo; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalDate getHireDate() { return hireDate; }
    public String getStatus() { return status; }
}
