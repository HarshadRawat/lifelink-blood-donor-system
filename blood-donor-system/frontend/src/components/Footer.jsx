import React from 'react';
import { HeartHandshake, ShieldCheck, PhoneCall, Mail, MapPin, AlertCircle, BookOpen, Droplet } from 'lucide-react';
import { Link } from 'react-router-dom';

export function Footer() {
  return (
    <footer className="footer bg-slate-900 text-slate-300 border-t border-slate-800">
      <div className="footer-container max-w-6xl mx-auto px-6 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand Col */}
          <div className="footer-brand-col space-y-4">
            <Link to="/" className="logo flex items-center gap-2">
              <span className="logo-icon-bg bg-red-600 text-white p-2 rounded-xl flex items-center justify-center">
                <HeartHandshake size={20} />
              </span>
              <span className="logo-text text-xl font-extrabold text-white">
                Life<span className="text-red-500">Link</span>
              </span>
            </Link>
            <p className="text-xs text-slate-400 leading-relaxed">
              Coordinating urgent blood requests between hospitals and potential donors with real-time geospatial matching.
            </p>
            <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-slate-800/80 border border-slate-700 rounded-lg text-xs text-emerald-400">
              <ShieldCheck size={14} />
              <span>Verified Hospital Coordination</span>
            </div>
          </div>

          {/* Platform Links */}
          <div className="footer-col">
            <h4 className="text-sm font-bold text-white uppercase tracking-wider mb-3">Platform</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <a href="/#how-it-works" className="hover:text-red-400 transition">How It Works</a>
              </li>
              <li>
                <Link to="/blood-groups" className="hover:text-red-400 transition">Blood Groups Explorer</Link>
              </li>
              <li>
                <Link to="/donation-guide" className="hover:text-red-400 transition">Donation Guide</Link>
              </li>
              <li>
                <Link to="/about" className="hover:text-red-400 transition">About LifeLink</Link>
              </li>
            </ul>
          </div>

          {/* Portals & Accounts */}
          <div className="footer-col">
            <h4 className="text-sm font-bold text-white uppercase tracking-wider mb-3">Participation</h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/register" state={{ defaultRole: 'DONOR' }} className="hover:text-red-400 transition">
                  Become a Voluntary Donor
                </Link>
              </li>
              <li>
                <Link to="/login" className="hover:text-red-400 transition">
                  Hospital Portal Access
                </Link>
              </li>
              <li>
                <Link to="/donation-guide" className="hover:text-red-400 transition">
                  Preparation & Safety Guidelines
                </Link>
              </li>
              <li>
                <Link to="/blood-groups" className="hover:text-red-400 transition">
                  Compatibility Quiz
                </Link>
              </li>
            </ul>
          </div>

          {/* Emergency & Support */}
          <div className="footer-col space-y-3">
            <h4 className="text-sm font-bold text-white uppercase tracking-wider mb-3">Coordination Network</h4>
            <div className="flex items-center gap-2 text-xs text-slate-300">
              <PhoneCall size={14} className="text-red-400 flex-shrink-0" />
              <span>Emergency Helpdesk: 24/7 Support</span>
            </div>
            <div className="flex items-center gap-2 text-xs text-slate-300">
              <Mail size={14} className="text-red-400 flex-shrink-0" />
              <span>coordination@lifelink.org</span>
            </div>
            <div className="flex items-center gap-2 text-xs text-slate-300">
              <MapPin size={14} className="text-red-400 flex-shrink-0" />
              <span>Geospatial 20 km Emergency Radius</span>
            </div>
          </div>
        </div>

        {/* Medical Education Disclaimer */}
        <div className="mt-10 pt-6 border-t border-slate-800">
          <div className="p-4 bg-slate-800/60 border border-slate-700/60 rounded-xl flex items-start gap-3">
            <AlertCircle size={18} className="text-amber-400 mt-0.5 flex-shrink-0" />
            <p className="text-xs text-slate-400 leading-relaxed">
              <strong className="text-slate-200">Clinical Disclaimer:</strong> Compatibility and guidance information provided on LifeLink is strictly educational and intended for emergency coordination support. Actual transfusion safety requires professional blood typing, antibody screening, crossmatching, and clinical judgment by qualified healthcare professionals in an accredited hospital laboratory.
            </p>
          </div>
        </div>

        <div className="mt-6 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-4">
          <p>© {new Date().getFullYear()} LifeLink Blood Donor Emergency Response System. All rights reserved.</p>
          <div className="flex gap-4">
            <Link to="/about" className="hover:text-slate-400">About System</Link>
            <span>•</span>
            <Link to="/donation-guide" className="hover:text-slate-400">Safety & Eligibility</Link>
            <span>•</span>
            <Link to="/blood-groups" className="hover:text-slate-400">RBC Compatibility</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
