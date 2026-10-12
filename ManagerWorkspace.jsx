import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity, ArrowDownRight, ArrowUpRight, Bus, CalendarClock, CheckCircle2,
  ChevronDown, CircleAlert, ClipboardList, FileText, Gauge, LayoutDashboard,
  Menu, MessageSquareText, RefreshCw, Route, Search, Settings2, ShieldCheck,
  Users, Wrench, CreditCard, UserRound, UserCog, Database, X, Plus, ExternalLink, LogOut
} from 'lucide-react';
import { getJson, postJson, patchJson } from './api.js';

const RESOURCES = {
  managers: {
    label: 'Managers', singular: 'Manager', icon: UserCog, endpoint: '/managers',
    columns: ['managerId', 'name', 'nic', 'phone', 'email'],
    fields: [
      { name: 'name', label: 'Full name', required: true }, { name: 'nic', label: 'NIC', required: true },
      { name: 'phone', label: 'Phone' }, { name: 'email', label: 'Email', type: 'email', required: true },
      { name: 'password', label: 'Initial password', type: 'password', required: true }
    ]
  },
  vehicles: {
    label: 'Vehicles', singular: 'Vehicle', icon: Bus, endpoint: '/vehicles',
    columns: ['vehicleId', 'registrationNo', 'vehicleType', 'manufactureYear', 'seatCapacity', 'fuelType', 'status'],
    fields: [
      { name: 'managerId', label: 'Manager ID', type: 'number', required: true },
      { name: 'vehicleType', label: 'Vehicle type', required: true, placeholder: 'BUS / VAN' },
      { name: 'registrationNo', label: 'Registration number', required: true },
      { name: 'manufactureYear', label: 'Manufacture year', type: 'number' },
      { name: 'seatCapacity', label: 'Seat capacity', type: 'number', required: true },
      { name: 'fuelType', label: 'Fuel type' },
      { name: 'status', label: 'Status', type: 'select', options: ['ACTIVE', 'INACTIVE', 'MAINTENANCE', 'RETIRED'], defaultValue: 'ACTIVE' }
    ]
  },
  drivers: {
    label: 'Drivers', singular: 'Driver', icon: UserRound, endpoint: '/drivers',
    columns: ['driverId', 'name', 'nic', 'licenseNo', 'phone', 'email', 'hireDate', 'status'],
    fields: [
      { name: 'managerId', label: 'Manager ID', type: 'number', required: true }, { name: 'name', label: 'Full name', required: true },
      { name: 'password', label: 'Initial password', type: 'password', required: true }, { name: 'nic', label: 'NIC', required: true },
      { name: 'licenseNo', label: 'Licence number', required: true }, { name: 'phone', label: 'Phone' },
      { name: 'email', label: 'Email', type: 'email' }, { name: 'hireDate', label: 'Hire date', type: 'date', required: true },
      { name: 'status', label: 'Status', type: 'select', options: ['ACTIVE', 'INACTIVE'], defaultValue: 'ACTIVE' }
    ]
  },
  routes: {
    label: 'Routes', singular: 'Route', icon: Route, endpoint: '/routes',
    columns: ['routeId', 'routeName', 'startLocation', 'endLocation', 'distanceKm', 'durationTime', 'status'],
    fields: [
      { name: 'managerId', label: 'Manager ID', type: 'number', required: true }, { name: 'routeName', label: 'Route name', required: true },
      { name: 'startLocation', label: 'Start location', required: true }, { name: 'endLocation', label: 'End location', required: true },
      { name: 'distanceKm', label: 'Distance (km)', type: 'number', required: true },
      { name: 'durationTime', label: 'Duration (minutes)', type: 'number', required: true },
      { name: 'status', label: 'Status', type: 'select', options: ['ACTIVE', 'INACTIVE'], defaultValue: 'ACTIVE' }
    ]
  },
  passengers: {
    label: 'Passengers', singular: 'Passenger', icon: Users, endpoint: '/passengers',
    columns: ['passengerId', 'name', 'nic', 'email', 'phone', 'passengerType', 'registeredDate'],
    fields: [
      { name: 'managerId', label: 'Manager ID', type: 'number', required: true }, { name: 'name', label: 'Full name', required: true },
      { name: 'nic', label: 'NIC' }, { name: 'email', label: 'Email', type: 'email', required: true },
      { name: 'password', label: 'Initial password', type: 'password', required: true }, { name: 'phone', label: 'Phone' },
      { name: 'passengerType', label: 'Passenger type', required: true, placeholder: 'REGULAR / STUDENT' }
    ]
  },
  technicians: {
    label: 'Technicians', singular: 'Technician', icon: Wrench, endpoint: '/technicians',
    columns: ['technicianId', 'name', 'phone', 'email', 'hireDate', 'specialization'],
    fields: [
      { name: 'managerId', label: 'Manager ID', type: 'number', required: true }, { name: 'name', label: 'Full name', required: true },
      { name: 'phone', label: 'Phone' }, { name: 'email', label: 'Email', type: 'email' },
      { name: 'password', label: 'Initial password', type: 'password', required: true },
      { name: 'hireDate', label: 'Hire date', type: 'date', required: true }, { name: 'specialization', label: 'Specialization', required: true }
    ]
  },
  trips: {
    label: 'Trips', singular: 'Trip', icon: CalendarClock, endpoint: '/trips',
    columns: ['tripId', 'tripDate', 'departureTime', 'arrivalTime', 'routeName', 'registrationNo', 'driverName', 'availableSeats', 'status'],
    fields: [
      { name: 'tripDate', label: 'Trip date', type: 'date', required: true }, { name: 'departureTime', label: 'Departure (HH:mm)', required: true, placeholder: '08:30' },
      { name: 'arrivalTime', label: 'Arrival (HH:mm)', required: true, placeholder: '11:00' }, { name: 'availableSeats', label: 'Available seats (optional; defaults to vehicle capacity)', type: 'number' },
      { name: 'status', label: 'Status', type: 'select', options: ['SCHEDULED', 'COMPLETED', 'CANCELLED'], defaultValue: 'SCHEDULED' },
      { name: 'routeId', label: 'Route ID', type: 'number', required: true }, { name: 'vehicleId', label: 'Vehicle ID', type: 'number', required: true },
      { name: 'driverId', label: 'Driver ID', type: 'number', required: true }
    ]
  },
  bookings: {
    label: 'Bookings', singular: 'Booking', icon: ClipboardList, endpoint: '/bookings',
    columns: ['bookingId', 'passengerName', 'routeName', 'tripDate', 'departureTime', 'seatNo', 'price', 'status'],
    fields: [
      { name: 'seatNo', label: 'Seat number', required: true }, { name: 'price', label: 'Price (LKR)', type: 'number', required: true },
      { name: 'passengerId', label: 'Passenger ID', type: 'number', required: true }, { name: 'tripId', label: 'Trip ID', type: 'number', required: true },
      { name: 'status', label: 'Status', type: 'select', options: ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'], defaultValue: 'PENDING' }
    ]
  },
  payments: {
    label: 'Payments', singular: 'Payment', icon: CreditCard, endpoint: '/payments',
    columns: ['paymentId', 'bookingId', 'passengerId', 'managerId', 'amount', 'paymentMethod', 'paymentDate', 'paymentStatus'],
    fields: [
      { name: 'bookingId', label: 'Booking ID (one payment per booking)', type: 'number', required: true },
      { name: 'managerId', label: 'Manager ID', type: 'number', required: true }, { name: 'amount', label: 'Amount (LKR)', type: 'number', required: true },
      { name: 'paymentMethod', label: 'Payment method', type: 'select', options: ['CASH', 'CARD', 'BANK_TRANSFER', 'ONLINE'], required: true },
      { name: 'paymentStatus', label: 'Payment status', type: 'select', options: ['PENDING', 'PAID', 'FAILED', 'REFUNDED'], defaultValue: 'PAID' }
    ]
  },
  maintenance: {
    label: 'Maintenance', singular: 'Maintenance record', icon: Wrench, endpoint: '/maintenance',
    columns: ['maintenanceId', 'registrationNo', 'maintenanceType', 'serviceDate', 'nextServiceDate', 'cost', 'status'],
    fields: [
      { name: 'vehicleId', label: 'Vehicle ID', type: 'number', required: true }, { name: 'technicianId', label: 'Technician ID', type: 'number', required: true },
      { name: 'managerId', label: 'Manager ID', type: 'number', required: true }, { name: 'maintenanceType', label: 'Maintenance type', required: true },
      { name: 'description', label: 'Description' }, { name: 'serviceDate', label: 'Service date', type: 'date', required: true },
      { name: 'cost', label: 'Cost (LKR)', type: 'number', defaultValue: '0' }, { name: 'nextServiceDate', label: 'Next service date', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', options: ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED'], defaultValue: 'SCHEDULED' }
    ]
  },
  feedback: {
    label: 'Feedback', singular: 'Feedback record', icon: MessageSquareText, endpoint: '/feedback',
    columns: ['feedbackId', 'passengerName', 'routeName', 'tripId', 'managerId', 'rating', 'description', 'feedbackDate'],
    fields: [
      { name: 'passengerId', label: 'Passenger ID', type: 'number', required: true }, { name: 'tripId', label: 'Trip ID', type: 'number', required: true },
      { name: 'managerId', label: 'Manager ID', type: 'number', required: true }, { name: 'rating', label: 'Rating (1–5)', type: 'number', min: 1, max: 5, required: true },
      { name: 'description', label: 'Comment', type: 'textarea' }
    ]
  }
};

