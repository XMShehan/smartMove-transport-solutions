import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity, ArrowDownRight, ArrowUpRight, Bus, CalendarClock, CheckCircle2,
  ChevronDown, CircleAlert, ClipboardList, FileText, Gauge, LayoutDashboard,
  Menu, MessageSquareText, RefreshCw, Route, Search, Settings2, ShieldCheck,
  Users, Wrench, CreditCard, UserRound, UserCog, Database, X, Plus, ExternalLink, LogOut, Pencil, Trash2
} from 'lucide-react';
import { getJson, postJson, putJson, deleteJson } from './api.js';
import ProfilePanel from './ProfilePanel.jsx';
import './crud.css';

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
      { name: 'managerId', label: 'Assigned manager ID', type: 'currentManager', required: true },
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
      { name: 'managerId', label: 'Assigned manager ID', type: 'currentManager', required: true }, { name: 'name', label: 'Full name', required: true },
      { name: 'password', label: 'Initial password', type: 'password', required: true }, { name: 'nic', label: 'NIC', required: true },
      { name: 'licenseNo', label: 'Licence number', required: true }, { name: 'phone', label: 'Phone' },
      { name: 'email', label: 'Email', type: 'email' }, { name: 'hireDate', label: 'Hire date', type: 'date', required: true },
      { name: 'status', label: 'Status', type: 'select', options: ['ACTIVE', 'INACTIVE', 'ON_LEAVE'], defaultValue: 'ACTIVE' }
    ]
  },
  routes: {
    label: 'Routes', singular: 'Route', icon: Route, endpoint: '/routes',
    columns: ['routeId', 'routeName', 'startLocation', 'endLocation', 'distanceKm', 'durationTime', 'status'],
    fields: [
      { name: 'managerId', label: 'Assigned manager ID', type: 'currentManager', required: true }, { name: 'routeName', label: 'Route name', required: true },
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
      { name: 'managerId', label: 'Assigned manager ID', type: 'currentManager', required: true }, { name: 'name', label: 'Full name', required: true },
      { name: 'nic', label: 'NIC' }, { name: 'email', label: 'Email', type: 'email', required: true },
      { name: 'password', label: 'Initial password', type: 'password', required: true }, { name: 'phone', label: 'Phone' },
      { name: 'passengerType', label: 'Passenger type', required: true, placeholder: 'REGULAR / STUDENT' }
    ]
  },
  technicians: {
    label: 'Technicians', singular: 'Technician', icon: Wrench, endpoint: '/technicians',
    columns: ['technicianId', 'name', 'phone', 'email', 'hireDate', 'specialization'],
    fields: [
      { name: 'managerId', label: 'Assigned manager ID', type: 'currentManager', required: true }, { name: 'name', label: 'Full name', required: true },
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
      { name: 'routeId', label: 'Route', type: 'reference', reference: 'routes', valueField: 'routeId', labelFields: ['routeId', 'routeName'], onlyActive: true, required: true }, { name: 'vehicleId', label: 'Vehicle', type: 'reference', reference: 'vehicles', valueField: 'vehicleId', labelFields: ['vehicleId', 'registrationNo', 'vehicleType'], onlyActive: true, required: true },
      { name: 'driverId', label: 'Driver', type: 'reference', reference: 'drivers', valueField: 'driverId', labelFields: ['driverId', 'name'], onlyActive: true, required: true }
    ]
  },
  bookings: {
    label: 'Bookings', singular: 'Booking', icon: ClipboardList, endpoint: '/bookings',
    columns: ['bookingId', 'passengerName', 'routeName', 'tripDate', 'departureTime', 'seatNo', 'price', 'status', 'paymentStatus'],
    fields: [
      { name: 'seatNo', label: 'Seat number', required: true },
      { name: 'passengerId', label: 'Passenger', type: 'reference', reference: 'passengers', valueField: 'passengerId', labelFields: ['passengerId', 'name'], required: true }, { name: 'tripId', label: 'Trip', type: 'reference', reference: 'trips', valueField: 'tripId', labelFields: ['tripId', 'routeName', 'tripDate', 'departureTime'], availableTripOnly: true, required: true },
      { name: 'status', label: 'Status', type: 'select', options: ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'], defaultValue: 'PENDING' }
    ]
  },
  payments: {
    label: 'Payments', singular: 'Payment', icon: CreditCard, endpoint: '/payments',
    columns: ['paymentId', 'bookingId', 'passengerId', 'managerId', 'amount', 'paymentMethod', 'paymentDate', 'paymentStatus'],
    fields: [
      { name: 'bookingId', label: 'Booking (one payment per booking)', type: 'reference', reference: 'bookings', valueField: 'bookingId', labelFields: ['bookingId', 'passengerName', 'routeName', 'price'], payableBookingOnly: true, required: true },
      { name: 'managerId', label: 'Assigned manager ID', type: 'currentManager', required: true },
      { name: 'paymentMethod', label: 'Payment method', type: 'select', options: ['CASH', 'CARD', 'BANK_TRANSFER', 'ONLINE'], required: true },
      { name: 'paymentStatus', label: 'Payment status', type: 'select', options: ['PENDING', 'PAID', 'FAILED', 'REFUNDED'], defaultValue: 'PAID' }
    ]
  },
  maintenance: {
    label: 'Maintenance', singular: 'Maintenance record', icon: Wrench, endpoint: '/maintenance',
    columns: ['maintenanceId', 'registrationNo', 'maintenanceType', 'serviceDate', 'nextServiceDate', 'cost', 'status'],
    fields: [
      { name: 'vehicleId', label: 'Vehicle', type: 'reference', reference: 'vehicles', valueField: 'vehicleId', labelFields: ['vehicleId', 'registrationNo', 'vehicleType'], required: true }, { name: 'technicianId', label: 'Technician', type: 'reference', reference: 'technicians', valueField: 'technicianId', labelFields: ['technicianId', 'name'], required: true },
      { name: 'managerId', label: 'Assigned manager ID', type: 'currentManager', required: true }, { name: 'maintenanceType', label: 'Maintenance type', required: true },
      { name: 'description', label: 'Description' }, { name: 'serviceDate', label: 'Service date', type: 'date', required: true },
      { name: 'cost', label: 'Cost (LKR)', type: 'number', defaultValue: '0' }, { name: 'nextServiceDate', label: 'Next service date', type: 'date' },
      { name: 'status', label: 'Status', type: 'select', options: ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE'], defaultValue: 'SCHEDULED' }
    ]
  },
  feedback: {
    label: 'Feedback', singular: 'Feedback record', icon: MessageSquareText, endpoint: '/feedback',
    columns: ['feedbackId', 'passengerName', 'routeName', 'tripId', 'managerId', 'rating', 'description', 'feedbackDate'],
    fields: [
      { name: 'passengerId', label: 'Passenger', type: 'reference', reference: 'passengers', valueField: 'passengerId', labelFields: ['passengerId', 'name'], required: true }, { name: 'tripId', label: 'Trip', type: 'reference', reference: 'trips', valueField: 'tripId', labelFields: ['tripId', 'routeName', 'tripDate', 'departureTime'], required: true },
      { name: 'managerId', label: 'Assigned manager ID', type: 'currentManager', required: true }, { name: 'rating', label: 'Rating (1–5)', type: 'number', min: 1, max: 5, required: true },
      { name: 'description', label: 'Comment', type: 'textarea' }
    ]
  }
};

// Primary-key names match the JSON returned by the existing Java APIs.
const RESOURCE_IDS = {
  managers: 'managerId', vehicles: 'vehicleId', drivers: 'driverId', routes: 'routeId',
  passengers: 'passengerId', technicians: 'technicianId', trips: 'tripId', bookings: 'bookingId',
  payments: 'paymentId', maintenance: 'maintenanceId', feedback: 'feedbackId'
};

// Do not expose password hashes or allow changing relationship IDs on existing bookings/payments.
// Booking/payment history is retained: use status/refund workflows rather than hard deletion.
const EDIT_FIELDS = {
  managers: ['name', 'nic', 'phone', 'email'],
  vehicles: ['vehicleType', 'registrationNo', 'manufactureYear', 'seatCapacity', 'fuelType', 'status'],
  drivers: ['name', 'nic', 'licenseNo', 'phone', 'email', 'hireDate', 'status'],
  routes: ['routeName', 'startLocation', 'endLocation', 'distanceKm', 'durationTime', 'status'],
  passengers: ['name', 'nic', 'email', 'phone', 'passengerType'],
  technicians: ['name', 'phone', 'email', 'hireDate', 'specialization'],
  trips: ['tripDate', 'departureTime', 'arrivalTime', 'availableSeats', 'status'],
  bookings: ['status'],
  payments: ['paymentMethod', 'paymentStatus'],
  maintenance: ['vehicleId', 'technicianId', 'maintenanceType', 'description', 'serviceDate', 'cost', 'nextServiceDate', 'status'],
  feedback: ['rating', 'description']
};
const NON_DELETABLE = new Set(['bookings', 'payments']);

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
  { id: 'reports', label: 'Reports', icon: FileText },
  { id: 'content', label: 'Content & Media', icon: Database }
];

