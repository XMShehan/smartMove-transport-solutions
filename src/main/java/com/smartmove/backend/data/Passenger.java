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
@Table(name = "PASSENGER")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passenger_id")
    private Long passengerId;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "nic", length = 20)
    private String nic;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "phone", length = 25)
    private String phone;

    @Column(name = "passenger_type", nullable = false, length = 30)
    private String passengerType;

    // Oracle supplies SYSDATE on insert; JPA reads this column on retrieval.
    @Column(name = "registered_date", insertable = false, updatable = false)
    private LocalDate registeredDate;

    protected Passenger() { }

    public Passenger(Long managerId, String name, String nic, String email,
                     String password, String phone, String passengerType) {
        this.managerId = managerId;
        this.name = name;
        this.nic = nic;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.passengerType = passengerType;
    }

    public Long getPassengerId() { return passengerId; }
    public Long getManagerId() { return managerId; }
    public String getName() { return name; }
    public String getNic() { return nic; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassengerType() { return passengerType; }
    public LocalDate getRegisteredDate() { return registeredDate; }
}
