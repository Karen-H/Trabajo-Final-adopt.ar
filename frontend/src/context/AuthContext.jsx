import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

function leerUsuario() {
  const token = localStorage.getItem('token')
  if (!token) return null
  return {
    token,
    nombre: localStorage.getItem('nombre'),
    role: localStorage.getItem('role'),
    activeProfile: localStorage.getItem('activeProfile'),
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(leerUsuario)

  function login(data) {
    localStorage.setItem('token', data.token)
    localStorage.setItem('nombre', data.nombre)
    localStorage.setItem('role', data.role)
    localStorage.setItem('activeProfile', data.activeProfile)
    setUser({
      token: data.token,
      nombre: data.nombre,
      role: data.role,
      activeProfile: data.activeProfile,
    })
  }

  function logout() {
    localStorage.clear()
    setUser(null)
  }

  function setActiveProfile(profile) {
    localStorage.setItem('activeProfile', profile)
    setUser(prev => ({ ...prev, activeProfile: profile }))
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, setActiveProfile }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
