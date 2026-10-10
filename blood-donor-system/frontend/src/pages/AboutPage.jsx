import React from 'react';
import { 
  Building2, 
  Heart, 
  ShieldCheck, 
  Lock, 
  MapPin, 
  Activity, 
  AlertTriangle,
  Users,
  Compass,
  CheckCircle2,
  ArrowRight
} from 'lucide-react';
import { Link } from 'react-router-dom';

export function AboutPage() {
  return (
    <div className="bg-slate-50 min-h-screen py-10 font-sans">
      <div className="max-w-5xl mx-auto px-4 sm:px-6">
        
        {/* Page Header */}
        <div className="text-center mb-12">
          <div className="inline-flex items-center gap-2 bg-red-50 text-red-700 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider mb-3 border border-red-200">
            <Activity size={14} className="text-red-600" />
            Platform Purpose & Architecture
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
            About LifeLink Emergency Response
          </h1>
          <p className="text-slate-600 max-w-2xl mx-auto mt-3 text-base">
            Bridging the communication gap between acute hospital blood shortages and willing community donors through algorithmic coordination.
          </p>
        </div>

        {/* Core Mission Card */}
        <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 mb-10 shadow-sm">
          <h2 className="text-xl font-bold text-slate-900 mb-3">The Problem LifeLink Solves</h2>
          <p className="text-sm text-slate-600 leading-relaxed mb-4">
            During acute medical emergencies—such as postpartum hemorrhage, multi-vehicle trauma accidents, cardiovascular surgery complications, or sudden hematological crises—hospital emergency departments can exhaust their on-shelf type-specific blood inventories in minutes.
          </p>
          <p className="text-sm text-slate-600 leading-relaxed mb-6">
            Historically, blood bank technicians spent precious hours manually calling donor lists or sending broadcast messages into unverified groups. LifeLink solves this communication bottleneck by providing an automated, real-time coordination hub that calculates blood-type compatibility and 20 km proximity to alert verified donors in seconds.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-4 border-t border-slate-100">
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <span className="font-bold text-slate-900 text-sm block mb-1">Targeted Dispatch</span>
              <p className="text-xs text-slate-500">Only compatible donor blood groups receive emergency alerts for a broadcast request.</p>
            </div>
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <span className="font-bold text-slate-900 text-sm block mb-1">Proximity Prioritization</span>
              <p className="text-xs text-slate-500">Haversine GPS calculation ensures alerted donors are within an accessible 20 km radius.</p>
            </div>
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
              <span className="font-bold text-slate-900 text-sm block mb-1">Direct Operational Link</span>
              <p className="text-xs text-slate-500">Donors confirm availability directly into the hospital dashboard with real-time WebSocket sync.</p>
            </div>
          </div>
        </div>

        {/* Who Uses LifeLink Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
          <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-7 shadow-sm">
            <div className="w-12 h-12 bg-red-100 text-red-600 rounded-xl flex items-center justify-center mb-4">
              <Building2 size={24} />
            </div>
            <h3 className="text-lg font-bold text-slate-900 mb-2">Hospital Emergency Teams</h3>
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed mb-4">
              Authorized clinical personnel register institution licenses and broadcast emergency blood requests specifying blood group, units required, and urgency tiers. Hospitals review matches and verify completion.
            </p>
            <ul className="text-xs text-slate-600 space-y-2">
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-emerald-600" /> Instant request broadcast</li>
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-emerald-600" /> Algorithmic donor matching engine</li>
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-emerald-600" /> Real-time donor acceptance tracking</li>
            </ul>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-7 shadow-sm">
            <div className="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-xl flex items-center justify-center mb-4">
              <Heart size={24} />
            </div>
            <h3 className="text-lg font-bold text-slate-900 mb-2">Volunteer Community Donors</h3>
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed mb-4">
              Individuals maintain their verified blood group, contact details, and current availability toggle. When a hospital broadcasts a nearby emergency matching their blood type, donors receive immediate notifications.
            </p>
            <ul className="text-xs text-slate-600 space-y-2">
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-emerald-600" /> One-click availability toggle</li>
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-emerald-600" /> WebSockets emergency push notifications</li>
              <li className="flex items-center gap-2"><CheckCircle2 size={14} className="text-emerald-600" /> Accept or decline requests with total autonomy</li>
            </ul>
          </div>
        </div>

        {/* Platform Transparency & Clinical Limitations */}
        <div className="bg-white rounded-2xl border border-slate-200 p-6 sm:p-8 mb-12 shadow-sm">
          <div className="flex items-center gap-3 pb-4 border-b border-slate-100 mb-5">
            <AlertTriangle size={24} className="text-amber-600" />
            <div>
              <h3 className="text-lg font-bold text-slate-900">Platform Scope & Clinical Limitations</h3>
              <p className="text-xs text-slate-500">Transparency on software coordination versus medical care</p>
            </div>
          </div>

          <div className="space-y-4 text-xs sm:text-sm text-slate-600 leading-relaxed">
            <p>
              <strong>1. Coordination, Not Clinical Diagnosis:</strong> LifeLink is a software coordination tool. It does not replace established blood transfusion services, central blood banking reserves, or national emergency dispatch systems.
            </p>
            <p>
              <strong>2. Laboratory Testing Remains Mandatory:</strong> Algorithmic matching verifies ABO/Rh compatibility on paper, but hospital blood bank staff must always perform laboratory crossmatching, viral screening, and physical donor evaluation before blood is drawn or transfused.
            </p>
            <p>
              <strong>3. Ethical Volunteerism:</strong> LifeLink promotes 100% voluntary, non-remunerated blood donation in accordance with World Health Organization (WHO) ethical guidelines.
            </p>
          </div>
        </div>

        {/* Call to Action */}
        <div className="bg-slate-900 rounded-2xl p-8 text-center text-white shadow-xl">
          <h3 className="text-2xl font-bold mb-2">Be Part of the Lifesaving Pipeline</h3>
          <p className="text-slate-300 text-sm max-w-lg mx-auto mb-6">
            Join the network as a verified medical facility or a registered voluntary donor today.
          </p>
          <div className="flex flex-wrap justify-center gap-4">
            <Link
              to="/register"
              className="px-6 py-3 bg-red-600 hover:bg-red-700 text-white rounded-xl text-sm font-semibold shadow-md transition flex items-center gap-2"
            >
              Register on LifeLink <ArrowRight size={16} />
            </Link>
            <Link
              to="/donation-guide"
              className="px-6 py-3 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-xl text-sm font-semibold transition"
            >
              Review Donation Guidelines
            </Link>
          </div>
        </div>

      </div>
    </div>
  );
}
