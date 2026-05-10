import { useState, useEffect } from 'react'
import { getEncontrados } from '../api/reporte'
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

function Encontrados() {
  const [reportes, setReportes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [tipos, setTipos] = useState([])
  const [provincia, setProvincia] = useState('')
  const [ciudad, setCiudad] = useState('')

  useEffect(() => {
    getEncontrados()
      .then(r => setReportes(r.data))
      .catch(() => {})
      .finally(() => setCargando(false))
  }, [])

  function toggleTipo(tipo) {
    setTipos(prev => prev.includes(tipo) ? prev.filter(t => t !== tipo) : [...prev, tipo])
  }

  const hayFiltros = tipos.length > 0 || provincia || ciudad
  const visibles = filtrar(reportes, tipos, provincia, ciudad)

  if (cargando) return <p>Cargando...</p>

  return (
    <div>
      <h2>Animales encontrados</h2>

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
        <p>No hay reportes de animales encontrados en este momento.</p>
      ) : visibles.length === 0 ? (
        <p>No hay resultados para los filtros seleccionados.</p>
      ) : (
        visibles.map(r => (
          <div key={r.id} style={{ border: '1px solid #ccc', marginBottom: 12, padding: 12 }}>
            <strong>{ETIQUETA_TIPO[r.tipo]}</strong>
            {r.fotos?.length > 0 && (
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '6px 0' }}>
                {r.fotos.map(f => (
                  <img key={f.id} src={f.url} alt="foto" style={{ width: 120, height: 120, objectFit: 'cover' }} />
                ))}
              </div>
            )}
            {r.direccion && <p>Visto en: {r.direccion}</p>}
            {r.descripcion && <p>{r.descripcion}</p>}
            <p>
              {r.enPosesionDelPublicador
                ? 'El animal esta en posesion del publicador'
                : 'El animal no esta en posesion del publicador'}
            </p>
            <p style={{ fontSize: 12, color: '#666' }}>
              Publicado por {r.rescatistaNombre} en {r.ciudad}, {r.provincia}
            </p>
          </div>
        ))
      )}
    </div>
  )
}

export default Encontrados
