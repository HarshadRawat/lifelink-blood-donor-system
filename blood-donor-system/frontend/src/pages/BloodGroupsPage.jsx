import React, { useState } from 'react';
import { 
  Droplet, 
  CheckCircle2, 
  XCircle, 
  ShieldAlert, 
  HelpCircle, 
  RefreshCw, 
  Award, 
  Info,
  ArrowRight,
  Sparkles,
  BookOpen
} from 'lucide-react';
import { Link } from 'react-router-dom';

export function BloodGroupsPage() {
  const [selectedGroup, setSelectedGroup] = useState('O-');

  // Red Blood Cell Compatibility Data based on medical standards
  const bloodData = {
    'O-': {
      label: 'O Negative',
      role: 'Universal Red Blood Cell Donor',
      antigens: 'None (neither A, B, nor Rh antigens)',
      antibodies: 'Anti-A, Anti-B',
      giveTo: ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'],
      receiveFrom: ['O-'],
      summary: 'Because O− red blood cells have no A, B, or Rh antigens, they can be given to virtually any recipient in acute trauma situations when the patient’s blood type is unknown.',
      plasmaNote: 'While O− is the universal red blood cell donor, it can only receive O− red blood cells because its plasma contains antibodies against all other types.'
    },
    'O+': {
      label: 'O Positive',
      role: 'Most Common Blood Type',
      antigens: 'Rh antigen present (no A or B antigens)',
      antibodies: 'Anti-A, Anti-B',
      giveTo: ['O+', 'A+', 'B+', 'AB+'],
      receiveFrom: ['O-', 'O+'],
      summary: 'O+ is one of the most frequently needed blood types. It can safely donate red blood cells to any Rh-positive recipient (O+, A+, B+, AB+).',
      plasmaNote: 'O+ patients can receive red blood cells from O− and O+ donors.'
    },
    'A-': {
      label: 'A Negative',
      role: 'Rare & Critical Donor',
      antigens: 'A antigen only (no Rh antigen)',
      antibodies: 'Anti-B',
      giveTo: ['A-', 'A+', 'AB-', 'AB+'],
      receiveFrom: ['O-', 'A-'],
      summary: 'A− individuals carry the A antigen on their red blood cells but lack the Rh protein. They can donate to both A and AB patients regardless of Rh status.',
      plasmaNote: 'Can receive red blood cells from O− and A− donors.'
    },
    'A+': {
      label: 'A Positive',
      role: 'Common Recipient & Donor',
      antigens: 'A and Rh antigens',
      antibodies: 'Anti-B',
      giveTo: ['A+', 'AB+'],
      receiveFrom: ['O-', 'O+', 'A-', 'A+'],
      summary: 'A+ is the second most common blood type. A+ patients have versatile receiving capability, accepting red blood cells from four blood groups.',
      plasmaNote: 'Can safely receive red blood cells from O−, O+, A−, and A+ donors.'
    },
    'B-': {
      label: 'B Negative',
      role: 'Rare Donor Type',
      antigens: 'B antigen only (no Rh antigen)',
      antibodies: 'Anti-A',
      giveTo: ['B-', 'B+', 'AB-', 'AB+'],
      receiveFrom: ['O-', 'B-'],
      summary: 'Found in only a small percentage of the population. Can donate red blood cells to B and AB recipients.',
      plasmaNote: 'Can receive red blood cells from O− and B− donors.'
    },
    'B+': {
      label: 'B Positive',
      role: 'Important Community Need',
      antigens: 'B and Rh antigens',
      antibodies: 'Anti-A',
      giveTo: ['B+', 'AB+'],
      receiveFrom: ['O-', 'O+', 'B-', 'B+'],
      summary: 'B+ donors can donate red blood cells to B+ and AB+ individuals, and can receive from four compatible donor groups.',
      plasmaNote: 'Can receive red blood cells from O−, O+, B−, and B+ donors.'
    },
    'AB-': {
      label: 'AB Negative',
      role: 'Rarest Blood Group',
      antigens: 'A and B antigens (no Rh antigen)',
      antibodies: 'None against A or B',
      giveTo: ['AB-', 'AB+'],
      receiveFrom: ['O-', 'A-', 'B-', 'AB-'],
      summary: 'Representing less than 1% of donors globally. Can donate red blood cells to AB− and AB+ recipients, and can receive from any Rh-negative donor.',
      plasmaNote: 'AB− individuals are universal plasma donors for Rh-negative recipients.'
    },
    'AB+': {
      label: 'AB Positive',
      role: 'Universal Red Blood Cell Recipient',
      antigens: 'A, B, and Rh antigens present',
      antibodies: 'No anti-A, anti-B, or anti-Rh antibodies',
      giveTo: ['AB+'],
      receiveFrom: ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'],
      summary: 'AB+ individuals can receive red blood cells from all eight common blood types because their immune system recognizes A, B, and Rh proteins as self-antigens.',
      plasmaNote: 'While AB+ can receive red blood cells from anyone, AB+ can only donate red blood cells to fellow AB+ recipients.'
    }
  };

  const allGroups = ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'];

  // Educational Quiz State
  const quizQuestions = [
    {
      question: 'Which blood group is universally used for emergency red blood cell transfusions when typing is pending?',
      options: ['O Negative (O−)', 'O Positive (O+)', 'AB Positive (AB+)', 'A Negative (A−)'],
      correctIndex: 0,
      explanation: 'O− red blood cells lack A, B, and Rh antigens, making them the safest choice for acute trauma resuscitation before antibody screening is completed.'
    },
    {
      question: 'What does the "+" or "−" designation in blood groups represent?',
      options: [
        'Hemoglobin concentration',
        'Presence or absence of the Rh (Rhesus) protein antigen',
        'Platelet count level',
        'Blood pressure rating'
      ],
      correctIndex: 1,
      explanation: 'The Rh factor is an inherited protein on red blood cells. If you have the Rh protein, you are Rh-positive (+); if not, you are Rh-negative (−).'
    },
    {
      question: 'Why is AB+ known as the universal red blood cell recipient?',
      options: [
        'Its red blood cells have no antigens whatsoever',
        'It has extra high platelet counts',
        'Its immune system produces no anti-A, anti-B, or anti-Rh antibodies',
        'It cannot clot during transfusions'
      ],
      correctIndex: 2,
      explanation: 'Because AB+ individuals carry A, B, and Rh antigens naturally, their plasma does not produce antibodies against any donor ABO/Rh antigens.'
    },
    {
      question: 'Is plasma compatibility identical to red blood cell compatibility?',
      options: [
        'Yes, they are completely interchangeable',
        'No, plasma compatibility is reversed because plasma contains antibodies rather than cell antigens',
        'Only for Rh-positive individuals',
        'Plasma does not require compatibility testing'
      ],
      correctIndex: 1,
      explanation: 'Plasma compatibility is reversed! AB is the universal plasma donor (has no antibodies), while O is the universal plasma recipient.'
    },
    {
      question: 'Why is clinical crossmatching essential even when blood groups match on paper?',
      options: [
        'To verify donor age and weight',
        'To detect unexpected atypical antibodies that could trigger a hemolytic reaction',
        'To determine if the patient has consumed water',
        'To ensure the hospital billing is verified'
      ],
      correctIndex: 1,
      explanation: 'Beyond the ABO and Rh systems, there are hundreds of minor blood group antigens (Kell, Duffy, Kidd). Laboratory crossmatching tests patient serum directly against donor cells to ensure true compatibility.'
    },
    {
      question: 'Can an O+ donor safely donate red blood cells to an O− recipient?',
      options: [
        'Yes, because both are O group',
        'No, because the O− recipient’s immune system will develop antibodies against the donor’s Rh antigen',
        'Only if the recipient is male',
        'Yes, if units are given slowly'
      ],
      correctIndex: 1,
      explanation: 'An O− recipient lacks the Rh antigen. Exposing an O− patient to Rh-positive blood can cause sensitization and severe hemolytic transfusion reactions.'
    }
  ];

  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState(null);
  const [showExplanation, setShowExplanation] = useState(false);
  const [score, setScore] = useState(0);
  const [quizFinished, setQuizFinished] = useState(false);

  const handleSelectOption = (index) => {
    if (showExplanation) return;
    setSelectedAnswer(index);
    setShowExplanation(true);
    if (index === quizQuestions[currentQuestion].correctIndex) {
      setScore((prev) => prev + 1);
    }
  };

  const handleNextQuestion = () => {
    if (currentQuestion < quizQuestions.length - 1) {
      setCurrentQuestion((prev) => prev + 1);
      setSelectedAnswer(null);
      setShowExplanation(false);
    } else {
      setQuizFinished(true);
    }
  };

  const handleRestartQuiz = () => {
    setCurrentQuestion(0);
    setSelectedAnswer(null);
    setShowExplanation(false);
    setScore(0);
    setQuizFinished(false);
  };

  const currentData = bloodData[selectedGroup];

  return (
    <div className="bg-slate-50 min-h-screen py-10 font-sans">
      <div className="max-w-6xl mx-auto px-4 sm:px-6">
        
        {/* Page Header */}
        <div className="text-center mb-12">
          <div className="inline-flex items-center gap-2 bg-red-50 text-red-700 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider mb-3 border border-red-200">
            <Droplet size={14} className="fill-red-600 text-red-600" />
            Medical Education Center
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
            Interactive Blood Group Explorer
          </h1>
          <p className="text-slate-600 max-w-2xl mx-auto mt-3 text-base">
            Explore the biological mechanisms of ABO antigens, Rh factors, and red blood cell transfusion compatibility rules.
          </p>
        </div>

        {/* Medical Disclaimer Banner */}
        <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4 sm:p-5 mb-10 flex items-start gap-3.5 shadow-sm">
          <ShieldAlert size={22} className="text-amber-600 shrink-0 mt-0.5" />
          <div className="text-xs sm:text-sm text-amber-900 leading-relaxed">
            <strong className="font-bold">Medical Education Notice:</strong> Compatibility matrices presented here outline general red blood cell (RBC) relationships. Actual patient transfusion safety strictly mandates formal ABO/Rh confirmatory typing, atypical antibody screening, and laboratory crossmatching conducted by certified clinical pathologists.
          </div>
        </div>

        {/* 8 Blood Groups Selector Grid */}
        <div className="mb-8">
          <h2 className="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4 text-center">
            Select a Blood Group to Inspect Compatibility
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3">
            {allGroups.map((group) => {
              const isSelected = selectedGroup === group;
              return (
                <button
                  key={group}
                  onClick={() => setSelectedGroup(group)}
                  className={`p-3.5 rounded-xl border text-center transition-all flex flex-col items-center justify-center cursor-pointer ${
                    isSelected
                      ? 'bg-red-600 text-white border-red-600 shadow-md transform -translate-y-0.5'
                      : 'bg-white text-slate-800 border-slate-200 hover:border-red-300 hover:bg-red-50/50 shadow-sm'
                  }`}
                  aria-pressed={isSelected}
                >
                  <span className="text-2xl font-black font-mono">{group}</span>
                  <span className={`text-[10px] mt-1 font-semibold ${isSelected ? 'text-red-100' : 'text-slate-500'}`}>
                    {group === 'O-' ? 'Universal' : group === 'AB+' ? 'Recipient' : 'ABO/Rh'}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Selected Blood Group Deep Dive Card */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8 mb-12">
          <div className="flex flex-col md:flex-row md:items-center justify-between pb-6 border-b border-slate-100 gap-4">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 bg-red-600 text-white rounded-2xl flex items-center justify-center font-mono font-black text-3xl shadow-md shadow-red-200">
                {selectedGroup}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-2xl font-bold text-slate-900">{currentData.label}</h3>
                  <span className="bg-red-50 text-red-700 text-xs font-semibold px-2.5 py-0.5 rounded-full border border-red-200">
                    {currentData.role}
                  </span>
                </div>
                <p className="text-slate-500 text-sm mt-1">{currentData.summary}</p>
              </div>
            </div>

            <div className="flex items-center gap-3 bg-slate-50 p-3 rounded-xl border border-slate-200 text-xs">
              <div>
                <span className="text-slate-500 block">Antigens on RBC:</span>
                <strong className="text-slate-800 font-medium">{currentData.antigens}</strong>
              </div>
              <div className="w-px h-8 bg-slate-200" />
              <div>
                <span className="text-slate-500 block">Plasma Antibodies:</span>
                <strong className="text-slate-800 font-medium">{currentData.antibodies}</strong>
              </div>
            </div>
          </div>

          {/* Directional Flow: Can Give To vs. Can Receive From */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
            
            {/* Can Donate Red Blood Cells To */}
            <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-5">
              <div className="flex items-center justify-between mb-3">
                <h4 className="text-sm font-bold text-emerald-950 flex items-center gap-2">
                  <CheckCircle2 size={18} className="text-emerald-600" />
                  Can Donate Red Blood Cells To:
                </h4>
                <span className="text-xs bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-md font-semibold">
                  {currentData.giveTo.length} Recipient Groups
                </span>
              </div>
              
              <div className="flex flex-wrap gap-2 pt-2">
                {allGroups.map((g) => {
                  const isCompatible = currentData.giveTo.includes(g);
                  return (
                    <span
                      key={g}
                      className={`px-3 py-1.5 rounded-lg text-xs font-mono font-bold flex items-center gap-1.5 ${
                        isCompatible
                          ? 'bg-emerald-600 text-white shadow-sm'
                          : 'bg-slate-100 text-slate-400 opacity-40'
                      }`}
                    >
                      {isCompatible ? <CheckCircle2 size={13} /> : <XCircle size={13} />}
                      {g}
                    </span>
                  );
                })}
              </div>
              <p className="text-xs text-emerald-800 mt-4 leading-relaxed">
                When an emergency request is broadcast for any of these compatible recipient types, {selectedGroup} donors are prioritized by the matching engine.
              </p>
            </div>

            {/* Can Receive Red Blood Cells From */}
            <div className="bg-blue-50/50 border border-blue-100 rounded-xl p-5">
              <div className="flex items-center justify-between mb-3">
                <h4 className="text-sm font-bold text-blue-950 flex items-center gap-2">
                  <Droplet size={18} className="text-blue-600" />
                  Can Receive Red Blood Cells From:
                </h4>
                <span className="text-xs bg-blue-100 text-blue-800 px-2 py-0.5 rounded-md font-semibold">
                  {currentData.receiveFrom.length} Donor Groups
                </span>
              </div>

              <div className="flex flex-wrap gap-2 pt-2">
                {allGroups.map((g) => {
                  const isCompatible = currentData.receiveFrom.includes(g);
                  return (
                    <span
                      key={g}
                      className={`px-3 py-1.5 rounded-lg text-xs font-mono font-bold flex items-center gap-1.5 ${
                        isCompatible
                          ? 'bg-blue-600 text-white shadow-sm'
                          : 'bg-slate-100 text-slate-400 opacity-40'
                      }`}
                    >
                      {isCompatible ? <CheckCircle2 size={13} /> : <XCircle size={13} />}
                      {g}
                    </span>
                  );
                })}
              </div>
              <p className="text-xs text-blue-800 mt-4 leading-relaxed">
                {currentData.plasmaNote}
              </p>
            </div>
          </div>
        </div>

        {/* Master Red Blood Cell Compatibility Table */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8 mb-12">
          <div className="mb-6">
            <h3 className="text-xl font-bold text-slate-900">Comprehensive Red Blood Cell Compatibility Matrix</h3>
            <p className="text-slate-500 text-xs sm:text-sm mt-1">
              Cross-reference any recipient (rows) against available donor blood types (columns).
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-center text-xs border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200">
                  <th className="p-3 text-left font-bold text-slate-700">Patient / Recipient</th>
                  {allGroups.map((donor) => (
                    <th key={donor} className="p-3 font-mono font-bold text-slate-800 border-l border-slate-100">
                      {donor} Donor
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {allGroups.map((recip) => (
                  <tr key={recip} className={`border-b border-slate-100 hover:bg-slate-50/80 ${selectedGroup === recip ? 'bg-red-50/40 font-semibold' : ''}`}>
                    <td className="p-3 text-left font-mono font-bold text-slate-900 flex items-center gap-2">
                      <span className="w-6 h-6 rounded-md bg-slate-200 text-slate-700 flex items-center justify-center text-[10px]">
                        {recip}
                      </span>
                      <span>Recipient</span>
                    </td>
                    {allGroups.map((donor) => {
                      const isMatch = bloodData[donor].giveTo.includes(recip);
                      return (
                        <td key={donor} className="p-3 border-l border-slate-100">
                          {isMatch ? (
                            <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-emerald-100 text-emerald-700 font-bold">
                              ✓
                            </span>
                          ) : (
                            <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-slate-100 text-slate-300">
                              ✕
                            </span>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 pt-4 border-t border-slate-100 flex flex-wrap items-center justify-between gap-4 text-xs text-slate-500">
            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1.5"><strong className="text-emerald-700">✓</strong> Compatible Red Blood Cells</span>
              <span className="flex items-center gap-1.5"><strong className="text-slate-400">✕</strong> Incompatible (Risk of Hemolysis)</span>
            </div>
            <span>Table strictly applies to packed Red Blood Cells (RBCs)</span>
          </div>
        </div>

        {/* Blood Group Knowledge Quiz Section */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8 mb-12">
          <div className="flex items-center justify-between pb-6 border-b border-slate-100 mb-6">
            <div>
              <div className="inline-flex items-center gap-1.5 text-xs font-bold text-red-600 uppercase tracking-wider mb-1">
                <Sparkles size={14} /> Interactive Knowledge Challenge
              </div>
              <h3 className="text-2xl font-bold text-slate-900">Blood Group Knowledge Quiz</h3>
            </div>
            
            {!quizFinished && (
              <span className="text-xs font-semibold text-slate-500 bg-slate-100 px-3 py-1.5 rounded-full">
                Question {currentQuestion + 1} of {quizQuestions.length}
              </span>
            )}
          </div>

          {!quizFinished ? (
            <div>
              {/* Progress bar */}
              <div className="w-full bg-slate-100 h-2 rounded-full mb-6 overflow-hidden">
                <div 
                  className="bg-red-600 h-full transition-all duration-300"
                  style={{ width: `${((currentQuestion + 1) / quizQuestions.length) * 100}%` }}
                />
              </div>

              {/* Current Question */}
              <h4 className="text-lg font-bold text-slate-900 mb-6 leading-snug">
                {quizQuestions[currentQuestion].question}
              </h4>

              {/* Multiple Choice Options */}
              <div className="space-y-3 mb-6">
                {quizQuestions[currentQuestion].options.map((opt, idx) => {
                  const isCorrect = idx === quizQuestions[currentQuestion].correctIndex;
                  const isSelected = selectedAnswer === idx;

                  let optClass = 'bg-slate-50 border-slate-200 hover:border-slate-300 text-slate-800';
                  if (showExplanation) {
                    if (isCorrect) {
                      optClass = 'bg-emerald-50 border-emerald-500 text-emerald-900 font-semibold shadow-sm';
                    } else if (isSelected) {
                      optClass = 'bg-rose-50 border-rose-400 text-rose-900 line-through';
                    } else {
                      optClass = 'bg-slate-50 border-slate-200 text-slate-400 opacity-60';
                    }
                  }

                  return (
                    <button
                      key={idx}
                      disabled={showExplanation}
                      onClick={() => handleSelectOption(idx)}
                      className={`w-full p-4 rounded-xl border text-left text-sm transition-all flex items-center justify-between ${optClass}`}
                    >
                      <span>{opt}</span>
                      {showExplanation && isCorrect && (
                        <CheckCircle2 size={18} className="text-emerald-600 shrink-0" />
                      )}
                      {showExplanation && isSelected && !isCorrect && (
                        <XCircle size={18} className="text-rose-500 shrink-0" />
                      )}
                    </button>
                  );
                })}
              </div>

              {/* Explanation Callout */}
              {showExplanation && (
                <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 mb-6 text-sm text-slate-700 animate-fadeIn">
                  <div className="flex items-center gap-1.5 font-bold text-slate-900 mb-1">
                    <Info size={16} className="text-blue-600" />
                    Clinical Explanation:
                  </div>
                  <p className="text-xs sm:text-sm leading-relaxed text-slate-600">
                    {quizQuestions[currentQuestion].explanation}
                  </p>
                </div>
              )}

              {/* Next Button */}
              {showExplanation && (
                <div className="flex justify-end">
                  <button
                    onClick={handleNextQuestion}
                    className="px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white font-semibold text-sm rounded-xl flex items-center gap-2 shadow-sm transition"
                  >
                    {currentQuestion < quizQuestions.length - 1 ? 'Next Question' : 'Complete Quiz'}
                    <ArrowRight size={16} />
                  </button>
                </div>
              )}
            </div>
          ) : (
            /* Quiz Completed View */
            <div className="text-center py-8">
              <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-4">
                <Award size={36} />
              </div>
              <h4 className="text-2xl font-bold text-slate-900 mb-1">Knowledge Challenge Complete!</h4>
              <p className="text-slate-500 text-sm mb-6">
                You correctly answered <strong className="text-slate-900">{score}</strong> out of <strong className="text-slate-900">{quizQuestions.length}</strong> questions.
              </p>

              <div className="inline-flex gap-4">
                <button
                  onClick={handleRestartQuiz}
                  className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-sm font-semibold flex items-center gap-2 transition"
                >
                  <RefreshCw size={15} /> Retake Quiz
                </button>
                <Link
                  to="/donation-guide"
                  className="px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-sm font-semibold flex items-center gap-2 transition"
                >
                  <BookOpen size={15} /> Read Donation Guide
                </Link>
              </div>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
