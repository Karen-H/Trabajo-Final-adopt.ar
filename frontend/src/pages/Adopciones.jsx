import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getAdopciones } from '../api/animal'
import FiltroUbicacion from '../components/FiltroUbicacion'

const TIPOS = ['PERRO', 'GATO', 'OTRO']
const ETIQUETA_TIPO = { PERRO: 'Perro', GATO: 'Gato', OTRO: 'Otro' }
const ETIQUETA_SEXO = { MACHO: 'Macho', HEMBRA: 'Hembra' }
const EDADES = [
  { value: 'CACHORRO', label: 'Cachorro (0-6 meses)' },
  { value: 'JOVEN', label: 'Joven (6 meses - 2 años)' },
  { value: 'ADULTO', label: 'Adulto (2-7 años)' },
  { value: 'SENIOR', label: 'Senior (7+ años)' },
]
const ETIQUETA_EDAD = {
  CACHORRO: 'Cachorro (0-6 meses)',
  JOVEN: 'Joven (6 meses - 2 años)',
  ADULTO: 'Adulto (2-7 años)',
  SENIOR: 'Senior (7+ años)',
}
const TIPOS_ADOPCION = [
  { value: 'PERMANENTE', label: 'Permanente' },
  { value: 'TRANSITO', label: 'Tránsito' },
]
const AMIGABLE_CON = [
  { key: 'amigableConGatos', label: 'Gatos' },
  { key: 'amigableConPerros', label: 'Perros' },
  { key: 'amigableConNinos', label: 'Niños' },
]

function filtrar(animales, tipos, provincia, ciudad, edades, tipoAdopcion, amigableCon) {
  return animales.filter(a => {
    const pasaTipo = tipos.length === 0 || tipos.includes(a.tipo)
    const pasaProv = !provincia || (a.provincia || '') === provincia
    const pasaCiudad = !ciudad || (a.ciudad || '') === ciudad
    const pasaEdad = edades.length === 0 || edades.includes(a.edad)
    const pasaTipoAdopcion = !tipoAdopcion || a.tipoAdopcion === tipoAdopcion
    const pasaAmigable = amigableCon.length === 0 || amigableCon.every(k => a[k])
    return pasaTipo && pasaProv && pasaCiudad && pasaEdad && pasaTipoAdopcion && pasaAmigable
  })
}

function Adopciones() {
  const [animales, setAnimales] = useState([])
  const [cargando, setCargando] = useState(true)
  const [tipos, setTipos] = useState([])
  const [provincia, setProvincia] = useState('')
  const [ciudad, setCiudad] = useState('')
  const [edades, setEdades] = useState([])
  const [tipoAdopcion, setTipoAdopcion] = useState('')
  const [amigableCon, setAmigableCon] = useState([])

  useEffect(() => {
    getAdopciones()
      .then(r => setAnimales(r.data))
      .catch(() => {})
      .finally(() => setCargando(false))
  }, [])

  function toggleTipo(tipo) {
    setTipos(prev => prev.includes(tipo) ? prev.filter(t => t !== tipo) : [...prev, tipo])
  }

  function toggleEdad(edad) {
    setEdades(prev => prev.includes(edad) ? prev.filter(e => e !== edad) : [...prev, edad])
  }

  function toggleAmigable(key) {
    setAmigableCon(prev => prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key])
  }

  const hayFiltros = tipos.length > 0 || provincia || ciudad || edades.length > 0 || tipoAdopcion || amigableCon.length > 0
  const visibles = filtrar(animales, tipos, provincia, ciudad, edades, tipoAdopcion, amigableCon)

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
        <div>
          <strong>Edad:</strong>{' '}
          {EDADES.map(e => (
            <label key={e.value} style={{ marginRight: 12 }}>
              <input
                type="checkbox"
                checked={edades.includes(e.value)}
                onChange={() => toggleEdad(e.value)}
              />
              {' '}{e.label}
            </label>
          ))}
        </div>
        <div>
          <strong>Tipo de adopción:</strong>{' '}
          {TIPOS_ADOPCION.map(t => (
            <label key={t.value} style={{ marginRight: 12 }}>
              <input
                type="radio"
                name="tipoAdopcion"
                value={t.value}
                checked={tipoAdopcion === t.value}
                onChange={() => setTipoAdopcion(t.value)}
              />
              {' '}{t.label}
            </label>
          ))}
        </div>
        <div>
          <strong>Amigable con:</strong>{' '}
          {AMIGABLE_CON.map(({ key, label }) => (
            <label key={key} style={{ marginRight: 12 }}>
              <input
                type="checkbox"
                checked={amigableCon.includes(key)}
                onChange={() => toggleAmigable(key)}
              />
              {' '}{label}
            </label>
          ))}
        </div>
        {hayFiltros && (
          <button onClick={() => { setTipos([]); setProvincia(''); setCiudad(''); setEdades([]); setTipoAdopcion(''); setAmigableCon([]) }}>
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
            {a.fotos?.length > 0 && (
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', margin: '6px 0' }}>
                {a.fotos.map(f => (
                  <img key={f.id} src={f.url} alt={a.nombre} style={{ width: 120, height: 120, objectFit: 'cover' }} />
                ))}
              </div>
            )}
            <p>Nombre: {a.nombre}</p>
            <p>Tipo: {ETIQUETA_TIPO[a.tipo]}</p>
            {a.sexo && <p>Sexo: {ETIQUETA_SEXO[a.sexo]}</p>}
            {a.edad && <p>Edad: {ETIQUETA_EDAD[a.edad]}</p>}
            {a.tipoAdopcion && <p>Tipo de adopción: {a.tipoAdopcion === 'PERMANENTE' ? 'Permanente' : 'Tránsito'}</p>}
            {(a.amigableConGatos || a.amigableConPerros || a.amigableConNinos) && (
              <p>Amigable con: {[a.amigableConGatos && 'gatos', a.amigableConPerros && 'perros', a.amigableConNinos && 'niños'].filter(Boolean).join(', ')}</p>
            )}
            {a.descripcion && <p>Descripción: {a.descripcion}</p>}
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
