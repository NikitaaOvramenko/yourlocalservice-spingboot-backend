package com.nikita_ovramenko.sping_all_purpose_server.quote.model;

import java.util.ArrayList;
import java.util.List;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.quote.status.QuoteStatus;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Quote {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ElementCollection
    @CollectionTable(name = "quote_service_types", joinColumns = @JoinColumn(name = "quote_id"))
    @Column(name = "service_type")
    private List<String> serviceType = new ArrayList<>();

    private String description;
    private String workType;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @ElementCollection
    @CollectionTable(name = "quote_pictures", joinColumns = @JoinColumn(name = "quote_id"))
    @Column(name = "pictures")
    private List<String> pictures = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private QuoteStatus status;

    public Quote() {
    }

    public Quote(long id, Client client, String description, String workType, QuoteStatus status, Location location) {
        this.id = id;
        this.client = client;
        this.description = description;
        this.workType = workType;
        this.location = location;
        this.status = status;
    }

    public Quote(Client client, String description, String workType, QuoteStatus status) {
        this.client = client;
        this.description = description;
        this.workType = workType;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<String> getServiceType() {
        return serviceType;
    }

    public void setServiceType(List<String> serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public List<String> getPictures() {
        return pictures;
    }

    public void setPictures(List<String> pictures) {
        this.pictures = pictures;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public void setStatus(QuoteStatus status) {
        this.status = status;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

}
