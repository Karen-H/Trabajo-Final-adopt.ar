import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getAdopciones } from '../api/animal'
import FiltroUbicacion from '../components/FiltroUbicacion'

const TIPOS = ['PERRO', 'GATO', 'OTRO']
const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }

function filtrar(animales, tipos, provincia, ciudad) {
  return animales.filter(a => {
    const pasaTipo = tipos.length === 0 || tipos.includes(a.tipo)
    const pasaProv = !provincia || (a.provincia || '') === provincia
    const pasaCiudad = !ciudad || (a.ciudad || '') === ciudad
    return pasaTipo && pasaProv && pasaCiudad
  })
}

function Adopciones() {
  const [animales, setAnimales] = useState([])
  const [cargando, setCargando] = useState(true)
  const [tipos, setTipos] = useState([])
  const [provincia, setProvincia] = useState('')
  const [ciudad, setCiudad] = useState('')

  useEffect(() => {
    getAdopciones()
      .then(r => setAnimales(r.data))
      .catch(() => {})
      .finally(() => setCargando(false))
  }, [])

  function toggleTipo(tipo) {
    setTipos(prev => prev.includes(tipo) ? prev.filter(t => t !== tipo) : [...prev, tipo])
  }

  const hayFiltros = tipos.length > 0 || provincia || ciudad
  const visibles = filtrar(animales, tipos, provincia, ciudad)

  if (cargando) return <p>Cargando...</p>

  return (
    <div>
      <h2>Animales en adopcion</h2>

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

      {animales.length === 0 ? (
        <p>No hay animales en adopcion en este momento.</p>
      ) : visibles.length === 0 ? (
        <p>No hay resultados para los filtros seleccionados.</p>
      ) : (
        visibles.map(a => (
          <div key={a.id} style={{ border: '1px solid #ccc', marginBottom: 12, padding: 12 }}>
            <strong>{a.nombre}</strong>
            {' '}
            ({ETIQUETA_TIPO[a.tipo]})
            {a.fotos?.length > 0 && (
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '6px 0' }}>
                {a.fotos.map(f => (
                  <img key={f.id} src={f.url} alt={a.nombre} style={{ width: 120, height: 120, objectFit: 'cover' }} />
                ))}
              </div>
            )}
            {a.descripcion && <p>{a.descripcion}</p>}
            <p style={{ fontSize: 12, color: '#666' }}>
              Publicado por {a.rescatistaNombre} en {a.ciudad}, {a.provincia}
            </p>
          </div>
        ))
      )}

      <br />
      <Link to="/">Volver al inicio</Link>
    </div>
  )
}

export default Adopciones
