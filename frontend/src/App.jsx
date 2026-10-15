import { useEffect, useState } from 'react';
import { Activity, ArrowRight, Bus, CalendarClock, CheckCircle2, ClipboardList, LogIn, LogOut, MessageSquareText, ShieldCheck, Ticket, UserRound, Users, Megaphone, RefreshCw, Star } from 'lucide-react';
import { getJson, postJson } from './api.js';
import ManagerWorkspace from './ManagerWorkspace.jsx';
import ProfilePanel from './ProfilePanel.jsx';
import './auth.css';

function App() {
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(true);
  const [authError, setAuthError] = useState('');

  useEffect(() => {
    let active = true;
    getJson('/auth/me')
      .then(payload => { if (active) setUser(payload?.user || null); })
      .catch(() => { if (active) setUser(null); })
      .finally(() => { if (active) setChecking(false); });
    return () => { active = false; };
  }, []);

  async function signOut() {
    try { await postJson('/auth/logout', {}); } catch { /* clear local view even if the session already expired */ }
    setUser(null);
    setAuthError('');
  }

  if (checking) return <div className="auth-loading"><span className="auth-spinner"/><span>Checking your SmartMove session…</span></div>;
  const syncProfile = (updated) => setUser(current => current ? { ...current, ...updated } : current);
  if (user?.role === 'MANAGER') return <ManagerWorkspace user={user} onLogout={signOut} onUserUpdated={syncProfile}/>;
  if (user?.role === 'PASSENGER') return <PassengerPortal user={user} onLogout={signOut} onUserUpdated={syncProfile}/>;
  return <AuthScreen onAuthenticated={setUser} externalError={authError} setExternalError={setAuthError}/>;
}

