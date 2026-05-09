const BASE = 'https://apis.datos.gob.ar/georef/api'

export async function getProvincias() {
  const res = await fetch(`${BASE}/provincias?orden=nombre&campos=id,nombre&max=100`)
  const data = await res.json()
  return data.provincias
}

export async function getMunicipios(provinciaNombre) {
  const res = await fetch(
    `${BASE}/municipios?provincia=${encodeURIComponent(provinciaNombre)}&orden=nombre&campos=id,nombre&max=500`
  )
  const data = await res.json()
  return data.municipios
}
