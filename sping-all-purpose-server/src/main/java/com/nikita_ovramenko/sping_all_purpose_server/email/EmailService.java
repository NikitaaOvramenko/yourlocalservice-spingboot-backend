package com.nikita_ovramenko.sping_all_purpose_server.email;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.nikita_ovramenko.sping_all_purpose_server.client.model.Client;
import com.nikita_ovramenko.sping_all_purpose_server.client.repository.ClientRepo;
import com.nikita_ovramenko.sping_all_purpose_server.location.enums.Country;
import com.nikita_ovramenko.sping_all_purpose_server.location.model.Location;
import com.nikita_ovramenko.sping_all_purpose_server.location.repository.LocationRepo;

import jakarta.transaction.Transactional;

@Service
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final ClientRepo clientRepo;
    private final LocationRepo locationRepo;

    private Map<String, String> workTypeToEmail = Map.of(
            "Junk Removal", "ovramenko.nikitka@gmail.com",
            "Appliance Repair", "tcspaintsfl@gmail.com");

    public EmailService(JavaMailSender javaMailSender, ClientRepo clientRepo, LocationRepo locationRepo) {
        this.javaMailSender = javaMailSender;
        this.clientRepo = clientRepo;
        this.locationRepo = locationRepo;
    }

    @Transactional
    public EmailDto sendFormResponseToClient(FormDto formDto) {
        // -------------------------
        // Create Client + Location Entities
        // -------------------------
        Client client = new Client();
        List<Location> locations = new ArrayList<>();
        Location location = new Location();
        String sender = workTypeToEmail.get(formDto.workType());

        client.setEmail(formDto.email());
        client.setName(formDto.name());
        client.setLastname(formDto.lastname());

        location.setCountry(Country.valueOf(formDto.country()));
        location.setTown(formDto.town());
        location.setStreet(formDto.street());
        location.setPostalCode(formDto.postal_code());

        locations.add(location);
        client.setLocations(locations);
        location.setClient(client);

        // Save to DB
        clientRepo.save(client);
        locationRepo.save(location);

        // -------------------------
        // Email to Client
        // -------------------------
        SimpleMailMessage mailForCustomer = new SimpleMailMessage();

        mailForCustomer.setFrom(sender);
        mailForCustomer.setTo(formDto.email());
        mailForCustomer.setSubject("Your Appointment Request Has Been Received");

        mailForCustomer.setText(
                "Hi " + formDto.name() + ",\n\n" +
                        "Thank you for submitting your appointment request.\n" +
                        "Here are the details we received:\n\n" +

                        "Full Name: " + formDto.name() + " " + formDto.lastname() + "\n" +
                        "Email: " + formDto.email() + "\n" +
                        "Phone: " + formDto.phone() + "\n\n" +

                        "Requested Service Information:\n" +
                        "• Work Type: " + formDto.workType() + "\n" +
                        "• Service: " + formDto.service() + "\n" +
                        "• Description: " + formDto.description() + "\n\n" +

                        "Location Details:\n" +
                        "• Country: " + formDto.country() + "\n" +
                        "• Town/City: " + formDto.town() + "\n" +
                        "• Street: " + formDto.street() + "\n" +
                        "• Postal Code: " + formDto.postal_code() + "\n\n" +

                        "We will contact you shortly to confirm the final appointment time.\n\n" +
                        "Best regards,\n" +
                        "TCS Paints Florida");

        javaMailSender.send(mailForCustomer);

        // -------------------------
        // Email to Business
        // -------------------------
        SimpleMailMessage mailForBusiness = new SimpleMailMessage();
        mailForBusiness.setFrom(sender);
        mailForBusiness.setTo(sender); // your own inbox

        mailForBusiness.setSubject("New Appointment Request Submitted");

        mailForBusiness.setText(
                "A new client has submitted an appointment request.\n\n" +

                        "Client Information:\n" +
                        "• Name: " + formDto.name() + " " + formDto.lastname() + "\n" +
                        "• Email: " + formDto.email() + "\n" +
                        "• Phone: " + formDto.phone() + "\n\n" +

                        "Requested Work Details:\n" +
                        "• Work Type: " + formDto.workType() + "\n" +
                        "• Service: " + formDto.service() + "\n" +
                        "• Description: " + formDto.description() + "\n\n" +

                        "Location Details:\n" +
                        "• Country: " + formDto.country() + "\n" +
                        "• Town/City: " + formDto.town() + "\n" +
                        "• Street: " + formDto.street() + "\n" +
                        "• Postal Code: " + formDto.postal_code() + "\n\n" +

                        "You can now follow up with the client.");

        javaMailSender.send(mailForBusiness);

        // -------------------------
        // Return DTO
        // -------------------------
        return new EmailDto(
                formDto.email(),
                "Emails sent successfully.");
    }

}
