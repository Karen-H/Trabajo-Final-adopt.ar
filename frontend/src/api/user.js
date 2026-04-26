import api from './api'

export function getProfile() {
  return api.get('/user/profile')
}

export function updateProfile(data) {
  return api.put('/user/profile', data)
}
