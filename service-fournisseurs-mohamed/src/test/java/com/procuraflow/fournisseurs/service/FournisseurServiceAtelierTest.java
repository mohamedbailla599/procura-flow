package com.procuraflow.fournisseurs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.procuraflow.fournisseurs.client.CommandesClient;
import com.procuraflow.fournisseurs.dto.FournisseurRequest;
import com.procuraflow.fournisseurs.dto.FournisseurResponse;
import com.procuraflow.fournisseurs.entity.Fournisseur;
import com.procuraflow.fournisseurs.exception.DuplicateEmailException;
import com.procuraflow.fournisseurs.exception.FournisseurNotFoundException;
import com.procuraflow.fournisseurs.repository.FournisseurRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FournisseurServiceAtelierTest {

    @Mock
    private FournisseurRepository fournisseurRepository;

    @Mock
    private CommandesClient commandesClient;

    @InjectMocks
    private FournisseurService service;

    // ---------- createFournisseur ----------
    
    @Test
    void testCreerFournisseurValide() {
        FournisseurRequest request = new FournisseurRequest(
                "Atlas Fournitures", "contact@atlas.ma", "+212600000000", "Agadir");
        when(fournisseurRepository.existsByEmailIgnoreCase("contact@atlas.ma")).thenReturn(false);
        when(fournisseurRepository.saveAndFlush(any(Fournisseur.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FournisseurResponse response = service.createFournisseur(request);

        assertEquals("Atlas Fournitures", response.nom());
        assertEquals("contact@atlas.ma", response.email());
        assertTrue(response.actif());
    }

    @Test
    void testCreerFournisseurEmailDejaUtiliseMemeEnMajuscules() {
        FournisseurRequest request = new FournisseurRequest(
                "Atlas", "  CONTACT@Atlas.MA ", null, null);
        when(fournisseurRepository.existsByEmailIgnoreCase("contact@atlas.ma")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.createFournisseur(request));
        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    @Test
    void testCreerFournisseurEmailNull() {
        FournisseurRequest request = new FournisseurRequest("Atlas", null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.createFournisseur(request));
        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    @Test
    void testCreerFournisseurNomComposeUniquementDEspaces() {
        FournisseurRequest request = new FournisseurRequest("   ", "x@atlas.ma", null, null);

        assertThrows(IllegalArgumentException.class, () -> service.createFournisseur(request));
        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    // ---------- updateFournisseur ----------

    @Test
    void testModifierFournisseurInexistant() {
        FournisseurRequest request = new FournisseurRequest("Nom", "nom@atlas.ma", null, null);
        when(fournisseurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FournisseurNotFoundException.class, () -> service.updateFournisseur(99L, request));
        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    // ---------- deactivate / reactivate ----------

    @Test
    void testDesactiverFournisseurSansLeSupprimer() {
        Fournisseur f = new Fournisseur("Souss Equipement", "contact@souss.ma", null, null);
        f.setId(7L);
        when(fournisseurRepository.findById(7L)).thenReturn(Optional.of(f));
        when(fournisseurRepository.saveAndFlush(any(Fournisseur.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FournisseurResponse response = service.deactivateFournisseur(7L);

        assertFalse(response.actif());
        verify(fournisseurRepository, never()).deleteById(any());
    }

    @Test
    void testReactiverFournisseur() {
        Fournisseur f = new Fournisseur("Souss Equipement", "contact@souss.ma", null, null);
        f.setId(7L);
        f.setActif(false);
        when(fournisseurRepository.findById(7L)).thenReturn(Optional.of(f));
        when(fournisseurRepository.saveAndFlush(any(Fournisseur.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FournisseurResponse response = service.reactivateFournisseur(7L);

        assertTrue(response.actif());
    }

        // ---------- updateFournisseur : entrées invalides ----------

    @Test
    void testModifierFournisseurEmailNull() {
        FournisseurRequest request = new FournisseurRequest("Atlas", null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.updateFournisseur(7L, request));
        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    @Test
    void testModifierFournisseurNomComposeUniquementDEspaces() {
        FournisseurRequest request = new FournisseurRequest("   ", "x@atlas.ma", null, null);

        assertThrows(IllegalArgumentException.class, () -> service.updateFournisseur(7L, request));
        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    // ---------- deactivate / reactivate : fournisseur introuvable ----------

    @Test
    void testDesactiverFournisseurInexistant() {
        when(fournisseurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FournisseurNotFoundException.class, () -> service.deactivateFournisseur(99L));
        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    @Test
    void testReactiverFournisseurInexistant() {
        when(fournisseurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FournisseurNotFoundException.class, () -> service.reactivateFournisseur(99L));
        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    // ---------- deactivate : cas limite (déjà désactivé) ----------

    @Test
    void testDesactiverFournisseurDejaDesactive() {
        Fournisseur f = new Fournisseur("Souss Equipement", "contact@souss.ma", null, null);
        f.setId(7L);
        f.setActif(false);
        when(fournisseurRepository.findById(7L)).thenReturn(Optional.of(f));
        when(fournisseurRepository.saveAndFlush(any(Fournisseur.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FournisseurResponse response = service.deactivateFournisseur(7L);

        assertFalse(response.actif());
        verify(fournisseurRepository, never()).deleteById(any());
    }
}