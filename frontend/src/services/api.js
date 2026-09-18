import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL
    ? `${import.meta.env.VITE_API_URL}/api`
    : '/api'
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('scr_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('scr_token');
      localStorage.removeItem('scr_user');
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// ---- Auth ----
export const authApi = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data)
};

// ---- Students ----
export const studentApi = {
  getAll: () => api.get('/students'),
  getById: (id) => api.get(`/students/${id}`),
  getMe: () => api.get('/students/me'),
  updateMyProfile: (data) => api.put('/students/me/profile', data),
  create: (data) => api.post('/students', data),
  update: (id, data) => api.put(`/students/${id}`, data),
  delete: (id) => api.delete(`/students/${id}`)
};

// ---- Companies ----
export const companyApi = {
  getAll: () => api.get('/companies'),
  getById: (id) => api.get(`/companies/${id}`),
  create: (data) => api.post('/companies', data),
  update: (id, data) => api.put(`/companies/${id}`, data),
  delete: (id) => api.delete(`/companies/${id}`)
};

// ---- Recruitment Drives ----
export const driveApi = {
  getAll: () => api.get('/recruitments'),
  getById: (id) => api.get(`/recruitments/${id}`),
  create: (data) => api.post('/recruitments', data),
  update: (id, data) => api.put(`/recruitments/${id}`, data),
  delete: (id) => api.delete(`/recruitments/${id}`),
  addRound: (id, data) => api.post(`/recruitments/${id}/rounds`, data),
  getRounds: (id) => api.get(`/recruitments/${id}/rounds`),
  deleteRound: (roundId) => api.delete(`/recruitments/rounds/${roundId}`),
  eligibleStudents: (id) => api.get(`/recruitments/${id}/eligible-students`),
  ineligibleStudents: (id) => api.get(`/recruitments/${id}/ineligible-students`),
  checkEligibility: (id, studentId) => api.get(`/recruitments/${id}/eligibility/${studentId}`)
};

// ---- Applications ----
export const applicationApi = {
  apply: (studentId, driveId) => api.post('/applications', { studentId, driveId }),
  getByDrive: (driveId) => api.get('/applications', { params: { driveId } }),
  getByStudent: (studentId) => api.get('/applications', { params: { studentId } }),
  getById: (id) => api.get(`/applications/${id}`),
  updateStatus: (id, status) => api.put(`/applications/${id}/status`, { status }),
  shortlist: (id) => api.post(`/applications/${id}/shortlist`),
  shortlistAll: (driveId) => api.post(`/applications/shortlist-all/${driveId}`),
  reject: (id) => api.post(`/applications/${id}/reject`),
  // Enriched view of the logged-in student's own applications: company, role, current round,
  // status, and marks/feedback for every round completed so far — powers the student dashboard.
  mySummary: () => api.get('/applications/me/summary')
};

// ---- Admin: Reset any user's password ----
export const adminUserApi = {
  resetPassword: (userId, newPassword) => api.post(`/admin/users/${userId}/reset-password`, { newPassword })
};

// ---- Self-service: change my own password / email ----
export const changePassword = (currentPassword, newPassword) =>
  api.post('/auth/change-password', { currentPassword, newPassword });

export const updateEmail = (newEmail, currentPassword) =>
  api.post('/auth/update-email', { newEmail, currentPassword });

// ---- Academic Settings (departments & degrees master lists) ----
export const academicSettingsApi = {
  get: () => api.get('/academic-settings'),
  addDepartment: (name) => api.post('/admin/academic-settings/departments', { name }),
  removeDepartment: (name) => api.delete(`/admin/academic-settings/departments/${encodeURIComponent(name)}`),
  addDegree: (name) => api.post('/admin/academic-settings/degrees', { name }),
  removeDegree: (name) => api.delete(`/admin/academic-settings/degrees/${encodeURIComponent(name)}`)
};

