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
import SolicitarTienda from './pages/SolicitarTienda'
import MiTienda from './pages/MiTienda'
import Donar from './pages/Donar'
import DonarResultado from './pages/DonarResultado'
import Chats from './pages/Chats'
import AnimalDetalle from './pages/AnimalDetalle'
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
        <Route path="/abrir-tienda" element={<SolicitarTienda />} />
        <Route path="/mi-tienda" element={<MiTienda />} />
        <Route path="/donar" element={<Donar />} />
        <Route path="/donar/exito" element={<DonarResultado estado="exito" />} />
        <Route path="/donar/fallo" element={<DonarResultado estado="fallo" />} />
        <Route path="/donar/pendiente" element={<DonarResultado estado="pendiente" />} />
        <Route path="/chats" element={<Chats />} />
        <Route path="/animal/:id" element={<AnimalDetalle />} />
      </Routes>
    </AuthProvider>
  )
}

export default App
