import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPerdidos } from '../api/reporte'
import { getFavoritos, agregarFavorito, quitarFavorito } from '../api/favorito'
import FiltroUbicacion from '../components/FiltroUbicacion'

const TIPOS = ['PERRO', 'GATO', 'OTRO']
const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }

function filtrar(reportes, tipos, provincia, ciudad) {
  return reportes.filter(r => {
    const pasaTipo = tipos.length === 0 || tipos.includes(r.tipo)
    const pasaProv = !provincia || (r.provincia || '') === provincia
    const pasaCiudad = !ciudad || (r.ciudad || '') === ciudad
    return pasaTipo && pasaProv && pasaCiudad
  })
}

function Perdidos() {
  const navigate = useNavigate()
  const estaLogueado = !!localStorage.getItem('token')
  const [reportes, setReportes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [tipos, setTipos] = useState([])
  const [provincia, setProvincia] = useState('')
  const [ciudad, setCiudad] = useState('')
  const [favoritoIds, setFavoritoIds] = useState(new Set())

  useEffect(() => {
    getPerdidos()
      .then(r => setReportes(r.data))
      .catch(() => {})
      .finally(() => setCargando(false))
    if (estaLogueado) {
      getFavoritos()
        .then(r => setFavoritoIds(new Set(r.data.map(f => f.animalId))))
        .catch(() => {})
    }
  }, [])

  function toggleTipo(tipo) {
    setTipos(prev => prev.includes(tipo) ? prev.filter(t => t !== tipo) : [...prev, tipo])
  }

  async function toggleFavorito(animalId) {
    if (!estaLogueado) {
      localStorage.setItem('pendingFavorito', animalId)
      navigate('/login')
      return
    }
    if (favoritoIds.has(animalId)) {
      await quitarFavorito(animalId)
      setFavoritoIds(prev => { const s = new Set(prev); s.delete(animalId); return s })
    } else {
      await agregarFavorito(animalId)
      setFavoritoIds(prev => new Set([...prev, animalId]))
    }
  }

  const hayFiltros = tipos.length > 0 || provincia || ciudad
  const visibles = filtrar(reportes, tipos, provincia, ciudad)

  if (cargando) return <p>Cargando...</p>

  return (
    <div>
      <h2>Animales perdidos</h2>

      <div style={{ marginBottom: '1rem', display: 'flex', gap: '1.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
        <div>
          <strong>Tipo:</strong>
          {' '}
          {TIPOS.map(t => (
            <label key={t} style={{ marginRight: 12 }}>
              <input
                type="checkbox"
                checked={tipos.includes(t)}
                onChange={() => toggleTipo(t)}
              />
              {' '}{ETIQUETA_TIPO[t]}
            </label>
          ))}
        </div>
        <FiltroUbicacion
          provincia={provincia}
          ciudad={ciudad}
          onProvinciaChange={setProvincia}
          onCiudadChange={setCiudad}
        />
        {hayFiltros && (
          <button onClick={() => { setTipos([]); setProvincia(''); setCiudad('') }}>
            Limpiar filtros
          </button>
        )}
      </div>

      {reportes.length === 0 ? (
        <p>No hay reportes de animales perdidos en este momento.</p>
      ) : visibles.length === 0 ? (
        <p>No hay resultados para los filtros seleccionados.</p>
      ) : (
        visibles.map(r => (
          <div key={r.id} style={{ border: '1px solid #ccc', marginBottom: 12, padding: 12 }}>
            {r.fotos?.length > 0 && (
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '6px 0' }}>
                {r.fotos.map(f => (
                  <img key={f.id} src={f.url} alt="foto" style={{ width: 120, height: 120, objectFit: 'cover' }} />
                ))}
              </div>
            )}
            <p>Tipo: {ETIQUETA_TIPO[r.tipo]}</p>
            {r.direccion && <p>Visto en: {r.direccion}</p>}
            {r.fechaAvistamiento && <p>Fecha: {r.fechaAvistamiento}</p>}
            {r.descripcion && <p>Descripción: {r.descripcion}</p>}
            <p style={{ fontSize: 12, color: '#666' }}>
              Publicado por {r.rescatistaNombre} en {r.ciudad}, {r.provincia}
            </p>
            <button
              onClick={() => toggleFavorito(r.id)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 22, padding: '4px 0' }}
              title={favoritoIds.has(r.id) ? 'Quitar de favoritos' : 'Agregar a favoritos'}
            >
              {favoritoIds.has(r.id) ? '❤️' : '🤍'}
            </button>
          </div>
        ))
      )}
    </div>
  )
}

export default Perdidos
