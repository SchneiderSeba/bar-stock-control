import type {Product} from './types'
export const stockUnit=(p:Product)=>p.unit==='keg'?'L':p.unit
export const pricedQuantity=(p:Product)=>p.unit==='keg'?p.stock/(p.kegSizeLitres||50):p.stock
export const purchaseUnit=(p:Product)=>p.unit==='keg'?`keg de ${p.kegSizeLitres} L`:p.unit