const MAIN_NAV = [
  { id: 'dashboard', label: 'Overview', icon: LayoutDashboard },
  { id: 'vehicles', label: 'Vehicles', icon: Bus },
  { id: 'drivers', label: 'Drivers', icon: UserRound },
  { id: 'routes', label: 'Routes', icon: Route },
  { id: 'passengers', label: 'Passengers', icon: Users },
  { id: 'trips', label: 'Trips', icon: CalendarClock },
  { id: 'bookings', label: 'Bookings', icon: ClipboardList },
  { id: 'payments', label: 'Payments', icon: CreditCard },
  { id: 'maintenance', label: 'Maintenance', icon: Wrench },
  { id: 'feedback', label: 'Feedback', icon: MessageSquareText },
  { id: 'technicians', label: 'Technicians', icon: Wrench },
  { id: 'managers', label: 'Managers', icon: UserCog },
  { id: 'reports', label: 'Reports', icon: FileText },
  { id: 'content', label: 'NoSQL content', icon: Database }
];

const REPORTS = [
  { id: 'routes', title: 'Most-booked routes', description: 'Booking count grouped by route.', path: '/reports/most-booked-routes' },
  { id: 'revenue', title: 'Revenue report', description: 'Total paid revenue for a selected date range.', path: '/reports/revenue', params: true },
  { id: 'travel', title: 'Passenger travel history', description: 'Bookings and trips for a passenger ID.', path: '/reports/passenger-travel-history/1', passenger: true },
  { id: 'maintenance', title: 'Vehicles due for maintenance', description: 'Maintenance records due or overdue.', path: '/reports/vehicles-due-maintenance' },
  { id: 'occupancy', title: 'Trip occupancy', description: 'Booked seats and occupancy percentage by trip.', path: '/reports/trip-occupancy' }
];

