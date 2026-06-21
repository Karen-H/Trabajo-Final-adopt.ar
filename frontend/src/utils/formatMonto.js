export function formatMonto(valor) {
  return Number(valor).toLocaleString('es-AR', { maximumFractionDigits: 0 })
}
