package com.smartmove.backend.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "MANAGER")
public class Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "nic", nullable = false, length = 20)
    private String nic;

    @Column(name = "phone", length = 25)
    private String phone;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @JsonIgnore
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    protected Manager() { }

    public Manager(String name, String nic, String phone, String email, String password) {
        this.name = name;
        this.nic = nic;
        this.phone = phone;
        this.email = email;
        this.password = password;
    }

    public Long getManagerId() { return managerId; }
    public String getName() { return name; }
    public String getNic() { return nic; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}
