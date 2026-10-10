package com.delivery.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.api.model.Hub;

public interface HubRepository extends JpaRepository<Hub, String> {
}
