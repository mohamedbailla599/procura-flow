import express from 'express';
import { createServer as createViteServer } from 'vite';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export interface FournisseurDto {
  id: number;
  nom: string;
  email: string;
  telephone: string | null;
  adresse: string | null;
  actif: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LigneCommandeDto {
  id: number;
  designation: string;
  quantite: number;
  prixUnitaire: number;
}

export interface CommandeDto {
  id: number;
  fournisseurId: number;
  dateCommande: string;
  statut: 'EN_ATTENTE' | 'VALIDEE' | 'RECEPTIONNEE' | 'FACTUREE' | 'ANNULEE';
  montantTotal: number;
  lignes: LigneCommandeDto[];
}

let commandesServiceAvailable = true;

let fournisseurs: FournisseurDto[] = [
  {
    id: 1,
    nom: 'Atlas Fournitures',
    email: 'contact@atlas.example',
    telephone: '+212600000000',
    adresse: 'Zone Industrielle Tassila, Agadir, Maroc',
    actif: true,
    createdAt: '2026-10-10T09:15:00Z',
    updatedAt: '2026-10-10T09:15:00Z',
  },
  {
    id: 2,
    nom: 'TechnoLab Équipements Scientifiques',
    email: 'contact@technolab-univ.fr',
    telephone: '+33142685510',
    adresse: '14 Rue des Sciences, 75005 Paris',
    actif: true,
    createdAt: '2026-10-12T11:30:00Z',
    updatedAt: '2026-10-12T11:30:00Z',
  },
  {
    id: 3,
    nom: 'Souss Bureautique & Réseaux',
    email: 'achats@souss-bureautique.ma',
    telephone: '+212528841200',
    adresse: 'Avenue Hassan II, Agadir',
    actif: true,
    createdAt: '2026-10-14T14:20:00Z',
    updatedAt: '2026-10-14T14:20:00Z',
  },
  {
    id: 4,
    nom: 'Ancien Fournisseur Archive SARL',
    email: 'archive@ancien-fournisseur.ma',
    telephone: '+212522001122',
    adresse: 'Casablanca, Maroc',
    actif: false,
    createdAt: '2026-09-18T08:45:00Z',
    updatedAt: '2026-10-01T16:00:00Z',
  },
];

let commandes: CommandeDto[] = [
  {
    id: 101,
    fournisseurId: 1,
    dateCommande: '2026-10-15T10:30:00Z',
    statut: 'VALIDEE',
    montantTotal: 450.0,
    lignes: [
      { id: 501, designation: 'Papier A4 80g (Ramettes)', quantite: 10, prixUnitaire: 45.0 },
    ],
  },
  {
    id: 108,
    fournisseurId: 1,
    dateCommande: '2026-10-19T14:00:00Z',
    statut: 'EN_ATTENTE',
    montantTotal: 1250.0,
    lignes: [
      { id: 502, designation: 'Toners Imprimante Laser Réseau', quantite: 5, prixUnitaire: 250.0 },
    ],
  },
  {
    id: 109,
    fournisseurId: 2,
    dateCommande: '2026-10-20T09:15:00Z',
    statut: 'RECEPTIONNEE',
    montantTotal: 3700.0,
    lignes: [
      { id: 503, designation: 'Oscilloscope Numérique 4 Voies', quantite: 2, prixUnitaire: 1850.0 },
    ],
  },
  {
    id: 95,
    fournisseurId: 4,
    dateCommande: '2026-09-22T11:00:00Z',
    statut: 'FACTUREE',
    montantTotal: 890.0,
    lignes: [
      { id: 490, designation: 'Chaises Ergonomiques Laboratoire', quantite: 2, prixUnitaire: 445.0 },
    ],
  },
];

let nextFournisseurId = 5;

function nowIsoUtc(): string {
  return new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');
}

function buildError(
  status: number,
  error: string,
  message: string,
  reqPath: string,
  validationErrors?: Record<string, string>
) {
  return {
    timestamp: nowIsoUtc(),
    status,
    error,
    message,
    path: reqPath,
    ...(validationErrors ? { validationErrors } : {}),
  };
}

function validateFournisseurPayload(body: Record<string, unknown>): Record<string, string> {
  const errors: Record<string, string> = {};
  const nom = typeof body?.nom === 'string' ? body.nom.trim() : '';
  const email = typeof body?.email === 'string' ? body.email.trim() : '';
  const telephone = typeof body?.telephone === 'string' ? body.telephone.trim() : '';
  const adresse = typeof body?.adresse === 'string' ? body.adresse.trim() : '';

  if (!nom) {
    errors.nom = 'Le nom du fournisseur est obligatoire.';
  } else if (nom.length > 150) {
    errors.nom = 'Le nom du fournisseur ne peut pas dépasser 150 caractères.';
  }

  const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
  if (!email) {
    errors.email = "L'adresse e-mail du fournisseur est obligatoire.";
  } else if (!emailRegex.test(email)) {
    errors.email = "Le format de l'adresse e-mail est invalide.";
  } else if (email.length > 150) {
    errors.email = "L'adresse e-mail ne peut pas dépasser 150 caractères.";
  }

  if (telephone && telephone.length > 30) {
    errors.telephone = 'Le numéro de téléphone ne peut pas dépasser 30 caractères.';
  }
  if (adresse && adresse.length > 2000) {
    errors.adresse = "L'adresse ne peut pas dépasser 2000 caractères.";
  }
  return errors;
}

const JAVA_PROJECT_FILES = [
  'service-fournisseurs-mohamed/pom.xml',
  'service-fournisseurs-mohamed/Dockerfile',
  'service-fournisseurs-mohamed/.env.example',
  'service-fournisseurs-mohamed/README.md',
  'service-fournisseurs-mohamed/src/main/resources/application.yml',
  'service-fournisseurs-mohamed/src/main/resources/db/migration/V1__init_schema_fournisseurs.sql',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/FournisseursApplication.java',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/entity/Fournisseur.java',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/repository/FournisseurRepository.java',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/service/FournisseurService.java',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/controller/FournisseurController.java',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/client/CommandesClient.java',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/config/NeonJdbcUrlResolver.java',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/config/DataSourceConfig.java',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/config/RestClientConfig.java',
  'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/exception/GlobalExceptionHandler.java',
  'service-fournisseurs-mohamed/src/test/java/com/procuraflow/fournisseurs/service/FournisseurServiceTest.java',
  'service-fournisseurs-mohamed/src/test/java/com/procuraflow/fournisseurs/controller/FournisseurControllerTest.java',
  'service-fournisseurs-mohamed/src/test/java/com/procuraflow/fournisseurs/client/CommandesClientTest.java',
  'service-fournisseurs-mohamed/src/test/java/com/procuraflow/fournisseurs/integration/FournisseurIntegrationTest.java',
];

async function startServer() {
  const app = express();
  app.use(express.json());

  // Actuator health endpoint
  app.get('/actuator/health', (_req, res) => {
    res.json({ status: 'UP' });
  });

  // GET /api/fournisseurs with optional filters: actif, nom, email, includeInactive
  app.get('/api/fournisseurs', (req, res) => {
    const { actif, nom, email, includeInactive } = req.query;

    let result = [...fournisseurs];

    if (actif !== undefined && actif !== '') {
      if (actif !== 'true' && actif !== 'false') {
        return res
          .status(400)
          .json(buildError(400, 'BAD_REQUEST', "Format invalide pour le paramètre 'actif'.", req.path));
      }
      const targetActif = actif === 'true';
      result = result.filter((f) => f.actif === targetActif);
    } else if (includeInactive !== 'true') {
      result = result.filter((f) => f.actif === true);
    }

    if (typeof nom === 'string' && nom.trim() !== '') {
      const q = nom.trim().toLowerCase();
      result = result.filter((f) => f.nom.toLowerCase().includes(q));
    }

    if (typeof email === 'string' && email.trim() !== '') {
      const q = email.trim().toLowerCase();
      result = result.filter((f) => f.email.toLowerCase().includes(q));
    }

    res.json(result);
  });

  // GET /api/fournisseurs/:id
  app.get('/api/fournisseurs/:id', (req, res) => {
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) {
      return res
        .status(400)
        .json(buildError(400, 'BAD_REQUEST', "Format invalide pour le paramètre 'id'.", req.path));
    }
    const found = fournisseurs.find((f) => f.id === id);
    if (!found) {
      return res
        .status(404)
        .json(buildError(404, 'NOT_FOUND', `Fournisseur introuvable avec l'identifiant : ${id}`, req.path));
    }
    res.json(found);
  });

  // POST /api/fournisseurs
  app.post('/api/fournisseurs', (req, res) => {
    const validationErrors = validateFournisseurPayload(req.body);
    if (Object.keys(validationErrors).length > 0) {
      const firstMessage = Object.values(validationErrors)[0];
      return res
        .status(400)
        .json(buildError(400, 'BAD_REQUEST', firstMessage, req.path, validationErrors));
    }

    const normalizedEmail = String(req.body.email).trim().toLowerCase();
    if (fournisseurs.some((f) => f.email.toLowerCase() === normalizedEmail)) {
      return res
        .status(409)
        .json(
          buildError(
            409,
            'CONFLICT',
            `Un fournisseur avec l'adresse e-mail '${normalizedEmail}' existe déjà.`,
            req.path
          )
        );
    }

    const now = nowIsoUtc();
    const created: FournisseurDto = {
      id: nextFournisseurId++,
      nom: String(req.body.nom).trim(),
      email: normalizedEmail,
      telephone: req.body.telephone && String(req.body.telephone).trim() ? String(req.body.telephone).trim() : null,
      adresse: req.body.adresse && String(req.body.adresse).trim() ? String(req.body.adresse).trim() : null,
      actif: true,
      createdAt: now,
      updatedAt: now,
    };

    fournisseurs.push(created);
    res.status(201).location(`/api/fournisseurs/${created.id}`).json(created);
  });

  // PUT /api/fournisseurs/:id
  app.put('/api/fournisseurs/:id', (req, res) => {
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) {
      return res
        .status(400)
        .json(buildError(400, 'BAD_REQUEST', "Format invalide pour le paramètre 'id'.", req.path));
    }

    const idx = fournisseurs.findIndex((f) => f.id === id);
    if (idx === -1) {
      return res
        .status(404)
        .json(buildError(404, 'NOT_FOUND', `Fournisseur introuvable avec l'identifiant : ${id}`, req.path));
    }

    const validationErrors = validateFournisseurPayload(req.body);
    if (Object.keys(validationErrors).length > 0) {
      const firstMessage = Object.values(validationErrors)[0];
      return res
        .status(400)
        .json(buildError(400, 'BAD_REQUEST', firstMessage, req.path, validationErrors));
    }

    const normalizedEmail = String(req.body.email).trim().toLowerCase();
    if (fournisseurs.some((f) => f.id !== id && f.email.toLowerCase() === normalizedEmail)) {
      return res
        .status(409)
        .json(
          buildError(
            409,
            'CONFLICT',
            `Un fournisseur avec l'adresse e-mail '${normalizedEmail}' existe déjà.`,
            req.path
          )
        );
    }

    fournisseurs[idx] = {
      ...fournisseurs[idx],
      nom: String(req.body.nom).trim(),
      email: normalizedEmail,
      telephone: req.body.telephone && String(req.body.telephone).trim() ? String(req.body.telephone).trim() : null,
      adresse: req.body.adresse && String(req.body.adresse).trim() ? String(req.body.adresse).trim() : null,
      updatedAt: nowIsoUtc(),
    };

    res.json(fournisseurs[idx]);
  });

  // PATCH /api/fournisseurs/:id/desactiver
  app.patch('/api/fournisseurs/:id/desactiver', (req, res) => {
    const id = Number(req.params.id);
    const idx = fournisseurs.findIndex((f) => f.id === id);
    if (idx === -1) {
      return res
        .status(404)
        .json(buildError(404, 'NOT_FOUND', `Fournisseur introuvable avec l'identifiant : ${id}`, req.path));
    }
    fournisseurs[idx] = {
      ...fournisseurs[idx],
      actif: false,
      updatedAt: nowIsoUtc(),
    };
    res.json(fournisseurs[idx]);
  });

  // PATCH /api/fournisseurs/:id/reactiver
  app.patch('/api/fournisseurs/:id/reactiver', (req, res) => {
    const id = Number(req.params.id);
    const idx = fournisseurs.findIndex((f) => f.id === id);
    if (idx === -1) {
      return res
        .status(404)
        .json(buildError(404, 'NOT_FOUND', `Fournisseur introuvable avec l'identifiant : ${id}`, req.path));
    }
    fournisseurs[idx] = {
      ...fournisseurs[idx],
      actif: true,
      updatedAt: nowIsoUtc(),
    };
    res.json(fournisseurs[idx]);
  });

  // DELETE /api/fournisseurs/:id (Soft deactivation per integration-contract.md Section 5.1 & 5.4)
  app.delete('/api/fournisseurs/:id', (req, res) => {
    const id = Number(req.params.id);
    const idx = fournisseurs.findIndex((f) => f.id === id);
    if (idx === -1) {
      return res
        .status(404)
        .json(buildError(404, 'NOT_FOUND', `Fournisseur introuvable avec l'identifiant : ${id}`, req.path));
    }
    fournisseurs[idx] = {
      ...fournisseurs[idx],
      actif: false,
      updatedAt: nowIsoUtc(),
    };
    res.status(204).send();
  });

  // GET /api/fournisseurs/:id/commandes (Consumes Commandes via REST)
  app.get('/api/fournisseurs/:id/commandes', (req, res) => {
    const id = Number(req.params.id);
    if (!Number.isInteger(id) || id <= 0) {
      return res
        .status(400)
        .json(buildError(400, 'BAD_REQUEST', "Format invalide pour le paramètre 'id'.", req.path));
    }

    const found = fournisseurs.find((f) => f.id === id);
    if (!found) {
      return res
        .status(404)
        .json(buildError(404, 'NOT_FOUND', `Fournisseur introuvable avec l'identifiant : ${id}`, req.path));
    }

    if (!commandesServiceAvailable) {
      return res
        .status(503)
        .json(
          buildError(
            503,
            'SERVICE_UNAVAILABLE',
            'Le service Commandes est temporairement indisponible. Veuillez réessayer ultérieurement.',
            req.path
          )
        );
    }

    const supplierOrders = commandes.filter((c) => c.fournisseurId === id);
    res.json(supplierOrders);
  });

  // Downstream Commandes endpoint (GET /api/commandes?fournisseurId={id})
  app.get('/api/commandes', (req, res) => {
    if (!commandesServiceAvailable) {
      return res
        .status(503)
        .json(buildError(503, 'SERVICE_UNAVAILABLE', 'Service Commandes indisponible.', req.path));
    }
    const { fournisseurId } = req.query;
    if (fournisseurId !== undefined) {
      const fId = Number(fournisseurId);
      return res.json(commandes.filter((c) => c.fournisseurId === fId));
    }
    res.json(commandes);
  });

  // Toggle simulated Commandes availability for interactive testing
  app.get('/api/simulation/commandes-status', (_req, res) => {
    res.json({ available: commandesServiceAvailable, baseUrl: 'http://service-commandes:8082' });
  });

  app.post('/api/simulation/commandes-status', (req, res) => {
    commandesServiceAvailable = Boolean(req.body?.available);
    res.json({ available: commandesServiceAvailable, baseUrl: 'http://service-commandes:8082' });
  });

  // Expose real Java Spring Boot files for inspection in the workspace viewer
  app.get('/api/workspace/files', (_req, res) => {
    const files = JAVA_PROJECT_FILES.map((relPath) => {
      try {
        const content = fs.readFileSync(path.join(__dirname, relPath), 'utf-8');
        return { path: relPath, content };
      } catch {
        return { path: relPath, content: '' };
      }
    });
    res.json({ files });
  });

  app.use(express.static(path.join(__dirname, 'public')));

  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true, hmr: false },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  const PORT = 3000;
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
