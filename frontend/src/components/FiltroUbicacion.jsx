import { useState, useEffect } from 'react'
import { getProvincias, getMunicipios } from '../api/georef'

function FiltroUbicacion({ provincia, ciudad, onProvinciaChange, onCiudadChange }) {
  const [provincias, setProvincias] = useState([])
  const [municipios, setMunicipios] = useState([])

  useEffect(() => {
    getProvincias().then(setProvincias).catch(() => setProvincias([]))
  }, [])

  async function handleProvincia(e) {
    const val = e.target.value
    onProvinciaChange(val)
    onCiudadChange('')
    if (val) {
      const munis = await getMunicipios(val).catch(() => [])
      setMunicipios(munis)
    } else {
      setMunicipios([])
    }
  }

  return (
    <>
      <div>
        <label>
          <strong>Provincia:</strong>
          {' '}
          <select value={provincia} onChange={handleProvincia}>
            <option value="">Todas</option>
            {provincias.map(p => (
              <option key={p.id} value={p.nombre}>{p.nombre}</option>
            ))}
          </select>
        </label>
      </div>
      {municipios.length > 0 && (
        <div>
          <label>
            <strong>Ciudad:</strong>
            {' '}
            <select value={ciudad} onChange={e => onCiudadChange(e.target.value)}>
              <option value="">Todas</option>
              {municipios.map(m => (
                <option key={m.id} value={m.nombre}>{m.nombre}</option>
              ))}
            </select>
          </label>
        </div>
      )}
    </>
  )
}

export default FiltroUbicacion
