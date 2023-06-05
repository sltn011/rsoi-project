package ru.rsoi.Accounts.Model;

import jakarta.persistence.*;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Entity
@Table(name="accounts")
public class MAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id = -1;

    @Column(name = "acc_uid", columnDefinition = "uuid UNIQUE NOT NULL")
    public UUID accUid;

    @Column(name = "username", length = 20, nullable = false)
    public String username;

    @Column(name = "password", length = 20, nullable = false)
    public String password;

    @Column(name = "role", nullable = false)
    public String role;

    public MAccount() {accUid = UUID.randomUUID(); role = "user";}

    public int getId() {return id;}

    public MAccount(UUID accUid, String username, String password, String role) {
        this.accUid = accUid;
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
