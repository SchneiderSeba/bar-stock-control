import {readFileSync,writeFileSync} from 'node:fs'
import {resolve} from 'node:path'

const file=resolve('node_modules/runonweb/src/ocr/index.ts')
const replacements=new Map([
 ['https://huggingface.co/PaddlePaddle/PP-OCRv6_small_det_onnx/resolve/main/inference.onnx','/ocr-models/ppocrv6-small-det.onnx'],
 ['https://huggingface.co/PaddlePaddle/PP-OCRv6_small_rec_onnx/resolve/main/inference.onnx','/ocr-models/ppocrv6-small-rec.onnx'],
 ['https://huggingface.co/PaddlePaddle/PP-OCRv6_small_rec_onnx/resolve/main/inference.yml','/ocr-models/ppocrv6-small-rec.yml'],
])

let source=readFileSync(file,'utf8')
for(const [remote,local] of replacements){
 if(source.includes(local))continue
 if(!source.includes(remote))throw new Error(`runonweb 0.0.1 changed: missing ${remote}`)
 source=source.replaceAll(remote,local)
}
writeFileSync(file,source)
console.log('runonweb OCR configured to use same-origin model files')
