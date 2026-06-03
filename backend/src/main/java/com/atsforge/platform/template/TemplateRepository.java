package com.atsforge.platform.template;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<TemplateEntity, UUID> {
    Optional<TemplateEntity> findByCodeAndActiveTrue(String code);
    List<TemplateEntity> findAllByActiveTrueOrderByPremiumAscNameAsc();
}

