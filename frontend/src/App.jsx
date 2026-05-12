import { Routes, Route } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Header from './components/Header'
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
import Favoritos from './pages/Favoritos'
import './App.css'

function Home() {
  const { user } = useAuth()

  return (
    <div className="home">
      {user ? (
        <>
          <h1>Bienvenido, {user.nombre}</h1>
          <p>Plataforma de adopcion de mascotas</p>
        </>
      ) : (
        <>
          <h1>Bienvenido a Adoptar</h1>
          <p>Plataforma de adopcion de mascotas</p>
        </>
      )}
    </div>
  )
}

function App() {
  return (
    <AuthProvider>
      <Header />
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
        <Route path="/favoritos" element={<Favoritos />} />
      </Routes>
    </AuthProvider>
  )
}

export default App
