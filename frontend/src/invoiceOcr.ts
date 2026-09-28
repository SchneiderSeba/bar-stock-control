export type InvoiceOcrSourceLine={text:string;score?:number;box:{xmin:number;ymin:number;xmax:number;ymax:number}}
export type InvoiceOcrCatalogItem={productId:number;productName:string;supplierSku:string;aliases:string[]}
export type InvoiceOcrLine={productId:number;productName:string;supplierSku:string;quantity:string;unitCost:string;sourceText:string;confidence:number}
export type InvoiceOcrUnknownLine={supplierSku:string;productName:string;quantity:string;unitCost:string;sourceText:string;confidence:number}
export type InvoiceOcrDraft={invoiceNumber:string;invoiceDate:string;lines:InvoiceOcrLine[];unknownLines:InvoiceOcrUnknownLine[];unmatchedRows:string[]}

type Row={text:string;center:number;height:number;confidence:number}
const normalized=(value:string)=>` ${value.toUpperCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^A-Z0-9]+/g,' ').trim()} `
const escaped=(value:string)=>value.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')

export function assembleInvoiceRows(lines:InvoiceOcrSourceLine[]):Row[]{
 const rows:{parts:InvoiceOcrSourceLine[];center:number;height:number}[]=[]
 for(const line of [...lines].filter(line=>line.text.trim()).sort((a,b)=>(a.box.ymin+a.box.ymax)/2-(b.box.ymin+b.box.ymax)/2||a.box.xmin-b.box.xmin)){
  const center=(line.box.ymin+line.box.ymax)/2,height=Math.max(1,line.box.ymax-line.box.ymin)
  const row=rows.find(candidate=>Math.abs(candidate.center-center)<=Math.max(8,Math.min(candidate.height,height)*.7))
  if(row){row.parts.push(line);const count=row.parts.length;row.center=(row.center*(count-1)+center)/count;row.height=Math.max(row.height,height)}
  else rows.push({parts:[line],center,height})
 }
 return rows.sort((a,b)=>a.center-b.center).map(row=>{const parts=row.parts.sort((a,b)=>a.box.xmin-b.box.xmin);return{text:parts.map(part=>part.text.trim()).join(' '),center:row.center,height:row.height,confidence:parts.reduce((sum,part)=>sum+(part.score??0),0)/parts.length}})
}

function parseAmount(value:string){
 let clean=value.replace(/[^\d,.-]/g,'').replace(/\s/g,'')
 const comma=clean.lastIndexOf(','),dot=clean.lastIndexOf('.')
 if(comma>=0&&dot>=0) clean=comma>dot?clean.replace(/\./g,'').replace(',','.'):clean.replace(/,/g,'')
 else if(comma>=0) clean=/,\d{1,2}$/.test(clean)?clean.replace(',','.'):clean.replace(/,/g,'')
 else if(dot>=0&&!/\.\d{1,2}$/.test(clean)) clean=clean.replace(/\./g,'')
 const number=Number(clean)
 return Number.isFinite(number)&&number>=0?number:undefined
}

function amounts(value:string){return [...value.matchAll(/(?:€|EUR|£|\$)?\s*\d+(?:[.,]\d+)?/gi)].map(match=>parseAmount(match[0])).filter((number):number is number=>number!==undefined)}
function value(number:number|undefined,decimals:number){return number===undefined?'':number.toFixed(decimals).replace(/\.0+$/,'')}

function parseDate(text:string){
 const iso=text.match(/\b(20\d{2})[-/.](\d{1,2})[-/.](\d{1,2})\b/)
 const local=text.match(/\b(\d{1,2})[-/.](\d{1,2})[-/.](20\d{2})\b/)
 const parts=iso?[Number(iso[1]),Number(iso[2]),Number(iso[3])]:local?[Number(local[3]),Number(local[2]),Number(local[1])]:undefined
 if(!parts)return'';const [year,month,day]=parts;const date=new Date(Date.UTC(year,month-1,day));return date.getUTCFullYear()===year&&date.getUTCMonth()===month-1&&date.getUTCDate()===day?`${year}-${String(month).padStart(2,'0')}-${String(day).padStart(2,'0')}`:''
}

