export type Theme='light'|'dark'

export const THEME_STORAGE_KEY='barstock-theme'

export function resolveTheme(stored:string|null,prefersDark:boolean):Theme{
 return stored==='light'||stored==='dark'?stored:prefersDark?'dark':'light'
}

export function oppositeTheme(theme:Theme):Theme{return theme==='dark'?'light':'dark'}