const REPORTS = [
  { id: 'routes', title: 'Most-booked routes', description: 'Booking count grouped by route.', path: '/reports/most-booked-routes' },
  { id: 'revenue', title: 'Revenue report', description: 'Total paid revenue for a selected date range.', path: '/reports/revenue', params: true },
  { id: 'travel', title: 'Passenger travel history', description: 'Bookings and trips for a passenger ID.', path: '/reports/passenger-travel-history/1', passenger: true },
  { id: 'maintenance', title: 'Vehicles due for maintenance', description: 'Maintenance records due or overdue.', path: '/reports/vehicles-due-maintenance' },
  { id: 'occupancy', title: 'Trip occupancy', description: 'Booked seats and occupancy percentage by trip.', path: '/reports/trip-occupancy' },
  { id: 'total-paid', title: 'Passenger total paid', description: 'Total amount recorded as PAID for a passenger using the PL/SQL function.', path: '/reports/passenger-total-paid/1', passengerTotal: true }
];

const CONTENT_QUERIES = [
  { id: 'vehicles', title: 'Highest-rated vehicles', path: '/content/reviews/highest-rated-vehicles' },
  { id: 'drivers', title: 'Highest-rated drivers', path: '/content/reviews/highest-rated-drivers' },
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
function editFieldsFor(key) {
  const allowed = new Set(EDIT_FIELDS[key] || []);
  return RESOURCES[key].fields.filter(field => allowed.has(field.name));
}
function inputValue(value, field) {
  if (value === null || value === undefined) return '';
  const text = String(value);
  if (field.type === 'date') return text.slice(0, 10);
  if (field.name === 'departureTime' || field.name === 'arrivalTime') return text.slice(0, 5);
  return text;
}

function DataTable({ data, columns, emptyMessage = 'No records found.', renderActions }) {
  const inferred = useMemo(() => {
    if (columns?.length) return columns;
    if (Array.isArray(data) && data.length) return Object.keys(data[0]).filter(k => k !== '_id');
    return [];
  }, [columns, data]);
  if (!Array.isArray(data) || data.length === 0) return <div className="empty-state"><div className="empty-icon"><ClipboardList size={22}/></div><strong>{emptyMessage}</strong><span>Try refreshing or changing the filters.</span></div>;
  return <div className="table-wrap"><table><thead><tr>{inferred.map(c => <th key={c}>{titleCase(c)}</th>)}{renderActions && <th className="crud-actions-heading">Actions</th>}</tr></thead><tbody>{data.map((row, idx) => <tr key={row.id ?? row._id?.$oid ?? row._id ?? row.vehicleId ?? row.driverId ?? row.routeId ?? row.passengerId ?? row.managerId ?? row.technicianId ?? row.tripId ?? row.bookingId ?? row.paymentId ?? row.maintenanceId ?? row.feedbackId ?? idx}>{inferred.map(c => <td key={c}>{c === 'status' || c === 'paymentStatus' ? <span className={`status-pill ${String(row[c] || '').toLowerCase()}`}>{displayValue(row[c])}</span> : displayValue(row[c])}</td>)}{renderActions && <td className="crud-actions-cell">{renderActions(row)}</td>}</tr>)}</tbody></table></div>;
}

function Field({ field, value, onChange, lookupOptions = {} }) {
  const common = { id: field.name, name: field.name, value: value ?? '', required: !!field.required, onChange: e => onChange(field.name, e.target.value), placeholder: field.placeholder || '' };
  if (field.type === 'currentManager') return <label className="field" htmlFor={field.name}><span>{field.label}{field.required && <b>*</b>}</span><input {...common} type="number" readOnly aria-readonly="true"/><small className="help-text">Automatically assigned from your signed-in manager account.</small></label>;
  if (field.type === 'reference') {
    let rows = lookupOptions[field.reference] || [];
    if (field.onlyActive) rows = rows.filter(row => String(row.status || '').toUpperCase() === 'ACTIVE');
    if (field.availableTripOnly) rows = rows.filter(row => String(row.status || 'SCHEDULED').toUpperCase() === 'SCHEDULED' && Number(row.availableSeats || 0) > 0 && (!row.tripDate || String(row.tripDate).slice(0, 10) >= new Date().toISOString().slice(0, 10)));
    if (field.payableBookingOnly) rows = rows.filter(row => !['CANCELLED'].includes(String(row.status || '').toUpperCase()) && !(lookupOptions.payments || []).some(payment => Number(payment.bookingId) === Number(row.bookingId)));
    return <label className="field" htmlFor={field.name}><span>{field.label}{field.required && <b>*</b>}</span><select {...common} disabled={common.disabled || rows.length === 0}><option value="">{rows.length ? 'Select a record…' : 'No related records available'}</option>{rows.map(row => { const optionValue = row[field.valueField]; const optionLabel = (field.labelFields || [field.valueField]).map(key => displayValue(row[key])).filter(Boolean).join(' · '); return <option key={optionValue} value={optionValue}>{optionLabel}</option>; })}</select></label>;
  }
  if (field.type === 'select') return <label className="field" htmlFor={field.name}><span>{field.label}{field.required && <b>*</b>}</span><select {...common}><option value="">Select…</option>{field.options.map(o => <option key={o} value={o}>{o.replaceAll('_', ' ')}</option>)}</select></label>;
  if (field.type === 'textarea') return <label className="field" htmlFor={field.name}><span>{field.label}{field.required && <b>*</b>}</span><textarea {...common} rows="3" /></label>;
  return <label className="field" htmlFor={field.name}><span>{field.label}{field.required && <b>*</b>}</span><input {...common} type={field.type || 'text'} min={field.min} max={field.max} step={field.type === 'number' ? 'any' : undefined}/></label>;
}

function ManagerWorkspace({ user, onLogout, onUserUpdated }) {
  const [active, setActive] = useState('dashboard');
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [data, setData] = useState([]);
  const [overview, setOverview] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [form, setForm] = useState({});
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [lookupOptions, setLookupOptions] = useState({});
  const [lookupLoading, setLookupLoading] = useState(false);
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
  const [contentFormType, setContentFormType] = useState('announcement');
  const [contentEditing, setContentEditing] = useState(null);
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
    setError(''); setData([]); setReportResult(null); setContentResult(null); setFilter(''); setShowForm(false); setEditingId(null);
    if (RESOURCES[active]) { const initial = makeInitialForm(RESOURCES[active]); if ('managerId' in initial && user?.managerId != null) initial.managerId = String(user.managerId); setForm(initial); loadResource(active); }
    else if (active === 'dashboard') { setLoading(true); loadOverview().finally(() => setLoading(false)); }
    else if (active === 'content') {
      setLoading(true);
      getJson('/content/announcements?activeOnly=false')
        .then(rows => setContentResult({ title: 'Announcements', value: rows, kind: 'announcements' }))
        .catch(e => setError(e.message || 'Could not load MongoDB content'))
        .finally(() => setLoading(false));
    }
    // Intentionally reload when the selected page changes.
  }, [active, loadResource, loadOverview, user?.managerId]);

  // Populate relationship selectors from the live API so users don't have to guess FK IDs.
  useEffect(() => {
    const fields = RESOURCES[active]?.fields || [];
    const references = [...new Set(fields.filter(field => field.type === 'reference').map(field => field.reference))];
    if (active === 'payments') references.push('payments');
    if (!references.length) { setLookupOptions({}); setLookupLoading(false); return undefined; }
    let cancelled = false;
    setLookupLoading(true);
    Promise.all(references.map(async key => {
      const rows = await getJson(RESOURCES[key].endpoint);
      return [key, Array.isArray(rows) ? rows : []];
    }))
      .then(entries => { if (!cancelled) setLookupOptions(Object.fromEntries(entries)); })
      .catch(error => { if (!cancelled) { setLookupOptions({}); setError(`Could not load related records for this form: ${error.message || 'API request failed'}`); } })
      .finally(() => { if (!cancelled) setLookupLoading(false); });
    return () => { cancelled = true; };
  }, [active]);

  const navTo = (id) => { setActive(id); setMobileNavOpen(false); };

  const resource = active === 'managers' ? null : RESOURCES[active];
  const visibleData = useMemo(() => {
    if (!filter.trim()) return data;
    const q = filter.toLowerCase();
    return data.filter(row => Object.values(row).some(v => displayValue(v).toLowerCase().includes(q)));
  }, [data, filter]);

  function openAddForm() {
    if (!resource) return;
    setEditingId(null);
    const initial = makeInitialForm(resource);
    if ('managerId' in initial && user?.managerId != null) initial.managerId = String(user.managerId);
    setForm(initial);
    setShowForm(true);
    setError('');
    setNotice('');
  }

  function beginEdit(row) {
    const idField = RESOURCE_IDS[active];
    const fields = editFieldsFor(active);
    setEditingId(row[idField]);
    setForm(Object.fromEntries(fields.map(field => [field.name, inputValue(row[field.name], field)])));
    setShowForm(true);
    setError('');
    setNotice('');
  }

  function closeResourceForm() {
    setShowForm(false);
    setEditingId(null);
    if (resource) {
      const initial = makeInitialForm(resource);
      if ('managerId' in initial && user?.managerId != null) initial.managerId = String(user.managerId);
      setForm(initial);
    }
  }

  async function createResource(e) {
    e.preventDefault(); if (!resource) return;
    const isEditing = editingId !== null;
    const fields = isEditing ? editFieldsFor(active) : resource.fields;
    setLoading(true); setError('');
    try {
      const payload = {};
      fields.forEach(field => {
        const value = form[field.name];
        if (!isEditing && (value === '' || value === null || value === undefined)) return;
        payload[field.name] = (field.type === 'number' || field.type === 'reference' || field.type === 'currentManager')
          ? (value === '' || value === null || value === undefined ? null : Number(value))
          : value;
      });
      if (!isEditing && fields.some(field => field.name === 'managerId')) {
        if (user?.managerId == null || !Number.isFinite(Number(user.managerId))) throw new Error('Your signed-in manager ID is missing. Sign out and sign in again.');
        payload.managerId = Number(user.managerId);
      }
      const result = isEditing
        ? await putJson(`${resource.endpoint}/${editingId}`, payload)
        : await postJson(resource.endpoint, payload);
      flash(result?.message || `${resource.singular} ${isEditing ? 'updated' : 'created'} successfully`);
      closeResourceForm();
      await loadResource(active); await loadOverview();
    } catch (e2) { setError(e2.message || `${isEditing ? 'Update' : 'Create'} request failed`); }
    finally { setLoading(false); }
  }

  async function deleteResource(row) {
    if (!resource || NON_DELETABLE.has(active)) return;
    const idField = RESOURCE_IDS[active];
    const id = row[idField];
    const label = row.name || row.registrationNo || row.routeName || row.passengerName || `${resource.singular} #${id}`;
    if (!window.confirm(`Delete ${resource.singular.toLowerCase()} ${label}? The database may reject this if other records reference it.`)) return;
    setLoading(true); setError(''); setNotice('');
    try {
      const result = await deleteJson(`${resource.endpoint}/${id}`);
      flash(result?.message || `${resource.singular} deleted successfully`);
      await loadResource(active); await loadOverview();
    } catch (e) {
      setError(e.message || `Could not delete this ${resource.singular.toLowerCase()}. Related records may depend on it.`);
    } finally { setLoading(false); }
  }


  async function runReport(report) {
    setLoading(true); setError(''); setReportResult(null);
    try {
      let path = report.path;
      if (report.params) path += `?startDate=${encodeURIComponent(revenueStart)}&endDate=${encodeURIComponent(revenueEnd)}`;
      if (report.passenger) path = `/reports/passenger-travel-history/${encodeURIComponent(passengerId)}`;
      else if (report.passengerTotal) path = `/reports/passenger-total-paid/${encodeURIComponent(passengerId)}`;
      const result = await getJson(path); setReportResult({ title: report.title, value: result });
    } catch (e) { setError(e.message || 'Report failed'); }
    finally { setLoading(false); }
  }

  async function runContentQuery(title, path, kind = 'readonly', parentId = null) {
    setLoading(true); setError(''); setContentResult(null);
    try {
      const value = await getJson(path);
      setContentResult({ title, value, kind, parentId });
    } catch (e) { setError(e.message || 'MongoDB request failed'); }
    finally { setLoading(false); }
  }

  function beginContentEdit(type, row, parentId = null) {
    const rawId = row?._id;
    const id = typeof rawId === 'string' ? rawId : rawId?.$oid;
    if (!id) {
      setError('This MongoDB document has no readable ObjectId. Refresh the result and try again.');
      return;
    }
    const dateInput = value => {
      if (!value) return '';
      const text = String(value);
      if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10);
      const parsed = new Date(value);
      return Number.isNaN(parsed.getTime()) ? '' : parsed.toISOString().slice(0, 10);
    };
    setContentEditing({ type, id, parentId });
    setContentFormType(type);
    setContentForm(prev => ({
      ...prev,
      title: row.title ?? '',
      message: row.message ?? '',
      announcementType: row.announcementType ?? 'GENERAL',
      active: String(row.active ?? true),
      publishFrom: dateInput(row.publishFrom) || dateValue(),
      publishUntil: dateInput(row.publishUntil) || dateValue(-30),
      documentType: row.documentType ?? 'VEHICLE_DOCUMENT',
      filePath: row.filePath ?? '',
      metadata: JSON.stringify(row.metadata ?? {}, null, 2),
      mediaType: row.mediaType ?? 'image',
      caption: row.caption ?? ''
    }));
    if (type === 'document' && parentId != null) setVehicleId(String(parentId));
    if (type === 'media' && parentId != null) setTripId(String(parentId));
    setError(''); setNotice('');
  }

  function resetContentFields() {
    setContentForm(prev => ({
      ...prev,
      title: '', message: '', announcementType: 'GENERAL', active: 'true',
      publishFrom: dateValue(), publishUntil: dateValue(-30),
      documentType: 'VEHICLE_DOCUMENT', filePath: '', metadata: '{}',
      mediaType: 'image', caption: ''
    }));
  }

  function cancelContentEdit() {
    setContentEditing(null);
    setContentFormType('announcement');
    resetContentFields();
    setError('');
  }

  async function deleteContent(type, row, parentId = null) {
    const rawId = row?._id;
    const id = typeof rawId === 'string' ? rawId : rawId?.$oid;
    if (!id) { setError('This MongoDB document has no readable ObjectId. Refresh the result and try again.'); return; }
    const label = row.title || row.caption || `document ${id}`;
    if (!window.confirm(`Delete ${label}? This removes the MongoDB document permanently.`)) return;

    let path, title, queryPath, resultKind, resultParent = parentId;
    if (type === 'announcement') {
      path = `/content/announcements/${encodeURIComponent(id)}`;
      title = 'Announcements'; queryPath = '/content/announcements?activeOnly=false'; resultKind = 'announcements'; resultParent = null;
    } else if (type === 'document') {
      path = `/content/vehicles/${encodeURIComponent(parentId)}/documents/${encodeURIComponent(id)}`;
      title = 'Vehicle documents'; queryPath = `/content/vehicles/${encodeURIComponent(parentId)}/documents`; resultKind = 'documents';
    } else {
      path = `/content/trips/${encodeURIComponent(parentId)}/media/${encodeURIComponent(id)}`;
      title = 'Trip media'; queryPath = `/content/trips/${encodeURIComponent(parentId)}/media`; resultKind = 'media';
    }

    setLoading(true); setError(''); setNotice('');
    try {
      const result = await deleteJson(path);
      flash(result?.message || 'MongoDB document deleted');
      await runContentQuery(title, queryPath, resultKind, resultParent);
    } catch (e) { setError(e.message || 'Could not delete this MongoDB document.'); }
    finally { setLoading(false); }
  }

  async function createContent(e) {
    e.preventDefault(); setLoading(true); setError('');
    const isEditing = contentEditing !== null;
    try {
      let path; let body; let title; let queryPath; let kind; let parentId = null;
      if (contentFormType === 'announcement') {
        path = isEditing
          ? `/content/announcements/${encodeURIComponent(contentEditing.id)}`
          : '/content/announcements';
        body = {
          ...(!isEditing ? { createdByManagerId: Number(user?.managerId) } : {}),
          title: contentForm.title,
          message: contentForm.message,
          announcementType: contentForm.announcementType,
          active: contentForm.active === 'true',
          publishFrom: contentForm.publishFrom,
          publishUntil: contentForm.publishUntil
        };
        title = 'Announcements'; queryPath = '/content/announcements?activeOnly=false'; kind = 'announcements';
      } else if (contentFormType === 'document') {
        parentId = isEditing ? contentEditing.parentId : Number(vehicleId);
        path = isEditing
          ? `/content/vehicles/${encodeURIComponent(parentId)}/documents/${encodeURIComponent(contentEditing.id)}`
          : `/content/vehicles/${Number(vehicleId)}/documents`;
        body = { documentType: contentForm.documentType, title: contentForm.title, filePath: contentForm.filePath, metadata: JSON.parse(contentForm.metadata || '{}') };
        title = 'Vehicle documents'; queryPath = `/content/vehicles/${encodeURIComponent(parentId)}/documents`; kind = 'documents';
      } else {
        parentId = isEditing ? contentEditing.parentId : Number(tripId);
        path = isEditing
          ? `/content/trips/${encodeURIComponent(parentId)}/media/${encodeURIComponent(contentEditing.id)}`
          : `/content/trips/${Number(tripId)}/media`;
        body = { mediaType: contentForm.mediaType, caption: contentForm.caption, filePath: contentForm.filePath, metadata: JSON.parse(contentForm.metadata || '{}') };
        title = 'Trip media'; queryPath = `/content/trips/${encodeURIComponent(parentId)}/media`; kind = 'media';
      }

      const result = isEditing ? await putJson(path, body) : await postJson(path, body);
      flash(result?.message || `${contentFormType === 'announcement' ? 'Announcement' : contentFormType === 'document' ? 'Vehicle document' : 'Trip media'} ${isEditing ? 'updated' : 'saved'} successfully`);
      setContentEditing(null);
      setContentFormType('announcement');
      resetContentFields();
      await runContentQuery(title, queryPath, kind, parentId);
    } catch (e2) { setError(e2.message || 'Could not save content. Check required fields, metadata JSON, and related IDs.'); }
    finally { setLoading(false); }
  }

  const totalRecords = Object.entries(overview).filter(([k,v]) => !k.startsWith('_') && Array.isArray(v)).reduce((sum, [,v]) => sum + v.length, 0);
  const pageTitle = active === 'managers' ? 'Manager accounts' : resource?.label || MAIN_NAV.find(n => n.id === active)?.label || 'Overview';

  return <div className="app-shell">
    <aside className={`sidebar ${mobileNavOpen ? 'open' : ''}`}>
      <div className="brand"><div className="brand-mark"><Bus size={23}/></div><div><strong>SmartMove</strong><span>TRANSPORT SOLUTIONS</span></div><button className="close-nav icon-btn" onClick={() => setMobileNavOpen(false)} aria-label="Close navigation"><X size={18}/></button></div>
      <div className="workspace-label">WORKSPACE</div>
      <nav className="side-nav" aria-label="Main navigation">
        {MAIN_NAV.map(item => { const Icon = item.icon; return <button key={item.id} className={`nav-item ${active === item.id ? 'active' : ''}`} onClick={() => navTo(item.id)}><Icon size={18}/><span>{item.label}</span>{item.id === 'content' && <span className="nav-tag">MongoDB</span>}</button>; })}
      </nav>
      <div className="sidebar-bottom"><div className="connection-indicator"><span className="pulse-dot"/><div><strong>Local environment</strong><small>API · localhost:8080</small></div></div><div className="sidebar-note">Development workspace<br/>Keep database containers running.</div></div>
    </aside>
    {mobileNavOpen && <button className="nav-backdrop" aria-label="Close menu" onClick={() => setMobileNavOpen(false)}/>}
    <main className="main-area">
      <header className="topbar"><div className="topbar-left"><button className="icon-btn menu-button" onClick={() => setMobileNavOpen(true)} aria-label="Open navigation"><Menu size={20}/></button><div className="breadcrumb">SmartMove <span>/</span> <b>{pageTitle}</b></div></div><div className="top-actions"><span className="local-badge"><span className="pulse-dot"/> Local system</span><button className="icon-btn" aria-label="Refresh data" onClick={() => resource ? loadResource(active) : active === 'dashboard' ? loadOverview() : flash('Use the Refresh button on the selected panel.')}><RefreshCw size={17}/></button><button type="button" className="avatar profile-avatar" title="My profile" aria-label="Open my profile" onClick={() => setShowProfile(true)}>{(user?.name || 'SM').split(/\s+/).map(part => part[0]).join('').slice(0, 2).toUpperCase()}</button><button className="icon-btn" title="Log out" aria-label="Log out" onClick={onLogout}><LogOut size={17}/></button></div></header>
      <div className="page-content">
        <div className="page-heading"><div><div className="eyebrow">TRANSPORT OPERATIONS</div><h1>{active === 'dashboard' ? 'Good to see you.' : pageTitle}</h1><p>{active === 'dashboard' ? 'Here is a live overview of the SmartMove workspace.' : resource ? `Manage ${resource.label.toLowerCase()} using your Oracle database.` : active === 'reports' ? 'Review operational reports generated from Oracle.' : 'Manage announcements, vehicle documents and trip media stored in MongoDB.'}</p></div><div className="heading-actions">{resource && <button className="btn primary" onClick={() => showForm ? closeResourceForm() : openAddForm()}><Plus size={17}/>{showForm ? 'Close form' : `Add ${resource.singular}`}</button>}</div></div>
        {error && <div className="alert error"><CircleAlert size={18}/><div><strong>Request failed</strong><span>{error}</span></div><button className="icon-btn" onClick={() => setError('')} aria-label="Dismiss error"><X size={16}/></button></div>}
        {notice && <div className="alert success"><CheckCircle2 size={18}/><span>{notice}</span><button className="icon-btn" onClick={() => setNotice('')} aria-label="Dismiss notification"><X size={16}/></button></div>}

        {active === 'dashboard' && <Dashboard overview={overview} totalRecords={totalRecords} onNavigate={navTo} onRefresh={loadOverview}/>}
        {active === 'managers' && <section className="panel manager-protection-panel"><div className="panel-head"><div><h2>Manager account protection</h2><p>Single-manager mode is enabled for this coursework application.</p></div><ShieldCheck size={21}/></div><p>Creating, editing, and deleting manager accounts is disabled to prevent loss of access and protect records that reference manager IDs. Existing manager rows are retained for foreign-key integrity. Use your profile button in the top-right corner to update your own details.</p></section>}
        {resource && <section className="panel">
          {showForm && <form className="create-form" onSubmit={createResource}><div className="panel-head"><div><h2>{editingId !== null ? `Edit ${resource.singular}` : `Add ${resource.singular}`}</h2><p>{editingId !== null ? 'Update the editable fields below. Passwords are never displayed or changed here.' : 'Required fields are marked with an asterisk.'}</p></div><button type="button" className="icon-btn" onClick={closeResourceForm} aria-label="Close form"><X size={18}/></button></div><div className="form-grid">{(editingId !== null ? editFieldsFor(active) : resource.fields).map(field => <Field key={field.name} field={field} value={form[field.name]} lookupOptions={lookupOptions} onChange={(name, val) => setForm(prev => ({ ...prev, [name]: val }))}/>)}</div><div className="form-footer"><span className="help-text">{active === 'bookings' || active === 'payments' ? 'Booking fare is calculated by the backend at LKR 20/km. Payment amount comes from the saved booking fare; a fare is not the same as a payment. Transaction history is retained.' : active === 'maintenance' ? 'Select an existing vehicle, technician and manager. Vehicle availability status remains managed in Vehicles.' : 'Choose related records from the dropdowns to pass valid foreign keys.'}</span><div className="crud-form-buttons"><button type="button" className="btn secondary" onClick={closeResourceForm} disabled={loading}>Cancel</button><button className="btn primary" type="submit" disabled={loading || lookupLoading}>{loading ? 'Saving…' : <>{editingId !== null ? 'Save changes' : `Save ${resource.singular}`}</>}</button></div></div></form>}
          <div className="panel-head"><div><h2>All {resource.label.toLowerCase()}</h2><p>{data.length} records returned by the API</p></div><div className="table-actions"><div className="search-box"><Search size={16}/><input value={filter} onChange={e => setFilter(e.target.value)} placeholder="Search records…"/></div><button className="btn secondary" onClick={() => loadResource(active)} disabled={loading}><RefreshCw size={15}/> Refresh</button></div></div>
          {loading ? <Loading/> : <DataTable data={visibleData} columns={resource.columns} emptyMessage={`No ${resource.label.toLowerCase()} found.`} renderActions={row => <div className="crud-actions"><button type="button" className="crud-action edit" onClick={() => beginEdit(row)} disabled={loading} title={`Edit ${resource.singular}`}><Pencil size={14}/> Edit</button>{!NON_DELETABLE.has(active) && <button type="button" className="crud-action delete" onClick={() => deleteResource(row)} disabled={loading} title={`Delete ${resource.singular}`}><Trash2 size={14}/> Delete</button>}</div>}/>}
          {(active === 'bookings' || active === 'payments') && data.length > 0 && <div className="panel-foot"><span>Transaction history is protected.</span><span>Bookings are cancelled through status changes; payments are updated rather than deleted.</span></div>}
        </section>}
        {active === 'reports' && <ReportsPanel loading={loading} result={reportResult} start={revenueStart} end={revenueEnd} setStart={setRevenueStart} setEnd={setRevenueEnd} passengerId={passengerId} setPassengerId={setPassengerId} onRun={runReport}/>}
        {active === 'content' && <ContentPanel loading={loading} result={contentResult} routeId={routeId} setRouteId={setRouteId} keyword={keyword} setKeyword={setKeyword} vehicleId={vehicleId} setVehicleId={setVehicleId} tripId={tripId} setTripId={setTripId} onQuery={runContentQuery} formType={contentFormType} setFormType={setContentFormType} form={contentForm} setForm={setContentForm} onCreate={createContent} managerId={user?.managerId} editing={contentEditing} onEdit={beginContentEdit} onDelete={deleteContent} onCancelEdit={cancelContentEdit}/>}
        <footer className="page-footer"><span>SmartMove Transport Solutions</span><span><ShieldCheck size={14}/> Local coursework build · API <a href="http://localhost:8080/api/health/databases" target="_blank" rel="noreferrer">health check <ExternalLink size={12}/></a></span></footer>
      </div>
    </main>
    {showProfile && <ProfilePanel onClose={() => setShowProfile(false)} onUserUpdated={onUserUpdated}/>}
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
  return <div className="report-grid">{REPORTS.map(report => <section key={report.id} className="panel report-card"><div className="report-icon"><FileText size={20}/></div><h2>{report.title}</h2><p>{report.description}</p>{report.params && <div className="report-params"><label className="field"><span>Start date</span><input type="date" value={start} onChange={e => setStart(e.target.value)}/></label><label className="field"><span>End date</span><input type="date" value={end} onChange={e => setEnd(e.target.value)}/></label></div>}{(report.passenger || report.passengerTotal) && <label className="field report-param-single"><span>Passenger ID</span><input type="number" min="1" value={passengerId} onChange={e => setPassengerId(e.target.value)}/></label>}<button className="btn primary full" disabled={loading} onClick={() => onRun(report)}>{loading ? 'Running…' : 'Run report'}<ArrowUpRight size={16}/></button></section>)}{result && <section className="panel report-result"><div className="panel-head"><div><h2>{result.title}</h2><p>Results captured from Oracle PL/SQL report procedures or functions.</p></div><span className="result-badge"><CheckCircle2 size={15}/> Complete</span></div>{Array.isArray(result.value?.lines) ? <div className="plsql-report-output"><pre>{result.value.lines.join('\n') || 'The procedure returned no output.'}</pre><p>Executed: {result.value.source || 'Oracle PL/SQL'}</p></div> : Array.isArray(result.value) ? <DataTable data={result.value}/> : <pre className="json-view">{JSON.stringify(result.value, null, 2)}</pre>}</section>}</div>;
}