function AuthScreen({ onAuthenticated }) {
  const [mode, setMode] = useState('login');
  const [role, setRole] = useState('MANAGER');
  const [setupAvailable, setSetupAvailable] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [fields, setFields] = useState({ name: '', nic: '', phone: '', email: '', password: '', passengerType: 'REGULAR' });

  useEffect(() => {
    getJson('/auth/setup-status').then(res => setSetupAvailable(Boolean(res?.managerSetupAvailable))).catch(() => setSetupAvailable(false));
  }, []);

  const update = (key, value) => setFields(prev => ({ ...prev, [key]: value }));
  const go = (next) => { setMode(next); setError(''); setMessage(''); };

  async function submit(event) {
    event.preventDefault(); setLoading(true); setError(''); setMessage('');
    try {
      let result;
      if (mode === 'login') {
        result = await postJson('/auth/login', { email: fields.email, password: fields.password, role });
      } else if (mode === 'setup') {
        result = await postJson('/auth/setup-manager', { name: fields.name, nic: fields.nic, phone: fields.phone, email: fields.email, password: fields.password });
      } else {
        result = await postJson('/auth/register-passenger', { name: fields.name, nic: fields.nic, phone: fields.phone, email: fields.email, password: fields.password, passengerType: fields.passengerType });
      }
      if (result?.user) onAuthenticated(result.user);
      else setMessage(result?.message || 'Request completed.');
    } catch (err) {
      setError(err.message || 'Unable to complete the request. Check the backend is running.');
    } finally { setLoading(false); }
  }

  const isLogin = mode === 'login';
  const isSetup = mode === 'setup';
  const isPassengerRegistration = mode === 'register';

  return <div className="auth-screen">
    <section className="auth-brand-panel">
      <div className="auth-brand"><span className="auth-brand-mark"><Bus size={28}/></span><span><strong>SmartMove</strong><small>TRANSPORT SOLUTIONS</small></span></div>
      <div className="auth-brand-copy"><span className="auth-eyebrow"><Activity size={15}/> SMARTER JOURNEYS, CONNECTED</span><h1>Move people.<br/><em>Move forward.</em></h1><p>A connected workspace for transport operations and a simpler way for passengers to plan every journey.</p>
        <div className="auth-feature"><span><ShieldCheck size={19}/></span><div><strong>Role-aware access</strong><small>Separate tools for managers and passengers</small></div></div>
        <div className="auth-feature"><span><Bus size={19}/></span><div><strong>Live transport data</strong><small>Powered by your Oracle and MongoDB databases</small></div></div>
      </div>
      <div className="auth-brand-footer">SMARTMOVE TRANSPORT SOLUTIONS <span>•</span> COURSEWORK BUILD</div>
    </section>
    <section className="auth-form-panel">
      <div className="auth-form-wrap">
        <div className="auth-mobile-brand"><span className="auth-brand-mark"><Bus size={23}/></span><strong>SmartMove</strong></div>
        <span className="auth-section-label">WELCOME TO SMARTMOVE</span>
        <h2>{isLogin ? 'Sign in to your account' : isSetup ? 'Set up your manager account' : 'Create a passenger account'}</h2>
        <p className="auth-subtitle">{isLogin ? 'Enter your account details to continue.' : isSetup ? 'This one-time setup creates the first account with manager access.' : 'Register to browse scheduled journeys and manage your bookings.'}</p>

        {isLogin && <div className="auth-role-switch" role="group" aria-label="Account type">
          <button className={role === 'MANAGER' ? 'selected' : ''} type="button" onClick={() => setRole('MANAGER')}><ShieldCheck size={17}/> Manager</button>
          <button className={role === 'PASSENGER' ? 'selected' : ''} type="button" onClick={() => setRole('PASSENGER')}><UserRound size={17}/> Passenger</button>
        </div>}

        {error && <div className="auth-alert error" role="alert">{error}</div>}
        {message && <div className="auth-alert success" role="status">{message}</div>}
        {!isLogin && isSetup && <div className="auth-info"><ShieldCheck size={17}/><span>First-use setup is available because your sample manager rows have placeholder passwords. It closes after the first real manager account is created.</span></div>}

        <form className="auth-form" onSubmit={submit}>
          {!isLogin && <label><span>Full name</span><input required value={fields.name} onChange={e => update('name', e.target.value)} autoComplete="name" placeholder="Enter your full name"/></label>}
          {!isLogin && isSetup && <label><span>NIC / Manager identification</span><input required value={fields.nic} onChange={e => update('nic', e.target.value)} placeholder="Enter a unique identification number"/></label>}
          {!isLogin && !isSetup && <><label><span>NIC (optional)</span><input value={fields.nic} onChange={e => update('nic', e.target.value)} placeholder="NIC or national ID"/></label><label><span>Phone (optional)</span><input value={fields.phone} onChange={e => update('phone', e.target.value)} autoComplete="tel" placeholder="Phone number"/></label><label><span>Passenger type</span><select value={fields.passengerType} onChange={e => update('passengerType', e.target.value)}><option value="REGULAR">Regular</option><option value="STUDENT">Student</option><option value="CORPORATE">Corporate</option></select></label></>}
          {!isLogin && isSetup && <label><span>Phone (optional)</span><input value={fields.phone} onChange={e => update('phone', e.target.value)} autoComplete="tel" placeholder="Phone number"/></label>}
          <label><span>Email address</span><input required type="email" value={fields.email} onChange={e => update('email', e.target.value)} autoComplete="email" placeholder="you@example.com"/></label>
          <label><span>Password</span><input required type="password" minLength="8" value={fields.password} onChange={e => update('password', e.target.value)} autoComplete={isLogin ? 'current-password' : 'new-password'} placeholder={isLogin ? 'Enter your password' : 'At least 8 characters'}/>{!isLogin && <small className="auth-field-hint">Use at least 8 characters.</small>}</label>
          <button type="submit" className="auth-submit" disabled={loading}>{loading ? 'Please wait…' : isLogin ? 'Sign in' : isSetup ? 'Create manager account' : 'Create passenger account'} <ArrowRight size={17}/></button>
        </form>

        <div className="auth-switches">
          {isLogin ? <>
            {role === 'PASSENGER' && <button type="button" onClick={() => go('register')}>New passenger? <strong>Create an account</strong></button>}
            {role === 'MANAGER' && setupAvailable && <button type="button" onClick={() => go('setup')}>First time here? <strong>Set up the manager account</strong></button>}
            {role === 'MANAGER' && !setupAvailable && <span>Manager access is available to registered manager accounts.</span>}
          </> : <button type="button" onClick={() => go('login')}>Already registered? <strong>Return to sign in</strong></button>}
        </div>
        <p className="auth-legal">Local coursework application. Passwords are hashed on the server; never reuse a personal password.</p>
      </div>
    </section>
  </div>;
}

