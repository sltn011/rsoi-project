package ru.rsoi.Accounts.Model;

import jakarta.persistence.*;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name="accounts")
public class MAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "accuid", columnDefinition = "uuid UNIQUE NOT NULL")
    public UUID accuid;

    @Column(name = "username", length = 20, nullable = false)
    public String username;

    @Column(name = "email", length = 20, nullable = false)
    public String email;

    @Column(name = "password", length = 20, nullable = false)
    public String password;

    @Column(name = "role", nullable = false)
    public String role;

    public MAccount() {accuid = UUID.randomUUID(); role = "USER";}

    public int getId() {return id;}

    public MAccount(UUID accUid, String username, String email, String password, String role) {
        this.accuid = accUid;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
