import { Routes, Route } from 'react-router-dom'
import { Link } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import './App.css'

function Home() {
  const nombre = localStorage.getItem('nombre')

  return (
    <div className="home">
      {nombre ? (
        <h1>Bienvenido, {nombre}</h1>
      ) : (
        <h1>Bienvenido a Adoptar</h1>
      )}
      <p>Plataforma de adopcion de mascotas</p>
      {nombre ? (
        <button onClick={() => { localStorage.clear(); window.location.reload() }}>Cerrar sesión</button>
      ) : (
        <>
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
    </Routes>
  )
}

export default App
