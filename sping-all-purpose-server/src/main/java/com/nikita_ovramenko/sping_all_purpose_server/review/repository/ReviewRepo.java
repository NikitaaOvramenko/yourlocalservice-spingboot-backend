package com.nikita_ovramenko.sping_all_purpose_server.review.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nikita_ovramenko.sping_all_purpose_server.review.model.Review;

@Repository
public interface ReviewRepo extends JpaRepository<Review, Long> {
}
