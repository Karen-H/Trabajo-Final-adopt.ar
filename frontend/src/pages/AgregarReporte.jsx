import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { crearReporte, buscarNominatim } from '../api/reporte'

const TIPOS = ['PERRO', 'GATO', 'OTRO']
const ESTADOS = [
  { value: 'PERDIDO', label: 'Lo perdí (mi animal)' },
  { value: 'ENCONTRADO', label: 'Lo encontré en la calle' },
]

function AgregarReporte() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    tipo: '',
    estadoInicial: '',
    direccion: '',
    latitud: '',
    longitud: '',
    provincia: '',
    ciudad: '',
    fechaAvistamiento: '',
    enPosesionDelPublicador: '',
    descripcion: '',
  })
  const [fotos, setFotos] = useState([])
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  // autocomplete
  const [sugerencias, setSugerencias] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const debounceRef = useRef(null)

  useEffect(() => {
    if (!busqueda || busqueda.length < 4) {
      setSugerencias([])
      return
    }
    clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(async () => {
      try {
        const results = await buscarNominatim(busqueda)
        setSugerencias(results)
      } catch {
        setSugerencias([])
      }
    }, 400)
  }, [busqueda])

  async function elegirSugerencia(lugar) {
    clearTimeout(debounceRef.current)
    const addr = lugar.address || {}
    const provincia = addr.state || ''
    const ciudad = addr.suburb || addr.quarter || addr.city_district || addr.city || addr.town || addr.village || addr.municipality || ''
    setForm(f => ({
      ...f,
      direccion: lugar.display_name,
      latitud: lugar.lat,
      longitud: lugar.lon,
      provincia,
      ciudad,
    }))
    setBusqueda(lugar.display_name)
    setSugerencias([])
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm(f => {
      const next = { ...f, [name]: value }
      if (name === 'estadoInicial' && value === 'PERDIDO') {
        next.enPosesionDelPublicador = ''
      }
      return next
    })
  }

  function handleFotos(e) {
    setFotos(Array.from(e.target.files))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    if (!form.tipo) return setError('Seleccioná el tipo de animal')
    if (!form.estadoInicial) return setError('Indicá si lo perdiste o lo encontraste')
    if (!form.direccion) return setError('Ingresá la dirección')
    if (!form.latitud || !form.longitud) return setError('Seleccioná una dirección de la lista para guardar las coordenadas')
    if (!form.fechaAvistamiento) return setError('Ingresá la fecha')
    if (form.estadoInicial === 'ENCONTRADO' && form.enPosesionDelPublicador === '') return setError('Indicá si el animal está en tu posesión')
    if (fotos.length === 0) return setError('Subí al menos una foto')
    if (fotos.length > 5) return setError('Máximo 5 fotos')

    const data = new FormData()
    data.append('tipo', form.tipo)
    data.append('estadoInicial', form.estadoInicial)
    data.append('direccion', form.direccion)
    data.append('latitud', form.latitud)
    data.append('longitud', form.longitud)
    if (form.provincia) data.append('provincia', form.provincia)
    if (form.ciudad) data.append('ciudad', form.ciudad)
    data.append('fechaAvistamiento', form.fechaAvistamiento)
    data.append('enPosesionDelPublicador', form.estadoInicial === 'PERDIDO' ? 'false' : form.enPosesionDelPublicador)
    if (form.descripcion) data.append('descripcion', form.descripcion)
    fotos.forEach(f => data.append('fotos', f))

    setCargando(true)
    try {
      await crearReporte(data)
      navigate('/mis-publicaciones')
    } catch (e) {
      setError(e.response?.data || 'Error al publicar el reporte')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div>
      <h2>Publicar animal perdido / encontrado</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Tipo de animal</label>
          <select name="tipo" value={form.tipo} onChange={handleChange}>
            <option value="">-- Seleccioná --</option>
            {TIPOS.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>

        <div>
          <label>¿Qué pasó?</label>
          <select name="estadoInicial" value={form.estadoInicial} onChange={handleChange}>
            <option value="">-- Seleccioná --</option>
            {ESTADOS.map(e => <option key={e.value} value={e.value}>{e.label}</option>)}
          </select>
        </div>

        <div style={{ position: 'relative' }}>
          <label>{form.estadoInicial === 'PERDIDO' ? 'Lugar donde se perdió' : 'Dirección donde fue visto'}</label>
          <input
            type="text"
            value={busqueda}
            onChange={e => {
              setBusqueda(e.target.value)
              setForm(f => ({ ...f, direccion: e.target.value, latitud: '', longitud: '' }))
            }}
            placeholder="Ej: Av. Corrientes 1234, Buenos Aires"
            autoComplete="off"
          />
          {sugerencias.length > 0 && (
            <ul style={{
              position: 'absolute',
              top: '100%',
              left: 0,
              right: 0,
              border: '1px solid #ccc',
              padding: 0,
              margin: 0,
              listStyle: 'none',
              background: '#fff',
              color: '#222',
              zIndex: 1000,
              boxShadow: '0 2px 6px rgba(0,0,0,0.15)',
              maxHeight: '200px',
              overflowY: 'auto',
            }}>
              {sugerencias.map(s => (
                <li
                  key={s.place_id}
                  onMouseDown={() => elegirSugerencia(s)}
                  style={{ padding: '8px 10px', cursor: 'pointer', borderBottom: '1px solid #eee' }}
                  onMouseEnter={e => e.currentTarget.style.background = '#f5f5f5'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                >
                  {s.display_name}
                </li>
              ))}
            </ul>
          )}
        </div>

        {form.estadoInicial === 'ENCONTRADO' && (
          <div>
            <label>¿El animal está en tu posesión?</label>
            <select name="enPosesionDelPublicador" value={form.enPosesionDelPublicador} onChange={handleChange}>
              <option value="">-- Seleccioná --</option>
              <option value="true">Sí, está conmigo</option>
              <option value="false">No</option>
            </select>
          </div>
        )}
        <div>
          <label>{form.estadoInicial === 'PERDIDO' ? 'Fecha en que se perdió' : 'Fecha en que fue visto'}</label>
          <input
            type="date"
            name="fechaAvistamiento"
            value={form.fechaAvistamiento}
            onChange={handleChange}
            max={new Date().toISOString().split('T')[0]}
          />
        </div>
        <div>
          <label>Descripción (opcional)</label>
          <textarea
            name="descripcion"
            value={form.descripcion}
            onChange={handleChange}
            placeholder="Color, señas particulares, collar, responde al nombre..."
            rows={3}
          />
        </div>

        <div>
          <label>Fotos (1 a 5)</label>
          <input type="file" accept="image/*" multiple onChange={handleFotos} />
        </div>

        {error && <p style={{ color: 'red' }}>{error}</p>}

        <button type="submit" disabled={cargando}>
          {cargando ? 'Publicando...' : 'Publicar'}
        </button>
      </form>
    </div>
  )
}

export default AgregarReporte
