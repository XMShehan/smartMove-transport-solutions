import { useEffect, useState } from 'react';
import { CheckCircle2, LockKeyhole, Save, UserRound, X } from 'lucide-react';
import { getJson, putJson } from './api.js';
import './profile.css';

export default function ProfilePanel({ onClose, onUserUpdated }) {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ name: '', nic: '', phone: '' });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    let active = true;
    getJson('/profile')
      .then(payload => {
        if (!active) return;
        const value = payload?.profile || payload;
        setProfile(value);
        setForm({ name: value?.name || '', nic: value?.nic || '', phone: value?.phone || '' });
      })
      .catch(err => { if (active) setError(err.message || 'Could not load your profile.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const update = (key, value) => setForm(current => ({ ...current, [key]: value }));

  async function saveProfile(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const result = await putJson('/profile', form);
      const value = result?.profile || result;
      setProfile(value);
      setForm({ name: value?.name || '', nic: value?.nic || '', phone: value?.phone || '' });
      setNotice(result?.message || 'Profile updated successfully.');
      onUserUpdated?.({ name: value?.name, email: value?.email, nic: value?.nic, phone: value?.phone });
    } catch (err) {
      setError(err.message || 'Could not update your profile.');
    } finally {
      setSaving(false);
    }
  }

  const role = String(profile?.role || '').toUpperCase();
  const accountId = role === 'MANAGER' ? profile?.managerId : profile?.passengerId;

  return (
    <div className="profile-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onClose?.(); }}>
      <section className="profile-dialog" role="dialog" aria-modal="true" aria-labelledby="profile-title">
        <header className="profile-dialog-header">
          <div className="profile-dialog-mark"><UserRound size={21} /></div>
          <div className="profile-dialog-title"><span>ACCOUNT SETTINGS</span><h2 id="profile-title">My profile</h2><p>View and update your personal details.</p></div>
          <button type="button" className="profile-close" onClick={onClose} aria-label="Close profile"><X size={19} /></button>
        </header>

        {error && <div className="profile-message error" role="alert">{error}</div>}
        {notice && <div className="profile-message success" role="status"><CheckCircle2 size={16} />{notice}</div>}

        {loading ? <div className="profile-loading">Loading your profile…</div> : profile && (
          <form className="profile-form" onSubmit={saveProfile}>
            <div className="profile-field-grid">
              <label className="profile-field"><span>Full name <b>*</b></span><input required maxLength="100" value={form.name} onChange={event => update('name', event.target.value)} autoComplete="name" /></label>
              <label className="profile-field"><span>NIC {role === 'MANAGER' && <b>*</b>}</span><input required={role === 'MANAGER'} maxLength="20" value={form.nic} onChange={event => update('nic', event.target.value)} autoComplete="off" /></label>
              <label className="profile-field"><span>Phone</span><input maxLength="25" value={form.phone} onChange={event => update('phone', event.target.value)} autoComplete="tel" /></label>
              <label className="profile-field"><span>Email address</span><input value={profile.email || ''} readOnly aria-readonly="true" /><small>Email is your sign-in identity and cannot be changed here.</small></label>
            </div>

            <div className="profile-account-summary">
              <div><span>Account type</span><strong>{role === 'MANAGER' ? 'Manager' : 'Passenger'}</strong></div>
              <div><span>Account ID</span><strong>{accountId ?? '—'}</strong></div>
              {role === 'PASSENGER' && profile.passengerType && <div><span>Passenger type</span><strong>{profile.passengerType}</strong></div>}
              {profile.registeredDate && <div><span>Registered</span><strong>{String(profile.registeredDate).slice(0, 10)}</strong></div>}
            </div>
            <div className="profile-security-note"><LockKeyhole size={16} /><span>Your profile is loaded using your signed-in session. You cannot view or change another user's profile here.</span></div>
            <footer className="profile-dialog-footer"><button type="button" className="btn secondary" onClick={onClose} disabled={saving}>Close</button><button type="submit" className="btn primary" disabled={saving}>{saving ? 'Saving…' : <><Save size={16} /> Save profile</>}</button></footer>
          </form>
        )}
      </section>
    </div>
  );
}
