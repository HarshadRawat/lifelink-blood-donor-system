import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  Heart, 
  Building2, 
  Activity, 
  Clock, 
  ShieldCheck, 
  ArrowRight, 
  CheckCircle2, 
  Droplet, 
  Users, 
  Zap,
  MapPin,
  BookOpen,
  HelpCircle,
  AlertTriangle,
  Stethoscope,
  Info,
  Layers,
  ChevronRight,
  ShieldAlert
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function LandingPage() {
  const { isAuthenticated, role } = useAuth();
  const navigate = useNavigate();
  const [selectedBlood, setSelectedBlood] = useState('O-');

  // Red Blood Cell compatibility relationships matching Section 5.B
  const compatibilityData = {
    'O-': { 
      canGiveTo: 'All eight common ABO/Rh types (Universal RBC Donor)', 
      canReceiveFrom: 'O- only',
      desc: 'O- red cells lack A, B, and Rh antigens, making them vital for uncrossmatched emergency transfusions.' 
    },
    'O+': { 
      canGiveTo: 'O+, A+, B+, AB+', 
      canReceiveFrom: 'O-, O+',
      desc: 'Most prevalent blood type. Widely used for emergency resuscitation when patient is Rh-positive.' 
    },
    'A-': { 
      canGiveTo: 'A-, A+, AB-, AB+', 
      canReceiveFrom: 'O-, A-',
      desc: 'Can donate red cells to both A and AB recipients of any Rh status.' 
    },
    'A+': { 
      canGiveTo: 'A+, AB+', 
      canReceiveFrom: 'O-, O+, A-, A+',
      desc: 'One of the most frequently requested blood groups for planned surgical procedures.' 
    },
    'B-': { 
      canGiveTo: 'B-, B+, AB-, AB+', 
      canReceiveFrom: 'O-, B-',
      desc: 'Relatively rare blood type; essential for matching Rh-negative patients with B antigens.' 
    },
    'B+': { 
      canGiveTo: 'B+, AB+', 
      canReceiveFrom: 'O-, O+, B-, B+',
      desc: 'Can receive red cells from both B and O types regardless of Rh positivity.' 
    },
    'AB-': { 
      canGiveTo: 'AB-, AB+', 
      canReceiveFrom: 'O-, A-, B-, AB-',
      desc: 'Can receive red blood cells from any Rh-negative donor group.' 
    },
    'AB+': { 
      canGiveTo: 'AB+ only', 
      canReceiveFrom: 'All eight common ABO/Rh types (Universal RBC Recipient)',
      desc: 'Can safely receive red blood cells from any common ABO/Rh donor type.' 
    },
  };

  const handleHospitalAction = () => {
    if (isAuthenticated) {
      navigate('/hospital/dashboard');
    } else {
      navigate('/login');
    }
  };

  const handleDonorAction = () => {
    if (isAuthenticated) {
      navigate(role === 'DONOR' ? '/donor/dashboard' : '/hospital/dashboard');
    } else {
      navigate('/register', { state: { defaultRole: 'DONOR' } });
    }
  };

  return (
    <div className="landing-page font-sans">
      {/* 4.B HERO SECTION */}
      <section className="hero-section">
        <div className="hero-grid">
          <div className="hero-text-col">
            <div className="hero-badge">
              <Activity className="text-red-600" size={16} />
              <span>COORDINATING URGENT TRANSFUSION NEEDS</span>
            </div>

            <h1 className="hero-title">
              Every drop can make a difference. <span className="highlight">Every connection matters.</span>
            </h1>

            <p className="hero-subtitle">
              LifeLink helps hospitals coordinate urgent blood requests with potential donors while making blood donation knowledge easier to understand and access.
            </p>

            {/* 4 Action Buttons required by Master Prompt Section 4.B */}
            <div className="hero-cta-group">
              <div className="hero-cta-primary-row">
                <Link to="/blood-groups" className="btn-secondary-hero">
                  <Droplet size={18} className="text-red-600" />
                  Explore Blood Groups
                </Link>
                <Link to="/donation-guide" className="btn-secondary-hero">
                  <BookOpen size={18} className="text-slate-700" />
                  Learn About Donation
                </Link>
              </div>

              <div className="hero-cta-auth-row">
                <button onClick={handleHospitalAction} className="btn-primary-hero">
                  <Building2 size={18} />
                  Hospital Login <ArrowRight size={16} />
                </button>
                <button onClick={handleDonorAction} className="btn-outline-hero">
                  <Heart size={18} className="text-red-600" />
                  Become a Donor
                </button>
              </div>
            </div>

            <div className="hero-trust-indicators">
              <div className="trust-item">
                <ShieldCheck size={18} className="text-emerald-600" />
                <span>Verified Hospital Accounts</span>
              </div>
              <div className="trust-item">
                <Clock size={18} className="text-amber-600" />
                <span>20 km Algorithmic Matching</span>
              </div>
            </div>
          </div>

          <div className="hero-graphic-col">
            <div className="hero-card-stack">
              <div className="glass-card main-stats-card">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="icon-pulse-red">
                      <Droplet size={24} className="text-red-600 fill-red-600" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-slate-900">Emergency Coordination Active</h4>
                      <p className="text-xs text-slate-500">Matching nearby eligible donors</p>
                    </div>
                  </div>
                  <span className="live-status-pill">ACTIVE</span>
                </div>

                <div className="urgency-preview-box">
                  <div className="flex justify-between items-center mb-2">
                    <span className="badge badge-critical">CRITICAL URGENCY</span>
                    <span className="text-xs font-mono text-slate-500">2 Units Requested</span>
                  </div>
                  <div className="font-semibold text-slate-800 text-sm">O- Red Blood Cells Needed</div>
                  <div className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                    <MapPin size={12} /> City General Trauma Wing • 20 km search radius
                  </div>
                </div>
              </div>

              <div className="glass-card floating-donor-card">
                <div className="flex items-center gap-3">
                  <div className="avatar-online">
                    <Heart size={16} className="text-white fill-white" />
                  </div>
                  <div>
                    <div className="text-sm font-semibold text-slate-800">Potential Donor Identified</div>
                    <div className="text-xs text-emerald-600 font-medium">Compatible O- Type within 3.2 km</div>
                  </div>
                </div>
              </div>

              <div className="mt-3 text-center">
                <span className="text-xs text-slate-400 italic">
                  * LifeLink facilitates communication; clinical compatibility testing is performed at the hospital.
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 4.C THE ACTUAL PROBLEM LIFELINK ADDRESSES */}
      <section className="section-padded bg-white border-y border-slate-200">
        <div className="max-w-6xl mx-auto">
          <div className="section-header text-center mb-12">
            <span className="sub-title">REAL-WORLD COORDINATION</span>
            <h2 className="main-title">The Coordination Challenge in Emergency Transfusions</h2>
            <p className="section-desc max-w-3xl mx-auto">
              In trauma cases and urgent surgeries, blood shortages are compounded by fragmented communication. Finding willing donors quickly requires structured, transparent coordination.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="feature-block-card">
              <div className="feature-icon bg-red-50 text-red-600">
                <Clock size={24} />
              </div>
              <h3 className="font-bold text-lg text-slate-900 mb-2">Manual Communication Delays</h3>
              <p className="text-slate-600 text-sm leading-relaxed">
                Traditional emergency sourcing often relies on informal phone trees and broadcast messages, reaching donors who may be miles away or biologically incompatible.
              </p>
            </div>

            <div className="feature-block-card">
              <div className="feature-icon bg-amber-50 text-amber-600">
                <MapPin size={24} />
              </div>
              <h3 className="font-bold text-lg text-slate-900 mb-2">Geographic Proximity</h3>
              <p className="text-slate-600 text-sm leading-relaxed">
                LifeLink applies a verified 20-kilometer radius algorithm, matching hospital requests only with potential donors capable of arriving in time for clinical screening.
              </p>
            </div>

            <div className="feature-block-card">
              <div className="feature-icon bg-blue-50 text-blue-600">
                <ShieldCheck size={24} />
              </div>
              <h3 className="font-bold text-lg text-slate-900 mb-2">Verified Oversight</h3>
              <p className="text-slate-600 text-sm leading-relaxed">
                Only authenticated hospital clinical coordinators can initiate emergency requests, ensuring requests are legitimate and preventing public misinformation.
              </p>
            </div>
          </div>

          {/* Boundaries & Limitations Disclaimer */}
          <div className="mt-8 p-4 bg-slate-50 border border-slate-200 rounded-xl flex items-start gap-3">
            <Info size={20} className="text-slate-600 mt-0.5 flex-shrink-0" />
            <p className="text-xs text-slate-600 leading-relaxed">
              <strong>Scope & Responsibility:</strong> LifeLink is a software coordination tool designed to assist hospitals in communicating urgent requirements with registered voluntary donors. LifeLink does not replace blood banks, emergency medical services, or established hospital transfusion services.
            </p>
          </div>
        </div>
      </section>

      {/* 4.D HOW LIFELINK WORKS (4-STEP WORKFLOW) */}
      <section className="section-padded bg-slate-50" id="how-it-works">
        <div className="max-w-6xl mx-auto">
          <div className="section-header text-center mb-14">
            <span className="sub-title">STEP-BY-STEP PROCESS</span>
            <h2 className="main-title">How LifeLink Coordinates an Emergency Request</h2>
            <p className="section-desc max-w-2xl mx-auto">
              A transparent, audit-ready workflow connecting authenticated hospitals with registered voluntary donors.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 relative">
            <div className="step-card">
              <div className="step-number">01</div>
              <div className="step-icon-wrapper red">
                <Building2 size={24} />
              </div>
              <h3>Hospital Request</h3>
              <p>An authorized hospital coordinator creates an emergency request specifying required ABO/Rh group, units, and urgency level.</p>
              <div className="step-tag bg-red-100 text-red-700">Authenticated</div>
            </div>

            <div className="step-card">
              <div className="step-number">02</div>
              <div className="step-icon-wrapper amber">
                <Zap size={24} />
              </div>
              <h3>Algorithmic Matching</h3>
              <p>The backend evaluates available registered donors within a 20 km radius matching strict red blood cell compatibility rules.</p>
              <div className="step-tag bg-amber-100 text-amber-700">20 km Radius</div>
            </div>

            <div className="step-card">
              <div className="step-number">03</div>
              <div className="step-icon-wrapper blue">
                <Users size={24} />
              </div>
              <h3>Potential Donor Review</h3>
              <p>Hospital reviews identified candidate donors. Matching identifies potential candidates—it does not imply accepted or completed donation.</p>
              <div className="step-tag bg-blue-100 text-blue-700">Candidate Match</div>
            </div>

            <div className="step-card">
              <div className="step-number">04</div>
              <div className="step-icon-wrapper green">
                <CheckCircle2 size={24} />
              </div>
              <h3>Coordination & Fulfillment</h3>
              <p>System sends real-time alerts. Hospital manages the request lifecycle from ACTIVE to FULFILLED as clinical steps complete.</p>
              <div className="step-tag bg-emerald-100 text-emerald-700">Status Lifecycle</div>
            </div>
          </div>
        </div>
      </section>

      {/* 5. INTERACTIVE BLOOD GROUP QUICK LOOK */}
      <section className="section-padded bg-white" id="compatibility">
        <div className="max-w-5xl mx-auto">
          <div className="section-header text-center mb-10">
            <span className="sub-title">EDUCATIONAL OVERVIEW</span>
            <h2 className="main-title">Blood Group Compatibility Quick Look</h2>
            <p className="section-desc">Select any of the 8 common ABO/Rh blood groups to see general red cell donation relationships.</p>
          </div>

          <div className="compatibility-matrix-container">
            <div className="blood-group-selector">
              {Object.keys(compatibilityData).map((bg) => (
                <button
                  key={bg}
                  onClick={() => setSelectedBlood(bg)}
                  className={`blood-type-btn ${selectedBlood === bg ? 'active' : ''}`}
                  aria-label={`Select blood group ${bg}`}
                >
                  {bg}
                </button>
              ))}
            </div>

            <div className="compatibility-info-card">
              <div className="blood-header">
                <span className="blood-large-badge">{selectedBlood}</span>
                <div>
                  <h3 className="text-xl font-bold text-slate-900">Blood Type {selectedBlood} Overview</h3>
                  <p className="text-slate-600 text-sm mt-1">{compatibilityData[selectedBlood].desc}</p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
                <div className="compat-box give">
                  <h4><CheckCircle2 size={18} className="text-emerald-600 inline mr-2" />Can Donate Red Cells To:</h4>
                  <p className="text-slate-900 font-semibold text-base mt-1">{compatibilityData[selectedBlood].canGiveTo}</p>
                </div>

                <div className="compat-box receive">
                  <h4><Droplet size={18} className="text-blue-600 inline mr-2" />Can Receive Red Cells From:</h4>
                  <p className="text-slate-900 font-semibold text-base mt-1">{compatibilityData[selectedBlood].canReceiveFrom}</p>
                </div>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="text-xs text-slate-500">
                  General red blood cell guide. Plasma/platelet rules differ. Clinical crossmatching required.
                </div>
                <Link to="/blood-groups" className="btn-learn-more-link inline-flex items-center gap-1.5 text-sm font-bold text-red-600 hover:text-red-700">
                  Open Interactive Explorer & Quiz <ChevronRight size={16} />
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 4.E RESPONSIBLE HEALTHCARE IMPACT (Qualitative, no fake numbers) */}
      <section className="section-padded bg-slate-50 border-t border-slate-200">
        <div className="max-w-6xl mx-auto">
          <div className="section-header text-center mb-12">
            <span className="sub-title">CLINICAL VALUE</span>
            <h2 className="main-title">Where Voluntary Blood Donation Makes an Impact</h2>
            <p className="section-desc max-w-2xl mx-auto">
              Blood cannot be synthetically manufactured. Timely voluntary donations are foundational across medical specialties.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            <div className="impact-card">
              <div className="impact-icon-circle bg-red-100 text-red-600">
                <Activity size={22} />
              </div>
              <h4 className="font-bold text-slate-900 mb-2">Emergency & Trauma</h4>
              <p className="text-xs text-slate-600 leading-relaxed">
                Massive blood transfusions are critical during vehicle collisions, severe accidents, and unexpected surgical hemorrhages.
              </p>
            </div>

            <div className="impact-card">
              <div className="impact-icon-circle bg-blue-100 text-blue-600">
                <Stethoscope size={22} />
              </div>
              <h4 className="font-bold text-slate-900 mb-2">Cancer Care</h4>
              <p className="text-xs text-slate-600 leading-relaxed">
                Chemotherapy and leukemia therapies suppress bone marrow, requiring regular red cell and platelet support throughout recovery.
              </p>
            </div>

            <div className="impact-card">
              <div className="impact-icon-circle bg-emerald-100 text-emerald-600">
                <Heart size={22} />
              </div>
              <h4 className="font-bold text-slate-900 mb-2">Obstetric Emergencies</h4>
              <p className="text-xs text-slate-600 leading-relaxed">
                Postpartum hemorrhage remains a leading maternal risk. Readily available blood units save new mothers during complex deliveries.
              </p>
            </div>

            <div className="impact-card">
              <div className="impact-icon-circle bg-amber-100 text-amber-600">
                <Droplet size={22} />
              </div>
              <h4 className="font-bold text-slate-900 mb-2">Chronic Blood Disorders</h4>
              <p className="text-xs text-slate-600 leading-relaxed">
                Patients with conditions like Thalassemia Major and Sickle Cell Disease rely on scheduled transfusions to sustain hemoglobin levels.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 4.F TRUST AND CLINICAL TRANSPARENCY */}
      <section className="section-padded bg-slate-900 text-white" id="trust-transparency">
        <div className="max-w-6xl mx-auto">
          <div className="section-header text-center mb-12">
            <span className="text-red-400 font-bold text-xs uppercase tracking-wider">CLINICAL GOVERNANCE</span>
            <h2 className="text-3xl md:text-4xl font-extrabold text-white mt-2 mb-3">
              Trust, Transparency & Safety Protocols
            </h2>
            <p className="text-slate-300 max-w-2xl mx-auto text-sm leading-relaxed">
              LifeLink bridges information gaps while respecting hospital clinical protocols, donor privacy, and mandatory transfusion safety standards.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-slate-800/80 border border-slate-700/80 p-6 rounded-2xl">
              <div className="flex items-center gap-3 mb-3">
                <Building2 size={20} className="text-red-400" />
                <h4 className="font-bold text-slate-100 text-base">Hospital Clinical Oversight</h4>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                Requests are strictly initiated by authenticated hospital personnel. Unverified individuals cannot trigger broadcast notifications or create false alerts.
              </p>
            </div>

            <div className="bg-slate-800/80 border border-slate-700/80 p-6 rounded-2xl">
              <div className="flex items-center gap-3 mb-3">
                <ShieldAlert size={20} className="text-amber-400" />
                <h4 className="font-bold text-slate-100 text-base">Mandatory Crossmatching</h4>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                Algorithm-matched donors are candidates only. Hospital transfusion laboratories perform full ABO/Rh retyping and antibody crossmatching prior to any administration.
              </p>
            </div>

            <div className="bg-slate-800/80 border border-slate-700/80 p-6 rounded-2xl">
              <div className="flex items-center gap-3 mb-3">
                <ShieldCheck size={20} className="text-emerald-400" />
                <h4 className="font-bold text-slate-100 text-base">Donor Privacy & Security</h4>
              </div>
              <p className="text-xs text-slate-300 leading-relaxed">
                Donor geographical coordinates are evaluated purely for radius matching. Full contact details are shared only in authorized emergency coordination contexts.
              </p>
            </div>
          </div>

          {/* Call to Action Banner */}
          <div className="mt-12 bg-gradient-to-r from-red-950/60 to-slate-800 border border-red-900/60 rounded-2xl p-8 flex flex-col md:flex-row items-center justify-between gap-6">
            <div>
              <h3 className="text-xl font-bold text-white mb-2">Ready to join the LifeLink emergency network?</h3>
              <p className="text-xs text-slate-300 max-w-xl">
                Whether you are a medical institution coordinating patient care or a healthy volunteer willing to donate, your participation strengthens local healthcare readiness.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link to="/register" state={{ defaultRole: 'DONOR' }} className="px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold text-sm rounded-xl transition shadow-lg shadow-red-900/50">
                Register as Donor
              </Link>
              <Link to="/login" className="px-5 py-2.5 bg-slate-700 hover:bg-slate-600 text-slate-200 font-bold text-sm rounded-xl transition">
                Hospital Portal
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