function PassengerPortal({ user, onLogout, onUserUpdated }) {
  const [section, setSection] = useState('trips');
  const [trips, setTrips] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showProfile, setShowProfile] = useState(false);
  const [seats, setSeats] = useState({});
  const [reviewForm, setReviewForm] = useState({ tripId: '', rating: '5', category: 'review', comments: '' });

  async function loadPortal(next = section) {
    setLoading(true); setError('');
    try {
      if (next === 'trips') setTrips(await getJson('/passenger/trips'));
      if (next === 'bookings') setBookings(await getJson('/passenger/bookings'));
      if (next === 'announcements') setAnnouncements(await getJson('/passenger/announcements'));
      if (next === 'reviews') { const tripRows = trips.length ? trips : await getJson('/passenger/trips'); if (!trips.length) setTrips(tripRows); setReviews(await getJson('/passenger/reviews')); if (!reviewForm.tripId && tripRows.length) setReviewForm(f => ({ ...f, tripId: String(tripRows[0].tripId) })); }
    } catch (err) { setError(err.message || 'Could not load passenger information.'); }
    finally { setLoading(false); }
  }

  useEffect(() => { loadPortal(section); /* load only the selected section */ }, [section]);

  // Keep passenger-facing data fresh while the portal is open. The data remains
  // session-scoped on the server; polling only refreshes the signed-in passenger's view.
  useEffect(() => {
    const refreshableSections = new Set(['trips', 'bookings', 'announcements', 'reviews']);
    if (!refreshableSections.has(section)) return undefined;

    let active = true;
    let inFlight = false;
    const refreshSelected = async () => {
      if (!active || inFlight || document.visibilityState !== 'visible') return;
      inFlight = true;
      try {
        if (section === 'trips') {
          const rows = await getJson('/passenger/trips');
          if (active) setTrips(Array.isArray(rows) ? rows : []);
        } else if (section === 'bookings') {
          const rows = await getJson('/passenger/bookings');
          if (active) setBookings(Array.isArray(rows) ? rows : []);
        } else if (section === 'announcements') {
          const rows = await getJson('/passenger/announcements');
          if (active) setAnnouncements(Array.isArray(rows) ? rows : []);
        } else if (section === 'reviews') {
          const rows = await getJson('/passenger/reviews');
          if (active) setReviews(Array.isArray(rows) ? rows : []);
        }
      } catch {
        // Keep the existing display during transient failures; the manual Refresh button shows errors.
      } finally {
        inFlight = false;
      }
    };

    const onVisibilityChange = () => { if (document.visibilityState === 'visible') refreshSelected(); };
    const timer = window.setInterval(refreshSelected, 20000);
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      active = false;
      window.clearInterval(timer);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [section]);

  async function reserve(trip) {
    const seatNo = String(seats[trip.tripId] || '').trim();
    if (!seatNo) { setError('Enter your requested seat number first.'); return; }
    setLoading(true); setError(''); setNotice('');
    try {
      const response = await postJson('/passenger/bookings', { tripId: trip.tripId, seatNo });
      setNotice(`${response.message} Booking #${response.bookingId}; fare LKR ${Number(response.fareLkr).toLocaleString()}.`);
      setSection('bookings');
    } catch (err) { setError(err.message || 'Unable to create the booking.'); }
    finally { setLoading(false); }
  }

  async function cancel(bookingId) {
    setLoading(true); setError(''); setNotice('');
    try { const response = await postJson(`/passenger/bookings/${bookingId}/cancel`, {}); setNotice(response.message); await loadPortal('bookings'); }
    catch (err) { setError(err.message || 'Unable to cancel this booking.'); }
    finally { setLoading(false); }
  }

  async function submitReview(event) {
    event.preventDefault(); setLoading(true); setError(''); setNotice('');
    try { const response = await postJson('/passenger/reviews', { ...reviewForm, tripId: Number(reviewForm.tripId), rating: Number(reviewForm.rating) }); setNotice(response.message); setReviewForm(f => ({ ...f, comments: '' })); await loadPortal('reviews'); }
    catch (err) { setError(err.message || 'Unable to save review.'); }
    finally { setLoading(false); }
  }

  const menu = [
    { id: 'trips', label: 'Browse trips', icon: CalendarClock },
    { id: 'bookings', label: 'My bookings', icon: Ticket },
    { id: 'announcements', label: 'Announcements', icon: Megaphone },
    { id: 'reviews', label: 'My reviews', icon: MessageSquareText },
    { id: 'profile', label: 'My profile', icon: UserRound }
  ];
  const title = menu.find(x => x.id === section)?.label || 'Passenger portal';

  return <div className="passenger-shell">
    <aside className="passenger-sidebar">
      <div className="auth-brand passenger-brand"><span className="auth-brand-mark"><Bus size={25}/></span><span><strong>SmartMove</strong><small>PASSENGER PORTAL</small></span></div>
      <div className="passenger-welcome"><span className="passenger-avatar"><UserRound size={22}/></span><div><strong>{user.name}</strong><small>{user.email}</small></div></div>
      <div className="workspace-label">MY JOURNEY</div>
      <nav className="side-nav">{menu.map(item => { const Icon = item.icon; const activeItem = item.id === 'profile' ? showProfile : section === item.id; return <button key={item.id} className={`nav-item ${activeItem ? 'active' : ''}`} onClick={() => item.id === 'profile' ? setShowProfile(true) : setSection(item.id)}><Icon size={18}/><span>{item.label}</span></button>; })}</nav>
      <div className="sidebar-bottom"><div className="connection-indicator"><span className="pulse-dot"/><div><strong>Connected</strong><small>SmartMove services</small></div></div><button className="passenger-logout" onClick={onLogout}><LogOut size={16}/> Sign out</button></div>
    </aside>
    <main className="passenger-main">
      <header className="passenger-topbar"><div><span className="auth-section-label">PASSENGER WORKSPACE</span><h1>{title}</h1></div><button className="btn secondary" onClick={() => loadPortal(section)} disabled={loading}><RefreshCw size={16}/> Refresh</button></header>
      <section className="passenger-content">
        <div className="passenger-banner"><div><span className="auth-eyebrow"><Activity size={14}/> YOUR JOURNEY, SIMPLIFIED</span><h2>Hello, {user.name.split(' ')[0]}.</h2><p>Browse upcoming services, keep track of reservations and stay informed before you travel.</p></div><div className="passenger-banner-icon"><Bus size={48}/></div></div>
        {error && <div className="auth-alert error">{error}</div>}{notice && <div className="auth-alert success">{notice}</div>}
        {loading && <div className="portal-loading"><span className="auth-spinner"/> Loading your information…</div>}
        {!loading && section === 'trips' && <section className="portal-panel"><div className="portal-panel-head"><div><h2>Upcoming trips</h2><p>Choose a scheduled service with seats available.</p></div><span className="portal-count">{trips.length} trips</span></div>
          {trips.length === 0 ? <div className="portal-empty">No upcoming trips with seats are currently available.</div> : <div className="trip-card-grid">{trips.map(trip => <article className="trip-card" key={trip.tripId}><div className="trip-card-top"><span className="trip-icon"><Bus size={20}/></span><span className="seats-pill">{trip.availableSeats} seats left</span></div><h3>{trip.routeName}</h3><p className="trip-route-line">{trip.startLocation} <span>→</span> {trip.endLocation}</p><div className="trip-details"><span><CalendarClock size={15}/>{formatDate(trip.tripDate)}</span><span>{trip.departureTime} – {trip.arrivalTime}</span><span>{trip.vehicleType} · {trip.distanceKm} km</span></div><div className="trip-card-bottom"><div><small>Estimated fare</small><strong>LKR {money(trip.estimatedFare)}</strong></div><span className="trip-fare-note">LKR 20 / km</span></div><div className="reserve-row"><label><span>Seat number</span><input value={seats[trip.tripId] || ''} onChange={e => setSeats(prev => ({ ...prev, [trip.tripId]: e.target.value }))} placeholder="e.g. 4"/></label><button className="auth-submit compact" disabled={loading} onClick={() => reserve(trip)}>Reserve <ArrowRight size={15}/></button></div></article>)}</div>}
          <p className="portal-note">This is an estimated booking fare (LKR 20/km), not a payment receipt. Booking requests are checked against Oracle seat availability; payment status appears under My bookings after the operator records a payment.</p>
        </section>}
        {!loading && section === 'bookings' && <section className="portal-panel"><div className="portal-panel-head"><div><h2>My bookings</h2><p>Only reservations linked to your signed-in account are shown.</p></div><span className="portal-count">{bookings.length} records</span></div>{bookings.length ? <div className="portal-table-wrap"><table><thead><tr><th>Booking</th><th>Journey</th><th>Date</th><th>Seat</th><th>Fare</th><th>Status</th><th>Payment</th><th></th></tr></thead><tbody>{bookings.map(b => <tr key={b.bookingId}><td>#{b.bookingId}</td><td>{b.routeName}</td><td>{formatDate(b.tripDate)} {b.departureTime}</td><td>{b.seatNo}</td><td>LKR {money(b.price)}</td><td><span className={`portal-status ${String(b.status).toLowerCase()}`}>{b.status}</span></td><td>{b.paymentStatus || 'NOT RECORDED'}</td><td>{['PENDING','CONFIRMED'].includes(b.status) && <button className="text-action danger" onClick={() => cancel(b.bookingId)}>Cancel</button>}</td></tr>)}</tbody></table></div> : <div className="portal-empty">You don't have any bookings yet. Browse trips to reserve a seat.</div>}</section>}
        {!loading && section === 'announcements' && <section className="portal-panel"><div className="portal-panel-head"><div><h2>Travel announcements</h2><p>Service updates and reminders from SmartMove.</p></div><span className="portal-count">{announcements.length} updates</span></div>{announcements.length ? <div className="announcement-list">{announcements.map((a, i) => <article className="announcement-card" key={String(a._id || i)}><span className="announcement-icon"><Megaphone size={18}/></span><div><div className="announcement-meta">{String(a.announcementType || 'UPDATE').replaceAll('_', ' ')} · {a.createdAt ? formatDate(a.createdAt) : 'SmartMove'}</div><h3>{a.title}</h3><p>{a.message}</p></div></article>)}</div> : <div className="portal-empty">No active announcements at the moment.</div>}</section>}
        {!loading && section === 'reviews' && <div className="portal-review-layout"><section className="portal-panel"><div className="portal-panel-head"><div><h2>Write a review</h2><p>You can review a journey you've booked.</p></div><Star size={19} className="gold-icon"/></div><form className="portal-review-form" onSubmit={submitReview}><label><span>Trip</span><select required value={reviewForm.tripId} onChange={e => setReviewForm(f => ({ ...f, tripId: e.target.value }))}><option value="">Choose a trip ID</option>{trips.map(t => <option key={t.tripId} value={t.tripId}>Trip {t.tripId} · {t.routeName} ({formatDate(t.tripDate)})</option>)}</select></label><label><span>Rating</span><select value={reviewForm.rating} onChange={e => setReviewForm(f => ({ ...f, rating: e.target.value }))}>{[5,4,3,2,1].map(n => <option key={n} value={n}>{n} / 5</option>)}</select></label><label><span>Category</span><select value={reviewForm.category} onChange={e => setReviewForm(f => ({ ...f, category: e.target.value }))}><option value="review">Review</option><option value="compliment">Compliment</option><option value="complaint">Complaint</option></select></label><label><span>Your comments</span><textarea required rows="4" value={reviewForm.comments} onChange={e => setReviewForm(f => ({ ...f, comments: e.target.value }))} placeholder="Tell us about your experience"/></label><button className="auth-submit" type="submit" disabled={loading}>Submit review <ArrowRight size={16}/></button></form></section><section className="portal-panel"><div className="portal-panel-head"><div><h2>My submitted reviews</h2><p>Review records stored in MongoDB.</p></div><span className="portal-count">{reviews.length}</span></div>{reviews.length ? <div className="review-list">{reviews.map((r, i) => <article className="review-card" key={String(r._id || i)}><div className="review-stars">{'★'.repeat(Number(r.rating) || 0)}{'☆'.repeat(Math.max(0,5-(Number(r.rating)||0)))}</div><p>{r.comments}</p><small>Trip {r.tripId} · {String(r.category || 'review').toUpperCase()}</small></article>)}</div> : <div className="portal-empty">You haven't submitted any reviews yet.</div>}</section></div>}
      </section>
    </main>
    {showProfile && <ProfilePanel onClose={() => setShowProfile(false)} onUserUpdated={onUserUpdated}/>}
  </div>;
}

function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? String(value).slice(0, 10) : d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}
function money(value) { return Number(value || 0).toLocaleString(undefined, { maximumFractionDigits: 2 }); }

export default App;
