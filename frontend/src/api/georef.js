const BASE = 'https://apis.datos.gob.ar/georef/api'

export async function getProvincias() {
  const res = await fetch(`${BASE}/provincias?orden=nombre&campos=id,nombre&max=100`)
  const data = await res.json()
  return data.provincias
}

export async function getMunicipios(provinciaNombre) {
  const res = await fetch(
    `${BASE}/localidades?provincia=${encodeURIComponent(provinciaNombre)}&orden=nombre&campos=id,nombre&max=1000`
  )
  const data = await res.json()
  return data.localidades
}

export async function ubicacionPorCoordenadas(lat, lon) {
  const res = await fetch(`${BASE}/ubicacion?lat=${lat}&lon=${lon}`)
  const data = await res.json()
  const ub = data.ubicacion
  return {
    provincia: ub?.provincia?.nombre || '',
    ciudad: ub?.localidad_censal?.nombre || ub?.municipio?.nombre || '',
  }
}