// ---- Round marks & results (admin + lecturer; lecturers see only their own department) ----
export const roundResultApi = {
  candidates: (roundId) => api.get(`/rounds/${roundId}/candidates`),
  uploadMarks: (roundId, data) => api.post(`/rounds/${roundId}/marks`, data),
  results: (roundId) => api.get(`/rounds/${roundId}/results`),
  driveResults: (driveId) => api.get(`/rounds/drives/${driveId}/results`),
  allResults: () => api.get('/rounds/results')
};

// ---- Admin: Lecturer Approvals ----
export const lecturerApprovalApi = {
  getPending: () => api.get('/admin/lecturer-approvals'),
  approve: (userId) => api.post(`/admin/lecturer-approvals/${userId}/approve`),
  reject: (userId) => api.post(`/admin/lecturer-approvals/${userId}/reject`)
};

// ---- Interviewers ----
export const interviewerApi = {
  getAll: () => api.get('/interviewers'),
  getById: (id) => api.get(`/interviewers/${id}`),
  getMe: () => api.get('/interviewers/me'),
  create: (data) => api.post('/interviewers', data),
  update: (id, data) => api.put(`/interviewers/${id}`, data),
  delete: (id) => api.delete(`/interviewers/${id}`)
};

// ---- Panels ----
export const panelApi = {
  create: (data) => api.post('/panels', data),
  getByDrive: (driveId) => api.get('/panels', { params: { driveId } }),
  getByRound: (roundId) => api.get('/panels', { params: { roundId } }),
  getByInterviewer: (interviewerId) => api.get('/panels', { params: { interviewerId } }),
  getById: (id) => api.get(`/panels/${id}`),
  update: (id, data) => api.put(`/panels/${id}`, data),
  rename: (id, panelName) => api.put(`/panels/${id}/rename`, { panelName }),
  addInterviewer: (id, interviewerId) => api.post(`/panels/${id}/interviewers/${interviewerId}`),
  removeInterviewer: (id, interviewerId) => api.delete(`/panels/${id}/interviewers/${interviewerId}`),
  delete: (id) => api.delete(`/panels/${id}`)
};

// ---- Availability ----
export const availabilityApi = {
  submitStudent: (data) => api.post('/availability/student', data),
  submitInterviewer: (data) => api.post('/availability/interviewer', data),
  getStudent: (id) => api.get(`/availability/student/${id}`),
  getInterviewer: (id) => api.get(`/availability/interviewer/${id}`)
};

// ---- Scheduler ----
export const schedulerApi = {
  generate: (data) => api.post('/scheduler/generate', data),
  reschedule: (data) => api.post('/scheduler/reschedule', data)
};

// ---- Interviews ----
export const interviewApi = {
  getAll: () => api.get('/interviews'),
  getByStudent: (studentId) => api.get('/interviews', { params: { studentId } }),
  getByInterviewer: (interviewerId) => api.get('/interviews', { params: { interviewerId } }),
  getByDrive: (driveId) => api.get('/interviews', { params: { driveId } }),
  getById: (id) => api.get(`/interviews/${id}`),
  updateDetails: (id, data) => api.put(`/interviews/${id}`, data),
  updateStatus: (id, status) => api.put(`/interviews/${id}/status`, { status })
};

// ---- Feedback ----
export const feedbackApi = {
  submit: (interviewId, data) => api.post(`/interviews/${interviewId}/feedback`, data),
  get: (interviewId) => api.get(`/interviews/${interviewId}/feedback`)
};

// ---- Notifications ----
export const notificationApi = {
  getMine: () => api.get('/notifications'),
  unreadCount: () => api.get('/notifications/unread-count'),
  markRead: (id) => api.put(`/notifications/${id}/read`)
};

// ---- Reports ----
export const reportApi = {
  placements: () => api.get('/reports/placements'),
  companyWise: () => api.get('/reports/company-wise'),
  departmentWise: () => api.get('/reports/department-wise')
};

export default api;