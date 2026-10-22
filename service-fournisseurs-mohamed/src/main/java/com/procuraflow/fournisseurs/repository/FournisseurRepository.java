package com.procuraflow.fournisseurs.repository;

import com.procuraflow.fournisseurs.entity.Fournisseur;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FournisseurRepository
        extends JpaRepository<Fournisseur, Long>, JpaSpecificationExecutor<Fournisseur> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Fournisseur> findByEmailIgnoreCase(String email);
}
