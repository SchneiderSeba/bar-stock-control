import test from 'node:test'
import assert from 'node:assert/strict'
import {assembleInvoiceRows,parseInvoiceOcr} from '../src/invoiceOcr.ts'

const line=(text,xmin,ymin,xmax=xmin+80,ymax=ymin+15,score=.95)=>({text,score,box:{xmin,ymin,xmax,ymax}})
const catalog=[
 {productId:1,productName:'Guinness Keg 50L',supplierSku:'50055',aliases:['50055','BEER-001']},
 {productId:2,productName:'Jameson 700ml',supplierSku:'9878',aliases:['9878','SPIR-001']},
]

test('assembles OCR fragments into visual rows',()=>{
 const rows=assembleInvoiceRows([line('50055',10,100),line('Guinness Keg 50L',100,102),line('2',390,101),line('85,50',460,101),line('171,00',540,101)])
 assert.equal(rows.length,1)
 assert.equal(rows[0].text,'50055 Guinness Keg 50L 2 85,50 171,00')
})

test('extracts metadata and editable product suggestions',()=>{
 const lines=[
  line('FACTURA INV-2048',10,10,220),line('Fecha 21/09/2026',10,35,220),
  line('50055',10,100),line('Guinness Keg 50L',100,102,280),line('2',390,101),line('85,50',460,101),line('171,00',540,101),
  line('9878 Jameson 700ml 3 22.40 67.20',10,140,560),
 ]
 const draft=parseInvoiceOcr(lines,'',catalog)
 assert.equal(draft.invoiceNumber,'INV-2048')
 assert.equal(draft.invoiceDate,'2026-09-21')
 assert.deepEqual(draft.lines.map(({productId,quantity,unitCost,supplierSku})=>({productId,quantity,unitCost,supplierSku})),[
  {productId:1,quantity:'2',unitCost:'85.50',supplierSku:'50055'},
  {productId:2,quantity:'3',unitCost:'22.40',supplierSku:'9878'},
 ])
})

test('accepts an internal SKU and leaves missing cost for user confirmation',()=>{
 const draft=parseInvoiceOcr([line('SPIR-001 Jameson 700ml 4',10,100,420)],'',catalog)
 assert.equal(draft.lines[0].productId,2)
 assert.equal(draft.lines[0].quantity,'4')
 assert.equal(draft.lines[0].unitCost,'')
})

test('extracts an unknown supplier SKU so the invoice can request product details',()=>{
 const draft=parseInvoiceOcr([line('NEW-77 Tonic Water 24 1.50 36.00',10,100,520)],'',catalog)
 assert.deepEqual(draft.unknownLines.map(({supplierSku,productName,quantity,unitCost})=>({supplierSku,productName,quantity,unitCost})),[
  {supplierSku:'NEW-77',productName:'Tonic Water',quantity:'24',unitCost:'1.50'},
 ])
})
