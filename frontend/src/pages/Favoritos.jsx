import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getFavoritos, quitarFavorito } from '../api/favorito'

const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }
const ETIQUETA_CATEGORIA = {
  ADOPCION: 'En adopción',
  PERDIDO_ENCONTRADO: 'Perdido / Encontrado',
}

function Favoritos() {
  const navigate = useNavigate()
  const [favoritos, setFavoritos] = useState([])
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    if (!localStorage.getItem('token')) {
      navigate('/login')
      return
    }
    getFavoritos()
      .then(r => setFavoritos(r.data))
      .catch(() => {})
      .finally(() => setCargando(false))
  }, [navigate])

  async function handleQuitar(animalId) {
    await quitarFavorito(animalId)
    setFavoritos(prev => prev.filter(f => f.animalId !== animalId))
  }

  if (cargando) return <p>Cargando...</p>

  return (
    <div>
      <h2>Mis favoritos</h2>

      {favoritos.length === 0 ? (
        <p>Todavía no tenés favoritos guardados.</p>
      ) : (
        favoritos.map(fav => {
          const a = fav.animal
          return (
            <div
              key={fav.animalId}
              style={{
                border: '1px solid #ccc',
                marginBottom: 12,
                padding: 12,
                position: 'relative',
                opacity: fav.disponible ? 1 : 0.7,
              }}
            >
              {!fav.disponible && (
                <div style={{
                  position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
                  backdropFilter: 'blur(2px)', display: 'flex', alignItems: 'center',
                  justifyContent: 'center', zIndex: 1, pointerEvents: 'none',
                }}>
                  <span style={{ background: 'rgba(200,0,0,0.8)', color: '#fff', padding: '4px 16px', fontWeight: 'bold', borderRadius: 4 }}>
                    No disponible
                  </span>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <small style={{ color: '#888' }}>{ETIQUETA_CATEGORIA[a.categoria]}</small>
                  {a.nombre && <h3 style={{ margin: '4px 0' }}>{a.nombre}</h3>}
                  {!a.nombre && <h3 style={{ margin: '4px 0' }}>{ETIQUETA_TIPO[a.tipo]}</h3>}
                </div>
                <button
                  onClick={() => handleQuitar(fav.animalId)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 22, zIndex: 2, position: 'relative' }}
                  title="Quitar de favoritos"
                >
                  ❤️
                </button>
              </div>

              {a.fotos?.length > 0 && (
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '6px 0' }}>
                  {a.fotos.map(f => (
                    <img key={f.id} src={f.url} alt="foto" style={{ width: 100, height: 100, objectFit: 'cover' }} />
                  ))}
                </div>
              )}

              <p>Tipo: {ETIQUETA_TIPO[a.tipo]}</p>
              {a.sexo && <p>Sexo: {a.sexo === 'MACHO' ? 'Macho' : 'Hembra'}</p>}
              {a.edad && <p>Edad: {a.edad}</p>}
              {a.descripcion && <p>Descripción: {a.descripcion}</p>}
              {a.direccion && <p>Visto en: {a.direccion}</p>}
              <p style={{ fontSize: 12, color: '#666' }}>
                {a.ciudad}, {a.provincia}
              </p>
            </div>
          )
        })
      )}
    </div>
  )
}

export default Favoritos
