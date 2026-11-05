import { apiRequest } from './api'
export const getUnreadCount = (token) => apiRequest('/api/notifications/unread-count', { token })
export const getNotifications = (token) => apiRequest('/api/notifications/my', { token })
export const markNotificationRead = (notificationId, token) => apiRequest(`/api/notifications/${notificationId}/read`, { method: 'PATCH', token })
export const markAllNotificationsRead = (token) => apiRequest('/api/notifications/read-all', { method: 'PATCH', token })