function ContentPanel({ loading, result, routeId, setRouteId, keyword, setKeyword, vehicleId, setVehicleId, tripId, setTripId, onQuery, formType, setFormType, form, setForm, onCreate, managerId, editing, onEdit, onDelete, onCancelEdit }) {
  const updateForm = (name, value) => setForm(prev => ({ ...prev, [name]: value }));
  const resultEditableType = result?.kind === 'announcements' ? 'announcement'
    : result?.kind === 'documents' ? 'document'
    : result?.kind === 'media' ? 'media' : null;
  return <div className="content-layout">
    <section className="panel content-query-panel">
      <div className="panel-head"><div><h2>Content & media library</h2><p>Manage MongoDB announcements and linked document/media metadata.</p></div><span className="nosql-chip">MongoDB</span></div>
      <div className="query-block">
        <h3>Passenger reviews</h3>
        <div className="query-controls"><label className="field"><span>Route ID</span><input type="number" min="1" value={routeId} onChange={e => setRouteId(e.target.value)}/></label><button className="btn secondary" disabled={loading} onClick={() => onQuery('Reviews for route', `/content/reviews?routeId=${encodeURIComponent(routeId)}`, 'reviews')}>Find by route</button></div>
        <div className="query-controls"><label className="field"><span>Keyword in comment</span><input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="late, delay…"/></label><button className="btn secondary" disabled={loading} onClick={() => onQuery('Reviews matching keyword', `/content/reviews?keyword=${encodeURIComponent(keyword)}`, 'reviews')}>Search comments</button></div>
      </div>
      <div className="query-block">
        <h3>Announcements</h3>
        <button className="quick-query" disabled={loading} onClick={() => onQuery('All announcements', '/content/announcements?activeOnly=false', 'announcements')}><span>View all announcements (active and inactive)</span><ArrowUpRight size={16}/></button>
      </div>
      <div className="query-block">
        <h3>Ratings</h3>
        <div className="button-stack">{CONTENT_QUERIES.map(q => <button className="quick-query" key={q.id} disabled={loading} onClick={() => onQuery(q.title, q.path, 'ratings')}><span>{q.title}</span><ArrowUpRight size={16}/></button>)}</div>
      </div>
      <div className="query-block">
        <h3>Vehicle documents</h3>
        <div className="query-controls"><label className="field"><span>Vehicle ID</span><input type="number" min="1" value={vehicleId} onChange={e => setVehicleId(e.target.value)}/></label><button className="btn secondary" disabled={loading || !vehicleId} onClick={() => onQuery('Vehicle document metadata', `/content/vehicles/${encodeURIComponent(vehicleId)}/documents`, 'documents', Number(vehicleId))}>View documents</button></div>
      </div>
      <div className="query-block last">
        <h3>Trip media</h3>
        <div className="query-controls"><label className="field"><span>Trip ID</span><input type="number" min="1" value={tripId} onChange={e => setTripId(e.target.value)}/></label><button className="btn secondary" disabled={loading || !tripId} onClick={() => onQuery('Trip media metadata', `/content/trips/${encodeURIComponent(tripId)}/media`, 'media', Number(tripId))}>View media</button></div>
      </div>
    </section>

    <section className="panel content-create-panel">
      <div className="panel-head"><div><h2>{editing ? `Edit ${editing.type === 'announcement' ? 'announcement' : editing.type === 'document' ? 'vehicle document' : 'trip media'}` : 'Add content or media'}</h2><p>{editing ? 'Update the selected MongoDB document.' : 'Create announcements and linked document/media metadata.'}</p></div><span className="create-icon">{editing ? <Pencil size={17}/> : <Plus size={17}/>}</span></div>
      <label className="field"><span>Content type</span><select value={formType} disabled={!!editing} onChange={e => setFormType(e.target.value)}><option value="announcement">Announcement</option><option value="document">Vehicle document metadata</option><option value="media">Trip media metadata</option></select></label>
      <form onSubmit={onCreate} className="content-form">
        {formType === 'announcement' && <>
          {!editing && <label className="field"><span>Assigned manager ID</span><input required type="number" min="1" value={managerId ?? ''} readOnly aria-readonly="true"/><small className="help-text">Automatically assigned from your signed-in manager account.</small></label>}
          <label className="field"><span>Title *</span><input required value={form.title} onChange={e => updateForm('title', e.target.value)}/></label>
          <label className="field"><span>Message *</span><textarea required rows="3" value={form.message} onChange={e => updateForm('message', e.target.value)}/></label>
          <div className="form-grid two"><label className="field"><span>Type</span><input required value={form.announcementType} onChange={e => updateForm('announcementType', e.target.value)}/></label><label className="field"><span>Active</span><select value={form.active} onChange={e => updateForm('active', e.target.value)}><option value="true">Yes</option><option value="false">No</option></select></label></div>
          <div className="form-grid two"><label className="field"><span>Publish from *</span><input required type="date" value={form.publishFrom} onChange={e => updateForm('publishFrom', e.target.value)}/></label><label className="field"><span>Publish until *</span><input required type="date" min={form.publishFrom || undefined} value={form.publishUntil} onChange={e => updateForm('publishUntil', e.target.value)}/></label></div>
        </>}
        {formType === 'document' && <>
          <div className="form-grid two"><label className="field"><span>Vehicle ID *</span><input required min="1" type="number" value={vehicleId} disabled={!!editing} onChange={e => setVehicleId(e.target.value)}/></label><label className="field"><span>Document type *</span><input required value={form.documentType} onChange={e => updateForm('documentType', e.target.value)}/></label></div>
          <label className="field"><span>Title *</span><input required value={form.title} onChange={e => updateForm('title', e.target.value)}/></label>
          <label className="field"><span>File path/reference *</span><input required value={form.filePath} onChange={e => updateForm('filePath', e.target.value)} placeholder="/uploads/vehicles/document.pdf"/></label>
          <label className="field"><span>Metadata JSON</span><textarea rows="2" value={form.metadata} onChange={e => updateForm('metadata', e.target.value)}/></label>
          <p className="help-text">This records metadata/path only; it does not upload an actual file.</p>
        </>}
        {formType === 'media' && <>
          <label className="field"><span>Trip ID *</span><input required min="1" type="number" value={tripId} disabled={!!editing} onChange={e => setTripId(e.target.value)}/></label>
          <div className="form-grid two"><label className="field"><span>Media type</span><select value={form.mediaType} onChange={e => updateForm('mediaType', e.target.value)}><option>image</option><option>video</option><option>audio</option></select></label><label className="field"><span>Caption *</span><input required value={form.caption} onChange={e => updateForm('caption', e.target.value)}/></label></div>
          <label className="field"><span>File path/reference *</span><input required value={form.filePath} onChange={e => updateForm('filePath', e.target.value)} placeholder="/uploads/trips/1/photo.jpg"/></label>
          <label className="field"><span>Metadata JSON</span><textarea rows="2" value={form.metadata} onChange={e => updateForm('metadata', e.target.value)}/></label>
          <p className="help-text">This records metadata/path only; it does not upload an actual file.</p>
        </>}
        <div className="crud-form-buttons">
          <button className="btn primary" type="submit" disabled={loading}>{loading ? 'Saving…' : editing ? 'Save changes' : 'Save content'}</button>
          {editing && <button className="btn secondary" type="button" onClick={onCancelEdit} disabled={loading}>Cancel edit</button>}
        </div>
      </form>
    </section>

    {result && <section className="panel content-result">
      <div className="panel-head"><div><h2>{result.title}</h2><p>{Array.isArray(result.value) ? `${result.value.length} records returned` : 'Response from MongoDB API'}</p></div><span className="nosql-chip">MongoDB result</span></div>
      {Array.isArray(result.value) && result.value.length > 0
        ? <DataTable data={result.value} renderActions={resultEditableType ? row => <div className="crud-actions"><button type="button" className="crud-action edit" onClick={() => onEdit(resultEditableType, row, result.parentId)} disabled={loading} title="Edit content"><Pencil size={14}/> Edit</button><button type="button" className="crud-action delete" onClick={() => onDelete(resultEditableType, row, result.parentId)} disabled={loading} title="Delete content"><Trash2 size={14}/> Delete</button></div> : undefined}/>
        : Array.isArray(result.value)
          ? <div className="empty-state"><div className="empty-icon"><Database size={22}/></div><strong>No matching documents</strong><span>Choose another filter or create content using the form.</span></div>
          : <pre className="json-view">{JSON.stringify(result.value, null, 2)}</pre>}
      {resultEditableType && <div className="panel-foot"><span>Edits affect MongoDB metadata only.</span><span>Passenger reviews and rating summaries are read-only in this screen.</span></div>}
    </section>}
  </div>;
}

export default ManagerWorkspace;
