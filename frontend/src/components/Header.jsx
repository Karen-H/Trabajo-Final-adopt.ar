import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { switchProfile } from '../api/user'
import { getNoLeidos } from '../api/chat'
import { useState, useEffect } from 'react'
import NotificacionBell from './NotificacionBell'

function Header() {
  const { user, logout, setActiveProfile } = useAuth()
  const navigate = useNavigate()
  const [noLeidos, setNoLeidos] = useState(0)

  // polling de mensajes no leídos cada 10 segundos
  useEffect(() => {
    if (!user) return
    const cargar = () => getNoLeidos().then(r => setNoLeidos(r.data)).catch(() => {})
    cargar()
    const intervalo = setInterval(cargar, 10000)
    return () => clearInterval(intervalo)
  }, [user])

  async function handleSwitch() {
    try {
      const res = await switchProfile()
      setActiveProfile(res.data.activeProfile)
    } catch {
      alert('No se pudo cambiar el perfil.')
    }
  }

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <header style={{ borderBottom: '1px solid #ccc', padding: '10px 16px', marginBottom: '16px', display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
      <Link to="/" style={{ fontWeight: 'bold' }}>adopt.ar</Link>

      <Link to="/adopciones">Adopciones</Link>
      <Link to="/perdidos">Perdidos</Link>
      <Link to="/encontrados">Encontrados</Link>
      <Link to="/donar">Donar</Link>

      {user ? (
        <>
          {user.role === 'ADMIN' && (
            <>
              <Link to="/admin">Panel admin</Link>
              <Link to="/mis-publicaciones">Mis publicaciones</Link>
              <Link to="/agregar-reporte">Publicar perdido/encontrado</Link>
            </>
          )}
          {user.role === 'USER' && user.activeProfile === 'RESCATISTA' && (
            <>
              <Link to="/mis-publicaciones">Mis publicaciones</Link>
              <Link to="/agregar-animal">Publicar en adopción</Link>
              <Link to="/agregar-reporte">Publicar perdido/encontrado</Link>
              {user.tieneTienda
                ? <Link to="/mi-tienda">Mi tienda</Link>
                : <Link to="/abrir-tienda">Abrir tienda</Link>
              }
            </>
          )}
          {user.role === 'USER' && user.activeProfile === 'ADOPTANTE' && (
            <>
              <Link to="/mis-publicaciones">Mis publicaciones</Link>
              <Link to="/agregar-reporte">Publicar perdido/encontrado</Link>
            </>
          )}

          <Link to="/favoritos">Favoritos</Link>
          <Link to="/chats" style={{ position: 'relative' }}>
            Chats
            {noLeidos > 0 && (
              <span style={{
                position: 'absolute', top: -6, right: -10,
                background: '#e53935', color: '#fff',
                borderRadius: 10, fontSize: 10, padding: '1px 5px', lineHeight: 1.4
              }}>
                {noLeidos}
              </span>
            )}
          </Link>

          {user.role === 'USER' && user.preferencia === 'AMBOS' && (
            <span style={{ fontSize: 13, color: '#555' }}>
              Perfil: <strong>{user.activeProfile}</strong>
              {' '}
              <button onClick={handleSwitch} style={{ fontSize: 12 }}>
                Cambiar a {user.activeProfile === 'ADOPTANTE' ? 'Rescatista' : 'Adoptante'}
              </button>
            </span>
          )}

          <NotificacionBell />
          <Link to="/perfil">{user.nombre}</Link>
          <button onClick={handleLogout}>Cerrar sesión</button>
        </>
      ) : (
        <>
          <Link to="/login">Iniciar sesión</Link>
          <Link to="/register">Registrarse</Link>
        </>
      )}
    </header>
  )
}

export default Header
