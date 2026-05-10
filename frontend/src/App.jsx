import { useState } from 'react'
import { Routes, Route } from 'react-router-dom'
import { Link } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import Profile from './pages/Profile'
import MisPublicaciones from './pages/MisPublicaciones'
import AgregarAnimal from './pages/AgregarAnimal'
import AdminPanel from './pages/AdminPanel'
import AgregarReporte from './pages/AgregarReporte'
import Adopciones from './pages/Adopciones'
import Perdidos from './pages/Perdidos'
import Encontrados from './pages/Encontrados'
import { switchProfile } from './api/user'
import './App.css'

function Home() {
  const nombre = localStorage.getItem('nombre')
  const role = localStorage.getItem('role')
  const [activeProfile, setActiveProfile] = useState(localStorage.getItem('activeProfile'))

  async function handleSwitch() {
    try {
      const res = await switchProfile()
      const newProfile = res.data.activeProfile
      localStorage.setItem('activeProfile', newProfile)
      setActiveProfile(newProfile)
    } catch {
      alert('No se pudo cambiar el perfil.')
    }
  }

  return (
    <div className="home">
      {nombre ? (
        <>
          <h1>Bienvenido, {nombre}</h1>
          <p>Plataforma de adopcion de mascotas</p>

          {role === 'USER' && (
            <div>
              <span>Perfil activo: <strong>{activeProfile}</strong></span>
              {' '}
              <button onClick={handleSwitch}>
                Cambiar a {activeProfile === 'ADOPTANTE' ? 'Rescatista' : 'Adoptante'}
              </button>
            </div>
          )}

          <nav>
            {role === 'ADMIN' ? (
              <>
                <div><Link to="/admin">Panel de administracion</Link></div>
                <div><Link to="/mis-publicaciones">Mis publicaciones</Link></div>
                <div><Link to="/agregar-reporte">Publicar perdido / encontrado</Link></div>
              </>
            ) : activeProfile === 'RESCATISTA' ? (
              <>
                <div><Link to="/mis-publicaciones">Mis publicaciones</Link></div>
                <div><Link to="/agregar-animal">Publicar animal en adopcion</Link></div>
                <div><Link to="/agregar-reporte">Publicar perdido / encontrado</Link></div>
              </>
            ) : (
              <>
                <div><Link to="/mis-publicaciones">Mis publicaciones</Link></div>
                <div><Link to="/agregar-reporte">Publicar perdido / encontrado</Link></div>
              </>
            )}
            <div><Link to="/adopciones">Animales en adopcion</Link></div>
            <div><Link to="/perdidos">Animales perdidos</Link></div>
            <div><Link to="/encontrados">Animales encontrados</Link></div>
          </nav>

          <br />
          <Link to="/perfil">Mi perfil</Link>
          {' | '}
          <button onClick={() => { localStorage.clear(); window.location.reload() }}>Cerrar sesion</button>
        </>
      ) : (
        <>
          <h1>Bienvenido a Adoptar</h1>
          <p>Plataforma de adopcion de mascotas</p>
          <nav>
            <div><Link to="/adopciones">Animales en adopcion</Link></div>
            <div><Link to="/perdidos">Animales perdidos</Link></div>
            <div><Link to="/encontrados">Animales encontrados</Link></div>
          </nav>
          <br />
          <Link to="/login">Iniciar sesion</Link>
          {' | '}
          <Link to="/register">Registrarse</Link>
        </>
      )}
    </div>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/perfil" element={<Profile />} />
      <Route path="/mis-publicaciones" element={<MisPublicaciones />} />
      <Route path="/agregar-animal" element={<AgregarAnimal />} />
      <Route path="/admin" element={<AdminPanel />} />
      <Route path="/agregar-reporte" element={<AgregarReporte />} />
      <Route path="/adopciones" element={<Adopciones />} />
      <Route path="/perdidos" element={<Perdidos />} />
      <Route path="/encontrados" element={<Encontrados />} />
    </Routes>
  )
}

export default App
