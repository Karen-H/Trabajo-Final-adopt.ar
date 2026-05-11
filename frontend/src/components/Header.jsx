import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { switchProfile } from '../api/user'

function Header() {
  const { user, logout, setActiveProfile } = useAuth()
  const navigate = useNavigate()

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
            </>
          )}
          {user.role === 'USER' && user.activeProfile === 'ADOPTANTE' && (
            <>
              <Link to="/mis-publicaciones">Mis publicaciones</Link>
              <Link to="/agregar-reporte">Publicar perdido/encontrado</Link>
            </>
          )}

          <Link to="/favoritos">Favoritos</Link>

          {user.role === 'USER' && (
            <span style={{ fontSize: 13, color: '#555' }}>
              Perfil: <strong>{user.activeProfile}</strong>
              {' '}
              <button onClick={handleSwitch} style={{ fontSize: 12 }}>
                Cambiar a {user.activeProfile === 'ADOPTANTE' ? 'Rescatista' : 'Adoptante'}
              </button>
            </span>
          )}

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
