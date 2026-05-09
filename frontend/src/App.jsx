import { useState } from 'react'
import { Routes, Route, useParams } from 'react-router-dom'
import { Link } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import Profile from './pages/Profile'
import { switchProfile } from './api/user'
import './App.css'

const MENU_ADOPTANTE = ['menu_adoptante1', 'menu_adoptante2', 'menu_adoptante3']
const MENU_RESCATISTA = ['menu_rescatista1', 'menu_rescatista2', 'menu_rescatista3']
const MENU_ADMIN = ['menu_admin1', 'menu_admin2', 'menu_admin3']

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

  function getMenu() {
    if (role === 'ADMIN') return MENU_ADMIN
    if (activeProfile === 'RESCATISTA') return MENU_RESCATISTA
    return MENU_ADOPTANTE
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
            {getMenu().map(item => (
              <div key={item}><Link to={`/menu/${item}`}>{item}</Link></div>
            ))}
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

function MenuPage() {
  const { nombre } = useParams()
  if (!localStorage.getItem('token')) {
    window.location.href = '/login'
    return null
  }
  return (
    <div>
      <h2>{nombre}</h2>
      <p>Contenido de {nombre}</p>
      <Link to="/">Volver</Link>
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
      <Route path="/menu/:nombre" element={<MenuPage />} />
    </Routes>
  )
}

export default App
