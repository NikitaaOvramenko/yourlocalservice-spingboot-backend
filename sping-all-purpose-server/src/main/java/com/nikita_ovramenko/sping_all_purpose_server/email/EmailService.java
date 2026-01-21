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
import com.nikita_ovramenko.sping_all_purpose_server.quote.dto.QuoteDto;
import com.nikita_ovramenko.sping_all_purpose_server.quote.model.Quote;

import jakarta.transaction.Transactional;

@Service
public class EmailService {
        private final JavaMailSender javaMailSender;
        private final ClientRepo clientRepo;
        private final LocationRepo locationRepo;

        private Map<String, String> workTypeToEmail = Map.of(
                        "Junk Removal", "info@yourlocalservice.co",
                        "Appliance Repair", "info@yourlocalservice.co");

        public EmailService(JavaMailSender javaMailSender, ClientRepo clientRepo, LocationRepo locationRepo) {
                this.javaMailSender = javaMailSender;
                this.clientRepo = clientRepo;
                this.locationRepo = locationRepo;
        }

        @Transactional
        public EmailDto sendFormResponseToClient(Client client, Location location, Quote quote) {
                // -------------------------
                // Create Client + Location Entities
                // -------------------------
                System.out.println("SMTP USER=" + System.getenv("EMAIL_SENDER"));
                System.out.println("SMTP PASS length=" + System.getenv("SMTP_PASS").length());

                String sender = workTypeToEmail.get(quote.getWorkType());

                // -------------------------
                // Email to Client
                // -------------------------
                SimpleMailMessage mailForCustomer = new SimpleMailMessage();

                mailForCustomer.setFrom(sender);
                mailForCustomer.setTo(client.getEmail());
                mailForCustomer.setSubject("Your Appointment Request Has Been Received");

                mailForCustomer.setText(
                                "Hi " + client.getName() + ",\n\n" +
                                                "Thank you for submitting your appointment request.\n" +
                                                "Here are the details we received:\n\n" +

                                                "Full Name: " + client.getName() + " " + client.getLastname() + "\n" +
                                                "Email: " + client.getEmail() + "\n" +
                                                "Phone: " + client.getPhone() + "\n\n" +

                                                "Requested Service Information:\n" +
                                                "• Work Type: " + quote.getWorkType() + "\n" +
                                                "• Service: " + quote.getServiceType() + "\n" +
                                                "• Description: " + quote.getDescription() + "\n\n" +

                                                "Location Details:\n" +
                                                "• Country: " + location.getCountry().toString() + "\n" +
                                                "• Town/City: " + location.getTown() + "\n" +
                                                "• Street: " + location.getStreet() + "\n" +
                                                "• Postal Code: " + location.getPostalCode() + "\n\n" +

                                                "We will contact you shortly to confirm the final appointment time.\n\n"
                                                +
                                                "Best regards,\n" +
                                                "YourLocal" + quote.getWorkType().replaceAll("\\s+", ""));

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
                                                "• Name: " + client.getName() + " " + client.getLastname() + "\n" +
                                                "• Email: " + client.getEmail() + "\n" +
                                                "• Phone: " + client.getPhone() + "\n\n" +

                                                "Requested Work Details:\n" +
                                                "• Work Type: " + quote.getWorkType() + "\n" +
                                                "• Service: " + quote.getServiceType() + "\n" +
                                                "• Description: " + quote.getDescription() + "\n\n" +

                                                "Location Details:\n" +
                                                "• Country: " + location.getCountry() + "\n" +
                                                "• Town/City: " + location.getTown() + "\n" +
                                                "• Street: " + location.getStreet() + "\n" +
                                                "• Postal Code: " + location.getPostalCode() + "\n\n" +

                                                "");

                javaMailSender.send(mailForBusiness);

                // -------------------------
                // Return DTO
                // -------------------------
                return new EmailDto(
                                client.getEmail(),
                                "Emails sent successfully.");
        }

}
