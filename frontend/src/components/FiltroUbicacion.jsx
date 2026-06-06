import { useMemo } from 'react'

function FiltroUbicacion({ animales = [], provincia, ciudad, onProvinciaChange, onCiudadChange }) {
  const provincias = useMemo(() => {
    const set = new Set(animales.map(a => a.provincia).filter(Boolean))
    return [...set].sort()
  }, [animales])

  const ciudades = useMemo(() => {
    if (!provincia) return []
    const set = new Set(
      animales.filter(a => a.provincia === provincia).map(a => a.ciudad).filter(Boolean)
    )
    return [...set].sort()
  }, [animales, provincia])

  function handleProvincia(e) {
    onProvinciaChange(e.target.value)
    onCiudadChange('')
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
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
        </label>
      </div>
      {ciudades.length > 0 && (
        <div>
          <label>
            <strong>Ciudad:</strong>
            {' '}
            <select value={ciudad} onChange={e => onCiudadChange(e.target.value)}>
              <option value="">Todas</option>
              {ciudades.map(c => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </label>
        </div>
      )}
    </>
  )
}

export default FiltroUbicacion