const CONTENT_QUERIES = [
  { id: 'vehicles', title: 'Highest-rated vehicles', path: '/content/reviews/highest-rated-vehicles' },
  { id: 'drivers', title: 'Highest-rated drivers', path: '/content/reviews/highest-rated-drivers' },
  { id: 'announcements', title: 'Active announcements', path: '/content/announcements' }
];

function titleCase(value) {
  return String(value).replace(/([a-z0-9])([A-Z])/g, '$1 $2').replace(/_/g, ' ').replace(/^./, c => c.toUpperCase());
}
function displayValue(value) {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}
function dateValue(daysAgo = 0) {
  const d = new Date(); d.setDate(d.getDate() - daysAgo); return d.toISOString().slice(0, 10);
}
function makeInitialForm(resource) {
  return Object.fromEntries(resource.fields.map(f => [f.name, f.defaultValue ?? '']));
}

function DataTable({ data, columns, emptyMessage = 'No records found.' }) {
  const inferred = useMemo(() => {
    if (columns?.length) return columns;
    if (Array.isArray(data) && data.length) return Object.keys(data[0]).filter(k => k !== '_id');
    return [];
  }, [columns, data]);
  if (!Array.isArray(data) || data.length === 0) return <div className="empty-state"><div className="empty-icon"><ClipboardList size={22}/></div><strong>{emptyMessage}</strong><span>Try refreshing or changing the filters.</span></div>;
  return <div className="table-wrap"><table><thead><tr>{inferred.map(c => <th key={c}>{titleCase(c)}</th>)}</tr></thead><tbody>{data.map((row, idx) => <tr key={row.id ?? row._id?.$oid ?? row._id ?? idx}>{inferred.map(c => <td key={c}>{c === 'status' || c === 'paymentStatus' ? <span className={`status-pill ${String(row[c] || '').toLowerCase()}`}>{displayValue(row[c])}</span> : displayValue(row[c])}</td>)}</tr>)}</tbody></table></div>;
}

function Field({ field, value, onChange }) {
  const common = { id: field.name, name: field.name, value: value ?? '', required: !!field.required, onChange: e => onChange(field.name, e.target.value), placeholder: field.placeholder || '' };
  if (field.type === 'select') return <label className="field" htmlFor={field.name}><span>{field.label}{field.required && <b>*</b>}</span><select {...common}><option value="">Select…</option>{field.options.map(o => <option key={o} value={o}>{o.replaceAll('_', ' ')}</option>)}</select></label>;
  if (field.type === 'textarea') return <label className="field" htmlFor={field.name}><span>{field.label}{field.required && <b>*</b>}</span><textarea {...common} rows="3" /></label>;
  return <label className="field" htmlFor={field.name}><span>{field.label}{field.required && <b>*</b>}</span><input {...common} type={field.type || 'text'} min={field.min} max={field.max} step={field.type === 'number' ? 'any' : undefined}/></label>;
}

