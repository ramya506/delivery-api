package com.delivery.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.api.model.Pincode;

public interface PincodeRepository extends JpaRepository<Pincode, String> {
}
