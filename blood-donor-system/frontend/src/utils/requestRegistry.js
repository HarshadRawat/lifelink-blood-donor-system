// Utility to map emergency request IDs with demanding hospitals and request details
// Ensures donors and hospitals always see rich contextual details (hospital name, request ID, urgency)

const STORAGE_KEY = 'lifelink_request_hospital_registry';

// Default seeds based on active database hospital records
const DEFAULT_SEEDS = {
  29: { hospitalName: 'Lodu Hospital', city: 'bb', bloodGroup: 'O-', urgency: 'CRITICAL', unitsRequired: 1 },
  28: { hospitalName: 'Lodu Hospital', city: 'bb', bloodGroup: 'O-', urgency: 'CRITICAL', unitsRequired: 1 },
  27: { hospitalName: 'Lodu Hospital', city: 'bb', bloodGroup: 'O-', urgency: 'CRITICAL', unitsRequired: 2 },
  26: { hospitalName: 'Central Hospital Care', city: 'Gotham', bloodGroup: 'O-', urgency: 'HIGH', unitsRequired: 2 },
  25: { hospitalName: 'Lodu Hospital', city: 'bb', bloodGroup: 'A+', urgency: 'HIGH', unitsRequired: 1 },
};

export const getRegistry = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(DEFAULT_SEEDS));
      return DEFAULT_SEEDS;
    }
    const parsed = JSON.parse(raw);
    return { ...DEFAULT_SEEDS, ...parsed };
  } catch (e) {
    return DEFAULT_SEEDS;
  }
};

export const registerHospitalRequest = (request, hospitalProfile = null) => {
  if (!request || !request.id) return;
  try {
    const registry = getRegistry();
    const hospitalName = 
      request.hospitalName || 
      hospitalProfile?.hospitalName || 
      'City General Emergency Center';

    registry[request.id] = {
      id: request.id,
      hospitalName,
      city: hospitalProfile?.city || request.city || '',
      bloodGroup: request.bloodGroup || '',
      urgency: request.urgency || '',
      unitsRequired: request.unitsRequired || 1,
      createdAt: request.createdAt || new Date().toISOString()
    };

    localStorage.setItem(STORAGE_KEY, JSON.stringify(registry));
  } catch (e) {
    console.warn('Failed to update request registry', e);
  }
};

export const registerHospitalRequestsList = (requestsList, hospitalProfile = null) => {
  if (!Array.isArray(requestsList)) return;
  try {
    const registry = getRegistry();
    let updated = false;

    requestsList.forEach((req) => {
      if (req && req.id) {
        const hospitalName = 
          req.hospitalName || 
          hospitalProfile?.hospitalName || 
          registry[req.id]?.hospitalName || 
          'Emergency Care Center';

        registry[req.id] = {
          id: req.id,
          hospitalName,
          city: hospitalProfile?.city || req.city || registry[req.id]?.city || '',
          bloodGroup: req.bloodGroup || registry[req.id]?.bloodGroup || '',
          urgency: req.urgency || registry[req.id]?.urgency || '',
          unitsRequired: req.unitsRequired || registry[req.id]?.unitsRequired || 1,
          createdAt: req.createdAt || registry[req.id]?.createdAt || new Date().toISOString()
        };
        updated = true;
      }
    });

    if (updated) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(registry));
    }
  } catch (e) {
    console.warn('Failed to batch update request registry', e);
  }
};

export const getRequestDetails = (requestId) => {
  if (!requestId) return null;
  const registry = getRegistry();
  return registry[requestId] || {
    id: requestId,
    hospitalName: 'Emergency Hospital Network',
    city: 'Local Region',
    bloodGroup: '',
    urgency: '',
    unitsRequired: 1
  };
};