function ManagerWorkspace({ user, onLogout }) {
  const [active, setActive] = useState('dashboard');
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [data, setData] = useState([]);
  const [overview, setOverview] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [form, setForm] = useState({});
  const [showForm, setShowForm] = useState(false);
  const [filter, setFilter] = useState('');
  const [reportResult, setReportResult] = useState(null);
  const [revenueStart, setRevenueStart] = useState(`${new Date().getFullYear()}-${String(new Date().getMonth()+1).padStart(2,'0')}-01`);
  const [revenueEnd, setRevenueEnd] = useState(dateValue());
  const [passengerId, setPassengerId] = useState('1');
  const [contentResult, setContentResult] = useState(null);
  const [routeId, setRouteId] = useState('1');
  const [keyword, setKeyword] = useState('late');
  const [vehicleId, setVehicleId] = useState('1');
  const [tripId, setTripId] = useState('1');
  const [contentFormType, setContentFormType] = useState('review');
  const [contentForm, setContentForm] = useState({ passengerId: '1', tripId: '1', rating: '5', category: 'review', comments: '', createdByManagerId: '1', title: '', message: '', announcementType: 'GENERAL', active: 'true', publishFrom: dateValue(), publishUntil: dateValue(-30), documentType: 'VEHICLE_DOCUMENT', filePath: '', mediaType: 'image', caption: '', metadata: '{}' });

  const flash = useCallback((message) => { setNotice(message); window.setTimeout(() => setNotice(''), 3800); }, []);

  const loadOverview = useCallback(async () => {
    const keys = Object.keys(RESOURCES);
    const results = await Promise.allSettled(keys.map(k => getJson(RESOURCES[k].endpoint)));
    const next = {};
    results.forEach((res, idx) => { next[keys[idx]] = res.status === 'fulfilled' && Array.isArray(res.value) ? res.value : []; });
    const oracleConnected = results.some(res => res.status === 'fulfilled');
    const mongoConnected = await getJson('/content/announcements').then(() => true).catch(() => false);
    next._health = { oracle: oracleConnected ? 'connected' : 'error', mongodb: mongoConnected ? 'connected' : 'error' };
    setOverview(next);
  }, []);

  const loadResource = useCallback(async (key) => {
    setLoading(true); setError(''); setData([]);
    try {
      const result = await getJson(RESOURCES[key].endpoint);
      setData(Array.isArray(result) ? result : []);
    } catch (e) { setError(e.message || 'Could not load records'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => {
    setError(''); setData([]); setReportResult(null); setContentResult(null); setFilter(''); setShowForm(false);
    if (RESOURCES[active]) { const initial = makeInitialForm(RESOURCES[active]); if ('managerId' in initial && user?.managerId != null) initial.managerId = String(user.managerId); setForm(initial); loadResource(active); }
    else if (active === 'dashboard') { setLoading(true); loadOverview().finally(() => setLoading(false)); }
    // Intentionally reload when the selected page changes.
  }, [active, loadResource, loadOverview, user?.managerId]);

  const navTo = (id) => { setActive(id); setMobileNavOpen(false); };

  const resource = RESOURCES[active];
  const visibleData = useMemo(() => {
    if (!filter.trim()) return data;
    const q = filter.toLowerCase();
    return data.filter(row => Object.values(row).some(v => displayValue(v).toLowerCase().includes(q)));
  }, [data, filter]);

  async function createResource(e) {
    e.preventDefault(); if (!resource) return;
    setLoading(true); setError('');
    try {
      const payload = {};
      resource.fields.forEach(field => {
        const value = form[field.name];
        if (value === '' || value === null || value === undefined) return;
        payload[field.name] = field.type === 'number' ? Number(value) : value;
      });
      const result = await postJson(resource.endpoint, payload);
      flash(result?.message || `${resource.singular} created successfully`);
      setShowForm(false); setForm(makeInitialForm(resource)); await loadResource(active); await loadOverview();
    } catch (e2) { setError(e2.message || 'Create request failed'); }
    finally { setLoading(false); }
  }

  async function setStatus(row, kind) {
    const idKey = kind === 'vehicles' ? 'vehicleId' : 'bookingId';
    const current = String(row.status || '');
    const options = kind === 'vehicles' ? ['ACTIVE','INACTIVE','MAINTENANCE','RETIRED'] : ['PENDING','CONFIRMED','COMPLETED','CANCELLED'];
    const next = options[(options.indexOf(current) + 1 + options.length) % options.length];
    if (!window.confirm(`Change ${kind.slice(0,-1)} ${row[idKey]} status from ${current} to ${next}?`)) return;
    try {
      await patchJson(`/${kind}/${row[idKey]}/status`, { status: next });
      flash(`Status updated to ${next}`); await loadResource(active); await loadOverview();
    } catch (e) { setError(e.message); }
  }

  async function runReport(report) {
    setLoading(true); setError(''); setReportResult(null);
    try {
      let path = report.path;
      if (report.params) path += `?startDate=${encodeURIComponent(revenueStart)}&endDate=${encodeURIComponent(revenueEnd)}`;
      if (report.passenger) path = `/reports/passenger-travel-history/${encodeURIComponent(passengerId)}`;
      const result = await getJson(path); setReportResult({ title: report.title, value: result });
    } catch (e) { setError(e.message || 'Report failed'); }
    finally { setLoading(false); }
  }

  async function runContentQuery(title, path) {
    setLoading(true); setError(''); setContentResult(null);
    try { setContentResult({ title, value: await getJson(path) }); }
    catch (e) { setError(e.message || 'MongoDB request failed'); }
    finally { setLoading(false); }
  }

  async function createContent(e) {
    e.preventDefault(); setLoading(true); setError('');
    try {
      let path; let body;
      if (contentFormType === 'review') {
        path = '/content/reviews';
        body = { passengerId: Number(contentForm.passengerId), tripId: Number(contentForm.tripId), rating: Number(contentForm.rating), category: contentForm.category, comments: contentForm.comments };
      } else if (contentFormType === 'announcement') {
        path = '/content/announcements';
        body = { createdByManagerId: Number(contentForm.createdByManagerId), title: contentForm.title, message: contentForm.message, announcementType: contentForm.announcementType, active: contentForm.active === 'true', publishFrom: contentForm.publishFrom, publishUntil: contentForm.publishUntil };
      } else if (contentFormType === 'document') {
        path = `/content/vehicles/${Number(vehicleId)}/documents`;
        body = { documentType: contentForm.documentType, title: contentForm.title, filePath: contentForm.filePath, metadata: JSON.parse(contentForm.metadata || '{}') };
      } else {
        path = `/content/trips/${Number(tripId)}/media`;
        body = { mediaType: contentForm.mediaType, caption: contentForm.caption, filePath: contentForm.filePath, metadata: JSON.parse(contentForm.metadata || '{}') };
      }
      const result = await postJson(path, body); flash(result?.message || 'Content saved successfully');
      if (contentFormType === 'review') await runContentQuery('Reviews', `/content/reviews?routeId=${routeId || 1}`);
      else if (contentFormType === 'announcement') await runContentQuery('Active announcements', '/content/announcements');
      else if (contentFormType === 'document') await runContentQuery('Vehicle documents', `/content/vehicles/${Number(vehicleId)}/documents`);
      else await runContentQuery('Trip media', `/content/trips/${Number(tripId)}/media`);
    } catch (e2) { setError(e2.message || 'Could not save content. Check all required fields and IDs.'); }
    finally { setLoading(false); }
  }

  const totalRecords = Object.entries(overview).filter(([k,v]) => !k.startsWith('_') && Array.isArray(v)).reduce((sum, [,v]) => sum + v.length, 0);
  const pageTitle = resource?.label || MAIN_NAV.find(n => n.id === active)?.label || 'Overview';

  return <div className="app-shell">
    <aside className={`sidebar ${mobileNavOpen ? 'open' : ''}`}>
      <div className="brand"><div className="brand-mark"><Bus size={23}/></div><div><strong>SmartMove</strong><span>TRANSPORT SOLUTIONS</span></div><button className="close-nav icon-btn" onClick={() => setMobileNavOpen(false)} aria-label="Close navigation"><X size={18}/></button></div>
      <div className="workspace-label">WORKSPACE</div>
      <nav className="side-nav" aria-label="Main navigation">
        {MAIN_NAV.map(item => { const Icon = item.icon; return <button key={item.id} className={`nav-item ${active === item.id ? 'active' : ''}`} onClick={() => navTo(item.id)}><Icon size={18}/><span>{item.label}</span>{item.id === 'content' && <span className="nav-tag">NoSQL</span>}</button>; })}
      </nav>
      <div className="sidebar-bottom"><div className="connection-indicator"><span className="pulse-dot"/><div><strong>Local environment</strong><small>API · localhost:8080</small></div></div><div className="sidebar-note">Development workspace<br/>Keep database containers running.</div></div>
    </aside>
    {mobileNavOpen && <button className="nav-backdrop" aria-label="Close menu" onClick={() => setMobileNavOpen(false)}/>}
    <main className="main-area">
      <header className="topbar"><div className="topbar-left"><button className="icon-btn menu-button" onClick={() => setMobileNavOpen(true)} aria-label="Open navigation"><Menu size={20}/></button><div className="breadcrumb">SmartMove <span>/</span> <b>{pageTitle}</b></div></div><div className="top-actions"><span className="local-badge"><span className="pulse-dot"/> Local system</span><button className="icon-btn" aria-label="Refresh data" onClick={() => resource ? loadResource(active) : active === 'dashboard' ? loadOverview() : flash('Use the Refresh button on the selected panel.')}><RefreshCw size={17}/></button><div className="avatar" title={user?.name || 'Manager'}>{(user?.name || 'SM').split(/\s+/).map(part => part[0]).join('').slice(0, 2).toUpperCase()}</div><button className="icon-btn" title="Log out" aria-label="Log out" onClick={onLogout}><LogOut size={17}/></button></div></header>
      <div className="page-content">
        <div className="page-heading"><div><div className="eyebrow">TRANSPORT OPERATIONS</div><h1>{active === 'dashboard' ? 'Good to see you.' : pageTitle}</h1><p>{active === 'dashboard' ? 'Here is a live overview of the SmartMove workspace.' : resource ? `Manage ${resource.label.toLowerCase()} using your Oracle database.` : active === 'reports' ? 'Review operational reports generated from Oracle.' : 'Explore and manage dynamic content stored in MongoDB.'}</p></div><div className="heading-actions">{resource && <button className="btn primary" onClick={() => { setShowForm(s => !s); setError(''); }}><Plus size={17}/>{showForm ? 'Close form' : `Add ${resource.singular}`}</button>}</div></div>
        {error && <div className="alert error"><CircleAlert size={18}/><div><strong>Request failed</strong><span>{error}</span></div><button className="icon-btn" onClick={() => setError('')} aria-label="Dismiss error"><X size={16}/></button></div>}
        {notice && <div className="alert success"><CheckCircle2 size={18}/><span>{notice}</span><button className="icon-btn" onClick={() => setNotice('')} aria-label="Dismiss notification"><X size={16}/></button></div>}

        {active === 'dashboard' && <Dashboard overview={overview} totalRecords={totalRecords} onNavigate={navTo} onRefresh={loadOverview}/>}
        {resource && <section className="panel">
          {showForm && <form className="create-form" onSubmit={createResource}><div className="panel-head"><div><h2>Add {resource.singular}</h2><p>Required fields are marked with an asterisk.</p></div><button type="button" className="icon-btn" onClick={() => setShowForm(false)} aria-label="Close form"><X size={18}/></button></div><div className="form-grid">{resource.fields.map(field => <Field key={field.name} field={field} value={form[field.name]} onChange={(name, val) => setForm(prev => ({ ...prev, [name]: val }))}/>)}</div><div className="form-footer"><span className="help-text">Use IDs from existing records for foreign-key fields.</span><button className="btn primary" type="submit" disabled={loading}>{loading ? 'Saving…' : <><Plus size={16}/> Save {resource.singular}</>}</button></div></form>}
          <div className="panel-head"><div><h2>All {resource.label.toLowerCase()}</h2><p>{data.length} records returned by the API</p></div><div className="table-actions"><div className="search-box"><Search size={16}/><input value={filter} onChange={e => setFilter(e.target.value)} placeholder="Search records…"/></div><button className="btn secondary" onClick={() => loadResource(active)} disabled={loading}><RefreshCw size={15}/> Refresh</button></div></div>
          {loading ? <Loading/> : <DataTable data={visibleData} columns={resource.columns} emptyMessage={`No ${resource.label.toLowerCase()} found.`}/>}
          {(active === 'vehicles' || active === 'bookings') && data.length > 0 && <div className="panel-foot"><span>Need to change a record's status?</span><span>Use the row action in a future CRUD extension; status update endpoint is available in the backend.</span></div>}
        </section>}
        {active === 'reports' && <ReportsPanel loading={loading} result={reportResult} start={revenueStart} end={revenueEnd} setStart={setRevenueStart} setEnd={setRevenueEnd} passengerId={passengerId} setPassengerId={setPassengerId} onRun={runReport}/>}
        {active === 'content' && <ContentPanel loading={loading} result={contentResult} routeId={routeId} setRouteId={setRouteId} keyword={keyword} setKeyword={setKeyword} vehicleId={vehicleId} setVehicleId={setVehicleId} tripId={tripId} setTripId={setTripId} onQuery={runContentQuery} formType={contentFormType} setFormType={setContentFormType} form={contentForm} setForm={setContentForm} onCreate={createContent}/>}
        <footer className="page-footer"><span>SmartMove Transport Solutions</span><span><ShieldCheck size={14}/> Local coursework build · API <a href="http://localhost:8080/api/health/databases" target="_blank" rel="noreferrer">health check <ExternalLink size={12}/></a></span></footer>
      </div>
    </main>
  </div>;
}

function Dashboard({ overview, totalRecords, onNavigate, onRefresh }) {
  const tiles = [
    { id: 'vehicles', label: 'Vehicles', value: overview.vehicles?.length ?? '—', icon: Bus, note: 'Fleet records' },
    { id: 'trips', label: 'Trips', value: overview.trips?.length ?? '—', icon: CalendarClock, note: 'Scheduled services' },
    { id: 'bookings', label: 'Bookings', value: overview.bookings?.length ?? '—', icon: ClipboardList, note: 'Passenger reservations' },
    { id: 'routes', label: 'Routes', value: overview.routes?.length ?? '—', icon: Route, note: 'Defined journeys' }
  ];
  const health = overview._health;
  return <>
    <div className="welcome-banner"><div className="welcome-copy"><span className="welcome-kicker"><Activity size={15}/> LIVE OPERATIONS</span><h2>Your transport operations,<br/>in one place.</h2><p>Connected to the coursework Oracle schema and MongoDB content database.</p><div className="health-row"><span className={`health-chip ${health?.oracle === 'connected' ? 'ok' : ''}`}><span className="mini-dot"/> Oracle {health?.oracle || 'checking'}</span><span className={`health-chip ${health?.mongodb === 'connected' ? 'ok' : ''}`}><span className="mini-dot"/> MongoDB {health?.mongodb || 'checking'}</span></div></div><div className="welcome-art"><div className="art-orbit orbit-one"/><div className="art-orbit orbit-two"/><div className="art-core"><Bus size={52}/></div><div className="art-card card-a"><span>ACTIVE FLEET</span><strong>{overview.vehicles?.filter(v => String(v.status).toUpperCase() === 'ACTIVE').length ?? '—'}</strong></div><div className="art-card card-b"><span>CONTENT DB</span><strong>{health?.mongodb === 'connected' ? 'Online' : '…'}</strong></div></div></div>
    <div className="section-row"><div><h2>Operations overview</h2><p>Counts are loaded directly from the backend APIs.</p></div><button className="text-button" onClick={onRefresh}><RefreshCw size={15}/> Refresh counts</button></div>
    <div className="stat-grid">{tiles.map((tile, i) => { const Icon = tile.icon; const trend = i === 0 ? '01' : i === 1 ? '02' : i === 2 ? '03' : '04'; return <button key={tile.id} className="stat-card" onClick={() => onNavigate(tile.id)}><div className="stat-top"><span className={`stat-icon tint-${i}`}><Icon size={20}/></span><span className="stat-id">{trend}</span></div><div className="stat-label">{tile.label}</div><div className="stat-value">{tile.value}</div><div className="stat-note">{tile.note}<ArrowUpRight size={14}/></div></button>; })}</div>
    <div className="dashboard-grid"><section className="panel quick-panel"><div className="panel-head"><div><h2>Quick access</h2><p>Jump to an operational section.</p></div><Gauge size={19} className="muted-icon"/></div><div className="quick-list">{[
      ['Passengers','passengers','Manage passenger profiles'],['Payments','payments','Review booking payments'],['Maintenance','maintenance','Track vehicle service records'],['Reports','reports','Open business reports']
    ].map(([label,id,note]) => <button key={id} className="quick-row" onClick={() => onNavigate(id)}><span className="quick-symbol"><ArrowDownRight size={17}/></span><span className="quick-text"><strong>{label}</strong><small>{note}</small></span><ChevronDown size={16} className="quick-chevron"/></button>)}</div></section><section className="panel status-panel"><div className="panel-head"><div><h2>System status</h2><p>Local development connections.</p></div><span className="status-orbit"><Activity size={17}/></span></div><div className="system-status-row"><span className="system-icon"><Database size={19}/></span><div><strong>Oracle Database</strong><small>Relational records · port 1521</small></div><span className={`live-status ${health?.oracle === 'connected' ? '' : 'pending'}`}><i/>{health?.oracle || 'checking'}</span></div><div className="system-status-row"><span className="system-icon purple"><Database size={19}/></span><div><strong>MongoDB</strong><small>Dynamic content · port 27017</small></div><span className={`live-status ${health?.mongodb === 'connected' ? '' : 'pending'}`}><i/>{health?.mongodb || 'checking'}</span></div><div className="system-summary"><div><span>Records currently loaded</span><strong>{totalRecords}</strong></div><button className="btn secondary" onClick={onRefresh}><RefreshCw size={15}/> Check again</button></div></section></div>
  </>;
}

function Loading() { return <div className="loading-state"><span className="spinner"/> Loading data from the backend…</div>; }

function ReportsPanel({ loading, result, start, end, setStart, setEnd, passengerId, setPassengerId, onRun }) {
  return <div className="report-grid">{REPORTS.map(report => <section key={report.id} className="panel report-card"><div className="report-icon"><FileText size={20}/></div><h2>{report.title}</h2><p>{report.description}</p>{report.params && <div className="report-params"><label className="field"><span>Start date</span><input type="date" value={start} onChange={e => setStart(e.target.value)}/></label><label className="field"><span>End date</span><input type="date" value={end} onChange={e => setEnd(e.target.value)}/></label></div>}{report.passenger && <label className="field report-param-single"><span>Passenger ID</span><input type="number" min="1" value={passengerId} onChange={e => setPassengerId(e.target.value)}/></label>}<button className="btn primary full" disabled={loading} onClick={() => onRun(report)}>{loading ? 'Running…' : 'Run report'}<ArrowUpRight size={16}/></button></section>)}{result && <section className="panel report-result"><div className="panel-head"><div><h2>{result.title}</h2><p>Result returned by the Java report API.</p></div><span className="result-badge"><CheckCircle2 size={15}/> Complete</span></div>{Array.isArray(result.value) ? <DataTable data={result.value}/> : <pre className="json-view">{JSON.stringify(result.value, null, 2)}</pre>}</section>}</div>;
}

function ContentPanel({ loading, result, routeId, setRouteId, keyword, setKeyword, vehicleId, setVehicleId, tripId, setTripId, onQuery, formType, setFormType, form, setForm, onCreate }) {
  const updateForm = (name, value) => setForm(prev => ({ ...prev, [name]: value }));
  return <div className="content-layout"><section className="panel content-query-panel"><div className="panel-head"><div><h2>MongoDB content queries</h2><p>Read dynamic content through the Java API.</p></div><span className="nosql-chip">MongoDB</span></div>
    <div className="query-block"><h3>Passenger reviews</h3><div className="query-controls"><label className="field"><span>Route ID</span><input type="number" min="1" value={routeId} onChange={e => setRouteId(e.target.value)}/></label><button className="btn secondary" disabled={loading} onClick={() => onQuery('Reviews for route', `/content/reviews?routeId=${encodeURIComponent(routeId)}`)}>Find by route</button></div><div className="query-controls"><label className="field"><span>Keyword in comment</span><input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="late, delay…"/></label><button className="btn secondary" disabled={loading} onClick={() => onQuery('Reviews matching keyword', `/content/reviews?keyword=${encodeURIComponent(keyword)}`)}>Search comments</button></div></div>
    <div className="query-block"><h3>Ratings and announcements</h3><div className="button-stack">{CONTENT_QUERIES.map(q => <button className="quick-query" key={q.id} disabled={loading} onClick={() => onQuery(q.title, q.path)}><span>{q.title}</span><ArrowUpRight size={16}/></button>)}</div></div>
    <div className="query-block"><h3>Vehicle documents</h3><div className="query-controls"><label className="field"><span>Vehicle ID</span><input type="number" min="1" value={vehicleId} onChange={e => setVehicleId(e.target.value)}/></label><button className="btn secondary" disabled={loading} onClick={() => onQuery('Vehicle document metadata', `/content/vehicles/${encodeURIComponent(vehicleId)}/documents`)}>View documents</button></div></div>
    <div className="query-block last"><h3>Trip media</h3><div className="query-controls"><label className="field"><span>Trip ID</span><input type="number" min="1" value={tripId} onChange={e => setTripId(e.target.value)}/></label><button className="btn secondary" disabled={loading} onClick={() => onQuery('Trip media metadata', `/content/trips/${encodeURIComponent(tripId)}/media`)}>View media</button></div></div>
    </section>
    <section className="panel content-create-panel"><div className="panel-head"><div><h2>Add dynamic content</h2><p>Writes metadata/documents through Java.</p></div><span className="create-icon"><Plus size={17}/></span></div><label className="field"><span>Content type</span><select value={formType} onChange={e => setFormType(e.target.value)}><option value="review">Passenger review</option><option value="announcement">Announcement</option><option value="document">Vehicle document metadata</option><option value="media">Trip media metadata</option></select></label>
      <form onSubmit={onCreate} className="content-form">
        {formType === 'review' && <><div className="form-grid two"><label className="field"><span>Passenger ID *</span><input required type="number" min="1" value={form.passengerId} onChange={e => updateForm('passengerId', e.target.value)}/></label><label className="field"><span>Trip ID *</span><input required type="number" min="1" value={form.tripId} onChange={e => updateForm('tripId', e.target.value)}/></label></div><div className="form-grid two"><label className="field"><span>Rating *</span><select required value={form.rating} onChange={e => updateForm('rating', e.target.value)}>{[5,4,3,2,1].map(n => <option key={n} value={n}>{n} / 5</option>)}</select></label><label className="field"><span>Category</span><select value={form.category} onChange={e => updateForm('category', e.target.value)}><option>review</option><option>compliment</option><option>complaint</option></select></label></div><label className="field"><span>Comment *</span><textarea required rows="3" value={form.comments} onChange={e => updateForm('comments', e.target.value)} placeholder="Share the passenger's feedback…"/></label></>}
        {formType === 'announcement' && <><label className="field"><span>Manager ID *</span><input required type="number" min="1" value={form.createdByManagerId} onChange={e => updateForm('createdByManagerId', e.target.value)}/></label><label className="field"><span>Title *</span><input required value={form.title} onChange={e => updateForm('title', e.target.value)}/></label><label className="field"><span>Message *</span><textarea required rows="3" value={form.message} onChange={e => updateForm('message', e.target.value)}/></label><div className="form-grid two"><label className="field"><span>Type</span><input value={form.announcementType} onChange={e => updateForm('announcementType', e.target.value)}/></label><label className="field"><span>Active</span><select value={form.active} onChange={e => updateForm('active', e.target.value)}><option value="true">Yes</option><option value="false">No</option></select></label></div><div className="form-grid two"><label className="field"><span>Publish from</span><input type="date" value={form.publishFrom} onChange={e => updateForm('publishFrom', e.target.value)}/></label><label className="field"><span>Publish until</span><input type="date" value={form.publishUntil} onChange={e => updateForm('publishUntil', e.target.value)}/></label></div></>}
        {formType === 'document' && <><div className="form-grid two"><label className="field"><span>Vehicle ID</span><input required type="number" min="1" value={vehicleId} onChange={e => setVehicleId(e.target.value)}/></label><label className="field"><span>Document type *</span><input required value={form.documentType} onChange={e => updateForm('documentType', e.target.value)}/></label></div><label className="field"><span>Title *</span><input required value={form.title} onChange={e => updateForm('title', e.target.value)}/></label><label className="field"><span>File path/reference *</span><input required value={form.filePath} onChange={e => updateForm('filePath', e.target.value)} placeholder="/uploads/vehicles/document.pdf"/></label><label className="field"><span>Metadata JSON</span><textarea rows="2" value={form.metadata} onChange={e => updateForm('metadata', e.target.value)}/></label><p className="help-text">This records metadata/path only; it does not upload an actual file.</p></>}
        {formType === 'media' && <><label className="field"><span>Trip ID</span><input required type="number" min="1" value={tripId} onChange={e => setTripId(e.target.value)}/></label><div className="form-grid two"><label className="field"><span>Media type</span><select value={form.mediaType} onChange={e => updateForm('mediaType', e.target.value)}><option>image</option><option>video</option><option>audio</option></select></label><label className="field"><span>Caption *</span><input required value={form.caption} onChange={e => updateForm('caption', e.target.value)}/></label></div><label className="field"><span>File path/reference *</span><input required value={form.filePath} onChange={e => updateForm('filePath', e.target.value)} placeholder="/uploads/trips/1/photo.jpg"/></label><label className="field"><span>Metadata JSON</span><textarea rows="2" value={form.metadata} onChange={e => updateForm('metadata', e.target.value)}/></label><p className="help-text">This records metadata/path only; it does not upload an actual file.</p></>}
        <button className="btn primary full" type="submit" disabled={loading}>{loading ? 'Saving…' : <><Plus size={16}/> Save content</>}</button>
      </form>
    </section>
    {result && <section className="panel content-result"><div className="panel-head"><div><h2>{result.title}</h2><p>{Array.isArray(result.value) ? `${result.value.length} documents returned` : 'Response from MongoDB API'}</p></div><span className="nosql-chip">Query result</span></div>{Array.isArray(result.value) ? <DataTable data={result.value}/> : <pre className="json-view">{JSON.stringify(result.value, null, 2)}</pre>}</section>}
  </div>;
}

export default ManagerWorkspace;
