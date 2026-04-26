import api from './api'

export function login(email, pass) {
  return api.post('/auth/login', { email, pass })
}

export function register(data) {
  return api.post('/auth/register', data)
}
