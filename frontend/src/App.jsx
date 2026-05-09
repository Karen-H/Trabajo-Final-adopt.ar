import { useState } from 'react'
import { Routes, Route } from 'react-router-dom'
import { Link } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import Profile from './pages/Profile'
import MisAnimales from './pages/MisAnimales'
import AgregarAnimal from './pages/AgregarAnimal'
import AdminPanel from './pages/AdminPanel'
import { switchProfile } from './api/user'
import './App.css'

const MENU_ADOPTANTE = ['menu_adoptante1', 'menu_adoptante2', 'menu_adoptante3']

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
            {activeProfile === 'RESCATISTA' ? (
              <>
                <div><Link to="/mis-animales">Mis animales</Link></div>
                <div><Link to="/agregar-animal">Publicar animal</Link></div>
              </>
            ) : role === 'ADMIN' ? (
              <div><Link to="/admin">Panel de administracion</Link></div>
            ) : (
              MENU_ADOPTANTE.map(item => (
                <div key={item}><Link to={`/menu/${item}`}>{item}</Link></div>
              ))
            )}
          </nav>

          <br />
          <Link to="/perfil">Mi perfil</Link>
          {' | '}
          <button onClick={() => { localStorage.clear(); window.location.reload() }}>Cerrar sesión</button>
        </>
      ) : (
        <>
          <h1>Bienvenido a Adoptar</h1>
          <p>Plataforma de adopcion de mascotas</p>
          <Link to="/login">Iniciar sesión</Link>
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
      <Route path="/mis-animales" element={<MisAnimales />} />
      <Route path="/agregar-animal" element={<AgregarAnimal />} />
      <Route path="/admin" element={<AdminPanel />} />
    </Routes>
  )
}

export default App