function parseInvoiceNumber(text:string){
 const match=text.match(/(?:FACTURA|INVOICE|INV(?:OICE)?|N[º°O.]?)\s*(?:N[º°O.]?\s*)?[:#-]?\s*([A-Z0-9][A-Z0-9/-]{2,})/i)
 return match?.[1]??''
}

export function parseInvoiceOcr(sourceLines:InvoiceOcrSourceLine[],rawText:string,catalog:InvoiceOcrCatalogItem[]):InvoiceOcrDraft{
 const rows=assembleInvoiceRows(sourceLines),result:InvoiceOcrLine[]=[];const unknownLines:InvoiceOcrUnknownLine[]=[];const unmatchedRows:string[]=[];const usedRows=new Set<number>()
 rows.forEach((row,rowIndex)=>{
  const rowNormalized=normalized(row.text)
  const candidates=catalog.flatMap(item=>item.aliases.map(alias=>({item,alias,needle:normalized(alias).trim()}))).filter(candidate=>candidate.needle&&rowNormalized.includes(` ${candidate.needle} `)).sort((a,b)=>b.needle.length-a.needle.length)
  const candidate=candidates.find(({item})=>!result.some(line=>line.productId===item.productId))
  if(!candidate)return
  const aliasPattern=new RegExp(escaped(candidate.alias).replace(/\s+/g,'\\s*'),'i')
  const aliasMatch=aliasPattern.exec(row.text);let remainder=aliasMatch?row.text.slice(aliasMatch.index+aliasMatch[0].length):row.text
  remainder=remainder.replace(new RegExp(escaped(candidate.item.productName).replace(/\s+/g,'\\s+'),'i'),' ')
  const numbers=amounts(remainder);let quantity:number|undefined,unitCost:number|undefined
  if(numbers.length>=3){quantity=numbers[numbers.length-3];unitCost=numbers[numbers.length-2]}
  else if(numbers.length===2){quantity=numbers[0];unitCost=numbers[1]}
  else if(numbers.length===1) quantity=numbers[0]
  result.push({productId:candidate.item.productId,productName:candidate.item.productName,supplierSku:candidate.item.supplierSku,quantity:value(quantity,3),unitCost:value(unitCost,2),sourceText:row.text,confidence:row.confidence})
  usedRows.add(rowIndex)
 })
 rows.forEach((row,index)=>{
  if(usedRows.has(index)||!/\d/.test(row.text)||row.text.length<=4)return
  const tokens=row.text.trim().split(/\s+/),numbers=amounts(row.text)
  if(numbers.length>=2&&tokens.length>=4&&/[A-Za-z]/.test(row.text)){
   const supplierSku=tokens[0].replace(/[^A-Za-z0-9._/-]/g,'')
   const numericStart=tokens.findIndex((token,i)=>i>0&&parseAmount(token)!==undefined)
   const productName=tokens.slice(1,numericStart>1?numericStart:tokens.length-Math.min(3,numbers.length)).join(' ').trim()
   const quantity=numbers.length>=3?numbers[numbers.length-3]:numbers[0]
   const unitCost=numbers.length>=3?numbers[numbers.length-2]:numbers[1]
   if(supplierSku&&productName)unknownLines.push({supplierSku,productName,quantity:value(quantity,3),unitCost:value(unitCost,2),sourceText:row.text,confidence:row.confidence})
   else unmatchedRows.push(row.text)
  } else unmatchedRows.push(row.text)
 })
 const text=[rawText,...rows.map(row=>row.text)].join('\n')
 return{invoiceNumber:parseInvoiceNumber(text),invoiceDate:parseDate(text),lines:result,unknownLines,unmatchedRows}
}
