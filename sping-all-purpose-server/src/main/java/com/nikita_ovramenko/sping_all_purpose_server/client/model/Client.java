package com.nikita_ovramenko.sping_all_purpose_server.client.model;

import java.util.ArrayList;
import java.util.List;

import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A person requesting work.
 *
 * <p>Note there is no quotes collection. Quotes used to be persisted by cascading
 * from clientRepo.save(client), which hid the write entirely; quotes are now saved
 * explicitly and read via QuoteRepo.findByClientId.
 */
@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Unique: ClientRepo looks clients up by email and returns an Optional. */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @OneToMany(mappedBy = "client", fetch = FetchType.LAZY,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<Location> locations = new ArrayList<>();

    public String fullName() {
        return firstName + " " + lastName;
    }
}
