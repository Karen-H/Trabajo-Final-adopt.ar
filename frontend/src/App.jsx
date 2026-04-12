import { Routes, Route } from 'react-router-dom'
import './App.css'

function Home() {
  return (
    <div className="home">
      <h1>Bienvenido a Adoptar</h1>
      <p>Plataforma de adopcion de mascotas</p>
    </div>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
    </Routes>
  )
}

export default App
