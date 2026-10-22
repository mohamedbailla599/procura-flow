import React, { useEffect, useState } from 'react';
import {
  Search,
  Plus,
  RefreshCw,
  FileCode,
  ExternalLink,
  Edit3,
  PowerOff,
  RotateCcw,
  ShoppingCart,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';

interface Fournisseur {
  id: number;
  nom: string;
  email: string;
  telephone: string | null;
  adresse: string | null;
  actif: boolean;
  createdAt: string;
  updatedAt: string;
}

interface LigneCommande {
  id: number;
  designation: string;
  quantite: number;
  prixUnitaire: number;
}

interface Commande {
  id: number;
  fournisseurId: number;
  dateCommande: string;
  statut: string;
  montantTotal: number;
  lignes?: LigneCommande[];
}

interface WorkspaceFile {
  path: string;
  content: string;
}

interface ApiErrorPayload {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

export default function App() {
  const [activeTab, setActiveTab] = useState<'fournisseurs' | 'commandes' | 'code' | 'config'>('fournisseurs');
  const [fournisseurs, setFournisseurs] = useState<Fournisseur[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // Filters
  const [searchNom, setSearchNom] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<'active' | 'inactive' | 'all'>('active');

  // Selected supplier & order history
  const [selectedFournisseur, setSelectedFournisseur] = useState<Fournisseur | null>(null);
  const [orders, setOrders] = useState<Commande[]>([]);
  const [ordersLoading, setOrdersLoading] = useState<boolean>(false);
  const [ordersError, setOrdersError] = useState<ApiErrorPayload | null>(null);
  const [commandesAvailable, setCommandesAvailable] = useState<boolean>(true);

  // Create / Edit Form state
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showFormModal, setShowFormModal] = useState<boolean>(false);
  const [formNom, setFormNom] = useState<string>('');
  const [formEmail, setFormEmail] = useState<string>('');
  const [formTelephone, setFormTelephone] = useState<string>('');
  const [formAdresse, setFormAdresse] = useState<string>('');
  const [formError, setFormError] = useState<ApiErrorPayload | null>(null);
  const [lastHttpTrace, setLastHttpTrace] = useState<{
    method: string;
    url: string;
    status: number;
    body: unknown;
  } | null>(null);

  // Workspace Java files
  const [workspaceFiles, setWorkspaceFiles] = useState<WorkspaceFile[]>([]);
  const [selectedFilePath, setSelectedFilePath] = useState<string>(
    'service-fournisseurs-mohamed/src/main/java/com/procuraflow/fournisseurs/controller/FournisseurController.java'
  );

  const fetchFournisseurs = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (statusFilter === 'active') {
        params.set('actif', 'true');
      } else if (statusFilter === 'inactive') {
        params.set('actif', 'false');
      } else {
        params.set('includeInactive', 'true');
      }
      if (searchNom.trim()) {
        params.set('nom', searchNom.trim());
      }

      const url = `/api/fournisseurs?${params.toString()}`;
      const res = await fetch(url);
      const data = await res.json();
      setFournisseurs(Array.isArray(data) ? data : []);
      setLastHttpTrace({ method: 'GET', url, status: res.status, body: data });
    } finally {
      setLoading(false);
    }
  };

  const fetchSimulationStatus = async () => {
    const res = await fetch('/api/simulation/commandes-status');
    if (res.ok) {
      const data = await res.json();
      setCommandesAvailable(Boolean(data.available));
    }
  };

  const fetchWorkspaceFiles = async () => {
    const res = await fetch('/api/workspace/files');
    if (res.ok) {
      const data = await res.json();
      setWorkspaceFiles(data.files || []);
    }
  };

  useEffect(() => {
    fetchFournisseurs();
  }, [statusFilter, searchNom]);

  useEffect(() => {
    fetchSimulationStatus();
    fetchWorkspaceFiles();
  }, []);

  const handleSelectSupplierOrders = async (fournisseur: Fournisseur) => {
    setSelectedFournisseur(fournisseur);
    setOrdersLoading(true);
    setOrdersError(null);
    const url = `/api/fournisseurs/${fournisseur.id}/commandes`;
    try {
      const res = await fetch(url);
      const data = await res.json();
      setLastHttpTrace({ method: 'GET', url, status: res.status, body: data });
      if (!res.ok) {
        setOrders([]);
        setOrdersError(data as ApiErrorPayload);
      } else {
        setOrders(Array.isArray(data) ? data : []);
      }
    } finally {
      setOrdersLoading(false);
    }
  };

  const toggleCommandesAvailability = async () => {
    const nextState = !commandesAvailable;
    const res = await fetch('/api/simulation/commandes-status', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ available: nextState }),
    });
    if (res.ok) {
      const data = await res.json();
      setCommandesAvailable(Boolean(data.available));
      if (selectedFournisseur) {
        handleSelectSupplierOrders(selectedFournisseur);
      }
    }
  };

  const openCreateModal = () => {
    setEditingId(null);
    setFormNom('');
    setFormEmail('');
    setFormTelephone('');
    setFormAdresse('');
    setFormError(null);
    setShowFormModal(true);
  };

  const openEditModal = (f: Fournisseur) => {
    setEditingId(f.id);
    setFormNom(f.nom);
    setFormEmail(f.email);
    setFormTelephone(f.telephone || '');
    setFormAdresse(f.adresse || '');
    setFormError(null);
    setShowFormModal(true);
  };

  const handleSubmitForm = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    const isEdit = editingId !== null;
    const url = isEdit ? `/api/fournisseurs/${editingId}` : '/api/fournisseurs';
    const method = isEdit ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nom: formNom,
        email: formEmail,
        telephone: formTelephone || null,
        adresse: formAdresse || null,
      }),
    });

    const data = await res.json();
    setLastHttpTrace({ method, url, status: res.status, body: data });

    if (!res.ok) {
      setFormError(data as ApiErrorPayload);
      return;
    }

    setShowFormModal(false);
    await fetchFournisseurs();
    if (selectedFournisseur && selectedFournisseur.id === data.id) {
      setSelectedFournisseur(data);
    }
  };

  const handleDeactivatePatch = async (f: Fournisseur) => {
    const url = `/api/fournisseurs/${f.id}/desactiver`;
    const res = await fetch(url, { method: 'PATCH' });
    const data = await res.json();
    setLastHttpTrace({ method: 'PATCH', url, status: res.status, body: data });
    await fetchFournisseurs();
    if (selectedFournisseur?.id === f.id) {
      setSelectedFournisseur(data);
    }
  };

  const handleReactivatePatch = async (f: Fournisseur) => {
    const url = `/api/fournisseurs/${f.id}/reactiver`;
    const res = await fetch(url, { method: 'PATCH' });
    const data = await res.json();
    setLastHttpTrace({ method: 'PATCH', url, status: res.status, body: data });
    await fetchFournisseurs();
    if (selectedFournisseur?.id === f.id) {
      setSelectedFournisseur(data);
    }
  };

  const selectedFile = workspaceFiles.find((file) => file.path === selectedFilePath) || workspaceFiles[0];

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col">
      {/* Top Bar Contract: 3 zones separated by gap-8 */}
      <header className="flex items-center justify-between gap-8 px-6 py-4 bg-white border-b border-slate-200">
        <a
          href="#fournisseurs"
          onClick={(e) => {
            e.preventDefault();
            setActiveTab('fournisseurs');
          }}
          className="text-lg font-bold tracking-tight text-slate-900 font-display whitespace-nowrap shrink-0"
        >
          ProcuraFlow Fournisseurs
        </a>

        <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-600">
          <button
            type="button"
            onClick={() => setActiveTab('fournisseurs')}
            className={`hover:text-slate-900 transition-colors whitespace-nowrap shrink-0 pb-0.5 ${
              activeTab === 'fournisseurs' ? 'text-slate-900 border-b-2 border-slate-900' : ''
            }`}
          >
            Référentiel Fournisseurs
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('commandes')}
            className={`hover:text-slate-900 transition-colors whitespace-nowrap shrink-0 pb-0.5 ${
              activeTab === 'commandes' ? 'text-slate-900 border-b-2 border-slate-900' : ''
            }`}
          >
            Intégration Commandes
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('code')}
            className={`hover:text-slate-900 transition-colors whitespace-nowrap shrink-0 pb-0.5 ${
              activeTab === 'code' ? 'text-slate-900 border-b-2 border-slate-900' : ''
            }`}
          >
            Sources Spring Boot
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('config')}
            className={`hover:text-slate-900 transition-colors whitespace-nowrap shrink-0 pb-0.5 ${
              activeTab === 'config' ? 'text-slate-900 border-b-2 border-slate-900' : ''
            }`}
          >
            Configuration Neon & Docker
          </button>
        </nav>

        <div className="flex items-center gap-3 shrink-0">
          <button
            type="button"
            onClick={openCreateModal}
            className="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold text-white bg-slate-900 rounded-lg hover:bg-slate-800 transition-colors whitespace-nowrap shrink-0"
          >
            <Plus className="w-3.5 h-3.5" />
            Nouveau fournisseur
          </button>
        </div>
      </header>

      <main className="flex-1 max-w-[1400px] w-full mx-auto px-6 py-8 space-y-8">
        {/* Contextual Workspace Header */}
        <section className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 pb-6 border-b border-slate-200">
          <div className="space-y-2 max-w-2xl">
            <div className="flex items-center gap-2 text-xs text-slate-500 font-mono">
              <span>service-fournisseurs-mohamed</span>
              <span aria-hidden="true">·</span>
              <span>Port 8081</span>
              <span aria-hidden="true">·</span>
              <span>schema_fournisseurs.fournisseurs</span>
              <span aria-hidden="true">·</span>
              <span>35/35 JUnit 5 Tests Passing</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 font-display">
              Gestion des fournisseurs et intégration REST inter-services
            </h1>
            <p className="text-sm text-slate-600 leading-relaxed">
              Implémentation complète Java / Spring Boot 3.3.5 connectée au schéma propriétaire{' '}
              <code className="font-mono text-xs text-slate-800">schema_fournisseurs</code> sur Neon PostgreSQL 17, avec
              désactivation logique et consommation du service Commandes via{' '}
              <code className="font-mono text-xs text-slate-800">COMMANDES_SERVICE_URL</code>.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <a
              href="/service-fournisseurs-mohamed.zip"
              download="service-fournisseurs-mohamed.zip"
              className="px-3.5 py-2 text-xs font-semibold rounded-lg bg-slate-900 text-white hover:bg-slate-800 transition-colors whitespace-nowrap shrink-0 flex items-center gap-2"
            >
              <ExternalLink className="w-3.5 h-3.5" />
              Télécharger service-fournisseurs-mohamed.zip
            </a>
            <button
              type="button"
              onClick={toggleCommandesAvailability}
              className={`px-3.5 py-2 text-xs font-medium rounded-lg border transition-colors whitespace-nowrap shrink-0 flex items-center gap-2 ${
                commandesAvailable
                  ? 'bg-white border-slate-200 text-slate-800 hover:bg-slate-100'
                  : 'bg-red-50 border-red-200 text-red-800 hover:bg-red-100'
              }`}
            >
              {commandesAvailable ? (
                <>
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                  Simuler panne Commandes (actuellement : En ligne)
                </>
              ) : (
                <>
                  <AlertTriangle className="w-3.5 h-3.5 text-red-600" />
                  Rétablir service Commandes (actuellement : Hors ligne → 503)
                </>
              )}
            </button>
          </div>
        </section>

        {activeTab === 'fournisseurs' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left 7 columns: Supplier directory table */}
            <div className="lg:col-span-7 bg-white border border-slate-200 rounded-xl overflow-hidden">
              <div className="p-4 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="relative flex-1">
                  <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={searchNom}
                    onChange={(e) => setSearchNom(e.target.value)}
                    placeholder="Filtrer par nom (?nom=...)"
                    className="w-full pl-9 pr-3 py-1.5 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-slate-900"
                  />
                </div>

                <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-lg">
                  <button
                    type="button"
                    onClick={() => setStatusFilter('active')}
                    className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 ${
                      statusFilter === 'active'
                        ? 'bg-white text-slate-900 shadow-xs'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Actifs (défaut)
                  </button>
                  <button
                    type="button"
                    onClick={() => setStatusFilter('inactive')}
                    className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 ${
                      statusFilter === 'inactive'
                        ? 'bg-white text-slate-900 shadow-xs'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Inactifs (?actif=false)
                  </button>
                  <button
                    type="button"
                    onClick={() => setStatusFilter('all')}
                    className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 ${
                      statusFilter === 'all'
                        ? 'bg-white text-slate-900 shadow-xs'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Tous (?includeInactive=true)
                  </button>
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50/70 text-xs font-semibold text-slate-600">
                      <th className="py-3 px-4 font-mono">ID</th>
                      <th className="py-3 px-4">Fournisseur & E-mail</th>
                      <th className="py-3 px-4">Statut</th>
                      <th className="py-3 px-4 text-right">Actions REST</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 text-sm">
                    {loading ? (
                      <tr>
                        <td colSpan={4} className="py-8 text-center text-slate-500">
                          Chargement des fournisseurs...
                        </td>
                      </tr>
                    ) : fournisseurs.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="py-10 text-center space-y-3">
                          <p className="text-sm text-slate-600">Aucun fournisseur ne correspond aux filtres sélectionnés.</p>
                          <button
                            type="button"
                            onClick={openCreateModal}
                            className="px-3.5 py-1.5 text-xs font-medium text-white bg-slate-900 rounded-lg hover:bg-slate-800"
                          >
                            Créer un fournisseur
                          </button>
                        </td>
                      </tr>
                    ) : (
                      fournisseurs.map((f) => (
                        <tr
                          key={f.id}
                          className={`hover:bg-slate-50 transition-colors ${
                            selectedFournisseur?.id === f.id ? 'bg-slate-50' : ''
                          }`}
                        >
                          <td className="py-3.5 px-4 font-mono text-xs text-slate-600 tabular-nums">#{f.id}</td>
                          <td className="py-3.5 px-4">
                            <div className="font-medium text-slate-900">{f.nom}</div>
                            <div className="text-xs text-slate-500 font-mono mt-0.5">
                              {f.email}
                              {f.telephone ? ` · ${f.telephone}` : ''}
                            </div>
                          </td>
                          <td className="py-3.5 px-4 text-xs font-medium">
                            {f.actif ? (
                              <span className="text-emerald-700">Actif (true)</span>
                            ) : (
                              <span className="text-amber-700">Désactivé (false)</span>
                            )}
                          </td>
                          <td className="py-3.5 px-4 text-right">
                            <div className="inline-flex items-center justify-end gap-1.5">
                              <button
                                type="button"
                                onClick={() => handleSelectSupplierOrders(f)}
                                title="GET /api/fournisseurs/{id}/commandes"
                                className="px-2.5 py-1 text-xs font-medium text-slate-700 bg-slate-100 rounded-md hover:bg-slate-200 transition-colors whitespace-nowrap shrink-0 inline-flex items-center gap-1"
                              >
                                <ShoppingCart className="w-3.5 h-3.5" />
                                Commandes
                              </button>
                              <button
                                type="button"
                                onClick={() => openEditModal(f)}
                                title="PUT /api/fournisseurs/{id}"
                                className="p-1.5 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-md transition-colors"
                              >
                                <Edit3 className="w-3.5 h-3.5" />
                              </button>
                              {f.actif ? (
                                <button
                                  type="button"
                                  onClick={() => handleDeactivatePatch(f)}
                                  title="PATCH /api/fournisseurs/{id}/desactiver"
                                  className="p-1.5 text-amber-700 hover:bg-amber-50 rounded-md transition-colors"
                                >
                                  <PowerOff className="w-3.5 h-3.5" />
                                </button>
                              ) : (
                                <button
                                  type="button"
                                  onClick={() => handleReactivatePatch(f)}
                                  title="PATCH /api/fournisseurs/{id}/reactiver"
                                  className="p-1.5 text-emerald-700 hover:bg-emerald-50 rounded-md transition-colors"
                                >
                                  <RotateCcw className="w-3.5 h-3.5" />
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Right 5 columns: Order History via Commandes & Live HTTP Trace */}
            <div className="lg:col-span-5 space-y-6">
              <div className="bg-white border border-slate-200 rounded-xl p-5 space-y-4">
                <div className="flex items-center justify-between border-b border-slate-200 pb-3">
                  <div>
                    <h2 className="text-base font-semibold text-slate-900 font-display">
                      Historique des commandes (via REST Commandes)
                    </h2>
                    <p className="text-xs text-slate-500 font-mono mt-0.5">
                      GET /api/fournisseurs/&#123;id&#125;/commandes → GET &#36;&#123;COMMANDES_SERVICE_URL&#125;/api/commandes?fournisseurId=&#123;id&#125;
                    </p>
                  </div>
                </div>

                {!selectedFournisseur ? (
                  <p className="text-sm text-slate-500 py-4">
                    Cliquez sur <strong>Commandes</strong> sur une ligne fournisseur pour déclencher l'appel REST vers le
                    microservice Commandes de Yassine.
                  </p>
                ) : (
                  <div className="space-y-4">
                    <div className="text-xs text-slate-600 flex items-center justify-between bg-slate-50 p-3 rounded-lg border border-slate-200">
                      <span>
                        Fournisseur sélectionné : <strong>{selectedFournisseur.nom}</strong> (#{selectedFournisseur.id})
                      </span>
                      <span className="font-mono">
                        actif={String(selectedFournisseur.actif)}
                      </span>
                    </div>

                    {ordersLoading ? (
                      <div className="text-sm text-slate-500 py-4">Appel REST vers Commandes en cours...</div>
                    ) : ordersError ? (
                      <div className="p-4 rounded-lg bg-red-50 border border-red-200 space-y-2">
                        <div className="text-xs font-semibold text-red-800 font-mono">
                          HTTP {ordersError.status} · {ordersError.error}
                        </div>
                        <p className="text-xs text-red-700">{ordersError.message}</p>
                      </div>
                    ) : orders.length === 0 ? (
                      <div className="text-sm text-slate-500 py-4">
                        Aucune commande trouvée auprès du service Commandes pour ce fournisseur.
                      </div>
                    ) : (
                      <div className="divide-y divide-slate-200 border border-slate-200 rounded-lg">
                        {orders.map((ord) => (
                          <div key={ord.id} className="p-3.5 flex items-center justify-between text-xs">
                            <div className="space-y-1">
                              <div className="font-semibold text-slate-900 font-mono">
                                Commande #{ord.id} · {ord.statut}
                              </div>
                              <div className="text-slate-500 font-mono">{ord.dateCommande}</div>
                            </div>
                            <div className="font-mono font-semibold text-slate-900 tabular-nums">
                              {ord.montantTotal.toFixed(2)} MAD
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {lastHttpTrace && (
                <div className="bg-slate-900 text-slate-100 rounded-xl p-5 space-y-3">
                  <div className="flex items-center justify-between text-xs font-mono border-b border-slate-800 pb-2.5">
                    <span className="text-slate-300">
                      {lastHttpTrace.method} {lastHttpTrace.url}
                    </span>
                    <span
                      className={
                        lastHttpTrace.status >= 200 && lastHttpTrace.status < 300
                          ? 'text-emerald-400'
                          : 'text-amber-400'
                      }
                    >
                      HTTP {lastHttpTrace.status}
                    </span>
                  </div>
                  <pre className="text-xs font-mono text-slate-300 overflow-x-auto max-h-60 leading-relaxed">
                    {JSON.stringify(lastHttpTrace.body, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'commandes' && (
          <div className="bg-white border border-slate-200 rounded-xl p-6 space-y-6">
            <h2 className="text-lg font-bold text-slate-900 font-display">
              Contrat d'intégration inter-services : Fournisseurs (8081) ↔ Commandes (8082)
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
              <div className="p-5 rounded-lg border border-slate-200 space-y-2">
                <h3 className="font-semibold text-slate-900">
                  1. Appel sortant depuis Fournisseurs vers Commandes
                </h3>
                <p className="text-slate-600 text-xs leading-relaxed">
                  Lorsqu'un client appelle <code className="font-mono">GET /api/fournisseurs/&#123;id&#125;/commandes</code>,
                  le microservice Fournisseurs vérifie d'abord que le fournisseur existe dans{' '}
                  <code className="font-mono">schema_fournisseurs.fournisseurs</code>, puis interroge Commandes via{' '}
                  <code className="font-mono">RestClient</code> :
                </p>
                <pre className="p-3 bg-slate-900 text-slate-100 rounded-lg text-xs font-mono overflow-x-auto">
                  GET &#36;&#123;COMMANDES_SERVICE_URL&#125;/api/commandes?fournisseurId=&#123;id&#125;
                </pre>
                <p className="text-xs text-slate-500">
                  Si Commandes est injoignable ou en timeout, Fournisseurs renvoie <code className="font-mono">503 Service Unavailable</code> sans exposer les détails internes.
                </p>
              </div>

              <div className="p-5 rounded-lg border border-slate-200 space-y-2">
                <h3 className="font-semibold text-slate-900">
                  2. Appel entrant depuis Commandes vers Fournisseurs
                </h3>
                <p className="text-slate-600 text-xs leading-relaxed">
                  Avant de créer une nouvelle commande (<code className="font-mono">POST /api/commandes</code>), le service
                  Commandes de Yassine appelle :
                </p>
                <pre className="p-3 bg-slate-900 text-slate-100 rounded-lg text-xs font-mono overflow-x-auto">
                  GET http://service-fournisseurs:8081/api/fournisseurs/&#123;fournisseurId&#125;
                </pre>
                <p className="text-xs text-slate-500">
                  Un fournisseur désactivé reste consultable via cet endpoint avec <code className="font-mono">"actif": false</code> afin que Commandes refuse toute nouvelle commande tout en préservant l'historique.
                </p>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'code' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 bg-white border border-slate-200 rounded-xl overflow-hidden">
            <div className="lg:col-span-4 border-r border-slate-200 p-4 space-y-2 max-h-[680px] overflow-y-auto">
              <div className="text-xs font-semibold text-slate-500 px-2 pb-2 border-b border-slate-200">
                Fichiers du microservice (service-fournisseurs-mohamed)
              </div>
              {workspaceFiles.map((file) => {
                const shortName = file.path.replace('service-fournisseurs-mohamed/', '');
                return (
                  <button
                    key={file.path}
                    type="button"
                    onClick={() => setSelectedFilePath(file.path)}
                    className={`w-full text-left px-3 py-2 rounded-lg text-xs font-mono transition-colors flex items-center gap-2 ${
                      selectedFilePath === file.path
                        ? 'bg-slate-900 text-white'
                        : 'text-slate-700 hover:bg-slate-100'
                    }`}
                  >
                    <FileCode className="w-3.5 h-3.5 shrink-0" />
                    <span className="truncate" title={shortName}>
                      {shortName}
                    </span>
                  </button>
                );
              })}
            </div>
            <div className="lg:col-span-8 bg-slate-950 text-slate-100 p-5 overflow-x-auto max-h-[680px]">
              {selectedFile && (
                <div className="space-y-3">
                  <div className="text-xs font-mono text-slate-400 border-b border-slate-800 pb-2">
                    {selectedFile.path}
                  </div>
                  <pre className="text-xs font-mono leading-relaxed text-slate-200">
                    {selectedFile.content}
                  </pre>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'config' && (
          <div className="bg-white border border-slate-200 rounded-xl p-6 space-y-6">
            <div className="space-y-1">
              <h2 className="text-lg font-bold text-slate-900 font-display">
                Variables d'environnement canoniques & Commandes d'exécution
              </h2>
              <p className="text-xs text-slate-600">
                Configuration standardisée sans variables dupliquées (port par défaut : 8081).
              </p>
            </div>

            <div className="overflow-x-auto border border-slate-200 rounded-lg">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                    <th className="py-2.5 px-4 font-mono">Variable</th>
                    <th className="py-2.5 px-4">Obligatoire</th>
                    <th className="py-2.5 px-4 font-mono">Défaut</th>
                    <th className="py-2.5 px-4">Format attendu</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  <tr>
                    <td className="py-2.5 px-4 font-mono font-semibold">DATABASE_URL</td>
                    <td className="py-2.5 px-4">Oui</td>
                    <td className="py-2.5 px-4 font-mono">—</td>
                    <td className="py-2.5 px-4 font-mono">
                      jdbc:postgresql://&lt;neon-host&gt;:5432/procura_flow?sslmode=require&amp;currentSchema=schema_fournisseurs
                    </td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-4 font-mono font-semibold">DATABASE_USERNAME</td>
                    <td className="py-2.5 px-4">Oui (sauf si dans URI)</td>
                    <td className="py-2.5 px-4 font-mono">—</td>
                    <td className="py-2.5 px-4">Identifiant utilisateur Neon PostgreSQL</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-4 font-mono font-semibold">DATABASE_PASSWORD</td>
                    <td className="py-2.5 px-4">Oui (sauf si dans URI)</td>
                    <td className="py-2.5 px-4 font-mono">—</td>
                    <td className="py-2.5 px-4">Mot de passe Neon PostgreSQL</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-4 font-mono font-semibold">SERVER_PORT</td>
                    <td className="py-2.5 px-4">Non</td>
                    <td className="py-2.5 px-4 font-mono">8081</td>
                    <td className="py-2.5 px-4 font-mono">8081</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-4 font-mono font-semibold">COMMANDES_SERVICE_URL</td>
                    <td className="py-2.5 px-4">Non</td>
                    <td className="py-2.5 px-4 font-mono">http://localhost:8082</td>
                    <td className="py-2.5 px-4 font-mono">
                      http://localhost:8082 (local) ou http://service-commandes:8082 (Docker Compose)
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        )}
      </main>

      {/* Modal Création / Modification */}
      {showFormModal && (
        <div className="fixed inset-0 bg-slate-900/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white border border-slate-200 rounded-xl max-w-md w-full p-6 space-y-4">
            <h3 className="text-base font-bold text-slate-900 font-display">
              {editingId ? `Modifier le fournisseur #${editingId}` : 'Créer un nouveau fournisseur'}
            </h3>

            {formError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-700 space-y-1">
                <div className="font-mono font-semibold">
                  HTTP {formError.status} · {formError.error}
                </div>
                <div>{formError.message}</div>
              </div>
            )}

            <form onSubmit={handleSubmitForm} className="space-y-3 text-sm">
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Nom *</label>
                <input
                  type="text"
                  required
                  maxLength={150}
                  value={formNom}
                  onChange={(e) => setFormNom(e.target.value)}
                  className="w-full px-3 py-1.5 border border-slate-200 rounded-lg text-sm"
                  placeholder="Atlas Fournitures"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">E-mail *</label>
                <input
                  type="email"
                  required
                  maxLength={150}
                  value={formEmail}
                  onChange={(e) => setFormEmail(e.target.value)}
                  className="w-full px-3 py-1.5 border border-slate-200 rounded-lg text-sm font-mono"
                  placeholder="contact@atlas.example"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Téléphone</label>
                <input
                  type="text"
                  maxLength={30}
                  value={formTelephone}
                  onChange={(e) => setFormTelephone(e.target.value)}
                  className="w-full px-3 py-1.5 border border-slate-200 rounded-lg text-sm font-mono"
                  placeholder="+212600000000"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Adresse</label>
                <textarea
                  rows={2}
                  maxLength={2000}
                  value={formAdresse}
                  onChange={(e) => setFormAdresse(e.target.value)}
                  className="w-full px-3 py-1.5 border border-slate-200 rounded-lg text-sm"
                  placeholder="Agadir, Maroc"
                />
              </div>
              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowFormModal(false)}
                  className="px-3.5 py-1.5 text-xs font-medium text-slate-700 bg-slate-100 rounded-lg hover:bg-slate-200"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 text-xs font-semibold text-white bg-slate-900 rounded-lg hover:bg-slate-800"
                >
                  {editingId ? 'Enregistrer (PUT)' : 'Créer (POST)'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
