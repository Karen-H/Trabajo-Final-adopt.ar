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
    tieneTienda: localStorage.getItem('tieneTienda') === 'true',
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(leerUsuario)

  function login(data) {
    localStorage.setItem('token', data.token)
    localStorage.setItem('nombre', data.nombre)
    localStorage.setItem('role', data.role)
    localStorage.setItem('activeProfile', data.activeProfile)
    localStorage.setItem('tieneTienda', data.tieneTienda ? 'true' : 'false')
    setUser({
      token: data.token,
      nombre: data.nombre,
      role: data.role,
      activeProfile: data.activeProfile,
      tieneTienda: data.tieneTienda ?? false,
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

  function setTieneTienda(valor) {
    localStorage.setItem('tieneTienda', valor ? 'true' : 'false')
    setUser(prev => ({ ...prev, tieneTienda: valor }))
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, setActiveProfile, setTieneTienda }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
