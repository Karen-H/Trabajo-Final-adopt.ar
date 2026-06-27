import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getPerdidos } from '../api/reporte'
import FiltroUbicacion from '../components/FiltroUbicacion'
import Paginacion from '../components/Paginacion'
import LayoutConMapa from '../components/LayoutConMapa'

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
  const [reportes, setReportes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [tipos, setTipos] = useState([])
  const [provincia, setProvincia] = useState('')
  const [ciudad, setCiudad] = useState('')
  const [pagina, setPagina] = useState(1)

  useEffect(() => {
    getPerdidos()
      .then(r => setReportes(r.data))
      .catch(() => {})
      .finally(() => setCargando(false))
  }, [])

  function toggleTipo(tipo) {
    setTipos(prev => prev.includes(tipo) ? prev.filter(t => t !== tipo) : [...prev, tipo])
  }

  const hayFiltros = tipos.length > 0 || provincia || ciudad
  const visibles = filtrar(reportes, tipos, provincia, ciudad)

  useEffect(() => { setPagina(1) }, [tipos, provincia, ciudad])

  const POR_PAGINA = 12
  const paginados = visibles.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA)

  if (cargando) return <p>Cargando...</p>

  const filtros = (
    <>
      <div>
        <strong>Tipo:</strong>
        <div style={{ display: 'flex', flexDirection: 'column', marginTop: 4 }}>
          {TIPOS.map(t => (
            <label key={t}>
              <input type="checkbox" checked={tipos.includes(t)} onChange={() => toggleTipo(t)} />
              {' '}{ETIQUETA_TIPO[t]}
            </label>
          ))}
        </div>
      </div>
      <FiltroUbicacion
        animales={reportes}
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
    </>
  )

  return (
    <div>
      <h2>Animales perdidos</h2>
      <LayoutConMapa filtros={filtros} animales={visibles}>
        {reportes.length === 0 ? (
          <p>No hay reportes de animales perdidos en este momento.</p>
        ) : visibles.length === 0 ? (
          <p>No hay resultados para los filtros seleccionados.</p>
        ) : (
          <>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              {paginados.map(r => (
                <div key={r.id} style={{ border: '1px solid #ccc', borderRadius: 4 }}>
                  <Link to={`/animal/${r.id}`} style={{ display: 'block', padding: 12, color: 'inherit', textDecoration: 'none' }}>
                    {r.fotos?.length > 0 && (
                      <img src={r.fotos[0].url} alt="foto" style={{ width: 120, height: 120, objectFit: 'cover', display: 'block', marginBottom: 8 }} />
                    )}
                    <p style={{ margin: '2px 0', fontWeight: 600 }}>{r.nombre || ETIQUETA_TIPO[r.tipo]}</p>
                    {(r.ciudad || r.provincia) && (
                      <p style={{ margin: '2px 0', fontSize: 12, color: '#666' }}>{[r.ciudad, r.provincia].filter(Boolean).join(', ')}</p>
                    )}
                  </Link>
                </div>
              ))}
            </div>
            <Paginacion total={visibles.length} porPagina={POR_PAGINA} pagina={pagina} onChange={setPagina} />
          </>
        )}
      </LayoutConMapa>
    </div>
  )
}

export default Perdidos
