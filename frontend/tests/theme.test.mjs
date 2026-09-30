import assert from 'node:assert/strict'
import test from 'node:test'
import {oppositeTheme,resolveTheme} from '../src/theme.ts'

test('uses a saved theme before the system preference',()=>{
 assert.equal(resolveTheme('light',true),'light')
 assert.equal(resolveTheme('dark',false),'dark')
})

test('uses the system preference when no valid theme is saved',()=>{
 assert.equal(resolveTheme(null,true),'dark')
 assert.equal(resolveTheme('unknown',false),'light')
})

test('toggles between light and dark themes',()=>{
 assert.equal(oppositeTheme('light'),'dark')
 assert.equal(oppositeTheme('dark'),'light')
})
