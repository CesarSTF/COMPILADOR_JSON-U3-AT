<script setup lang="ts">
import { ref, shallowRef } from 'vue'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'

const MONACO_EDITOR_OPTIONS = {
  automaticLayout: true,
  formatOnType: true,
  formatOnPaste: true,
  minimap: { enabled: false },
  fontSize: 14,
  fontFamily: "'JetBrains Mono', 'Fira Code', monospace",
  scrollBeyondLastLine: false,
}

const code = ref(`{
  "host": "localhost",
  "puerto": 8080,
  "modo": "desarrollo",
  "debug": true
}`)

const isLoading = ref(false)
const result = ref<any>(null)
const rawHtmlResponse = ref<string>("")

const examples = {
  jsonCorrect: `{\n  "host": "localhost",\n  "puerto": 8080,\n  "modo": "produccion",\n  "debug": false\n}`,
  jsonError: `{\n  "host": "localhost",\n  "puerto": 999999,\n  "modo": "produccion",\n  "debug": true\n}`
}

const loadExample = (key: keyof typeof examples) => {
  code.value = examples[key]
}

const validateCode = async () => {
  isLoading.value = true
  result.value = null
  rawHtmlResponse.value = ""
  try {
    const res = await fetch('http://localhost:8001/analizar', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: code.value,
    })
    
    // La API podría devolver errores HTML (ej. 500 Interno)
    const contentType = res.headers.get("content-type");
    if (contentType && contentType.includes("text/html")) {
        rawHtmlResponse.value = await res.text()
    } else {
        const data = await res.json()
        result.value = { status: res.status, data }
    }
  } catch (error: any) {
    result.value = { 
      status: 0, 
      data: { error: "No se pudo conectar con el servidor API." } 
    }
  } finally {
    isLoading.value = false
  }
}

const editorMounted = (editor: any) => {
  // Opcional: enfocar el editor al cargar
  editor.focus()
}
</script>

<template>
  <div class="min-h-screen bg-slate-900 text-slate-100 flex flex-col font-sans">
    
    <!-- Navbar -->
    <header class="bg-slate-800 border-b border-slate-700 p-4 shadow-sm">
      <div class="max-w-7xl mx-auto flex items-center justify-between">
        <div class="flex items-center space-x-3">
          <div class="bg-blue-500 p-2 rounded-lg">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"></path></svg>
          </div>
          <div>
            <h1 class="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-indigo-400">
              Distributed DSL Compiler
            </h1>
            <p class="text-xs text-slate-400">Powered by FastAPI & Qwen2.5</p>
          </div>
        </div>
        
        <button 
          @click="validateCode" 
          :disabled="isLoading"
          class="flex items-center px-6 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-semibold rounded-lg shadow-lg shadow-blue-500/30 transition-all disabled:opacity-50 disabled:cursor-not-allowed transform active:scale-95"
        >
          <svg v-if="isLoading" class="animate-spin -ml-1 mr-2 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <svg v-else class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          {{ isLoading ? 'Validando...' : 'Compilar & Validar' }}
        </button>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 flex flex-col md:flex-row gap-6 p-6 max-w-7xl mx-auto w-full">
      
      <!-- Left Column: Editor -->
      <section class="flex-1 flex flex-col space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-semibold text-slate-300 uppercase tracking-wider flex items-center">
            <svg class="w-4 h-4 mr-2 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
            Archivo de Configuración
          </h2>
          <div class="flex space-x-2 text-xs">
            <button @click="loadExample('jsonCorrect')" class="px-2 py-1 bg-slate-700 hover:bg-slate-600 rounded text-slate-300 transition-colors">JSON ✓</button>
            <button @click="loadExample('jsonError')" class="px-2 py-1 bg-slate-700 hover:bg-slate-600 rounded text-rose-300 transition-colors">JSON ✗</button>
          </div>
        </div>
        <div class="flex-1 min-h-[400px] border border-slate-700 rounded-xl overflow-hidden shadow-2xl relative">
          <div class="absolute inset-0 bg-[#1e1e1e]">
            <vue-monaco-editor
              v-model:value="code"
              theme="vs-dark"
              language="json"
              :options="MONACO_EDITOR_OPTIONS"
              @mount="editorMounted"
            />
          </div>
        </div>
      </section>

      <!-- Right Column: Output -->
      <section class="flex-1 flex flex-col space-y-3">
        <h2 class="text-sm font-semibold text-slate-300 uppercase tracking-wider flex items-center">
          <svg class="w-4 h-4 mr-2 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          Consola del Compilador
        </h2>
        <div class="flex-1 bg-slate-800 border border-slate-700 rounded-xl p-5 overflow-auto shadow-xl">
          
          <!-- Estado Inicial -->
          <div v-if="!result && !isLoading && !rawHtmlResponse" class="h-full flex flex-col items-center justify-center text-slate-500">
            <svg class="w-16 h-16 mb-4 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
            <p>Esperando ejecución...</p>
          </div>

          <!-- HTML Error Fallback (Ej: API 500) -->
          <div v-else-if="rawHtmlResponse" class="text-red-400 bg-red-900/20 p-4 rounded-lg border border-red-800">
             <h3 class="font-bold text-lg mb-2 flex items-center">
               <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
               Error del Servidor API (HTML)
             </h3>
             <p class="text-sm">El servidor no devolvió JSON. Probablemente la API devolvió un 500 Internal Server Error.</p>
          </div>

          <!-- Loading -->
          <div v-else-if="isLoading" class="space-y-4">
             <div class="flex items-center space-x-3 text-slate-400">
                <svg class="animate-spin h-5 w-5 text-indigo-500" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span class="font-mono text-sm">Analizando sintaxis y validando reglas de negocio...</span>
             </div>
             <div class="h-2 bg-slate-700 rounded-full overflow-hidden">
                <div class="h-full bg-indigo-500 w-1/2 animate-pulse"></div>
             </div>
          </div>

          <!-- Resultado -->
          <div v-else-if="result" class="space-y-6 animate-fade-in">
            
            <!-- EXITO -->
            <div v-if="result.status === 200" class="bg-emerald-900/20 border border-emerald-800/50 rounded-xl p-5">
              <div class="flex items-center space-x-3 mb-4">
                <div class="bg-emerald-500/20 p-2 rounded-full">
                  <svg class="w-6 h-6 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                </div>
                <h3 class="text-emerald-400 text-lg font-bold">¡Configuración Válida!</h3>
              </div>
              <p class="text-emerald-200/80 mb-4">{{ result.data.mensaje }}</p>
              
              <div class="bg-slate-900/50 rounded-lg p-4 font-mono text-sm text-slate-300">
                <pre class="whitespace-pre-wrap">{{ JSON.stringify(result.data.configuracion_aprobada, null, 2) }}</pre>
              </div>

              <!-- Raw Técnico AST (Agregado para ver el arbol) -->
              <details class="group bg-slate-800 border border-slate-700 rounded-xl mt-4">
                <summary class="cursor-pointer p-4 text-slate-400 font-medium text-sm flex items-center justify-between">
                  <span>Ver Detalle del Árbol (AST)</span>
                  <svg class="w-5 h-5 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                </summary>
                <div class="p-4 border-t border-slate-700 bg-slate-900/50 font-mono text-xs text-slate-500 overflow-x-auto rounded-b-xl max-h-96">
                  <pre>{{ JSON.stringify(result.data.ast, null, 2) }}</pre>
                </div>
              </details>
            </div>

            <!-- ERROR 422 -->
            <div v-else-if="result.status === 422" class="space-y-4">
              <!-- Header de Error -->
              <div class="bg-rose-900/20 border border-rose-800/50 rounded-xl p-5 relative overflow-hidden">
                <div class="absolute top-0 left-0 w-1 h-full bg-rose-500"></div>
                <div class="flex justify-between items-start">
                  <div class="flex items-center space-x-3">
                    <div class="bg-rose-500/20 p-2 rounded-full">
                      <svg class="w-6 h-6 text-rose-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
                    </div>
                    <div>
                      <h3 class="text-rose-400 text-lg font-bold">Error {{ result.data.fase_fallo }}</h3>
                      <p class="text-rose-300/70 text-sm">El analizador estricto rechazó la configuración.</p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Tarjeta IA -->
              <div class="bg-indigo-900/20 border border-indigo-800/50 rounded-xl p-5 relative">
                <div class="flex items-center space-x-2 mb-3">
                  <svg class="w-5 h-5 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
                  <span class="text-indigo-400 font-bold uppercase tracking-wider text-xs">Diagnóstico Asistido por IA</span>
                </div>
                
                <p class="text-indigo-100 text-base leading-relaxed mb-4">
                  {{ result.data.diagnostico_ia?.reason || 'No se pudo generar un diagnóstico amigable.' }}
                </p>

                <div v-if="result.data.diagnostico_ia?.solution_example" class="mt-4 border-t border-indigo-800/50 pt-4">
                  <span class="block text-xs text-indigo-300 font-semibold mb-2">SOLUCIÓN SUGERIDA:</span>
                  <div class="bg-slate-900/80 p-3 rounded-lg border border-slate-700/50 font-mono text-sm text-green-400 shadow-inner">
                    <pre class="whitespace-pre-wrap">{{ result.data.diagnostico_ia.solution_example }}</pre>
                  </div>
                </div>
              </div>

              <!-- Raw Técnico -->
              <details class="group bg-slate-800 border border-slate-700 rounded-xl">
                <summary class="cursor-pointer p-4 text-slate-400 font-medium text-sm flex items-center justify-between">
                  <span>Ver Detalle Técnico Interno</span>
                  <svg class="w-5 h-5 transition-transform group-open:rotate-180" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                </summary>
                <div class="p-4 border-t border-slate-700 bg-slate-900/50 font-mono text-xs text-slate-500 overflow-x-auto rounded-b-xl">
                  <pre>{{ JSON.stringify(result.data.detalle_tecnico, null, 2) }}</pre>
                </div>
              </details>
            </div>

            <!-- CUALQUIER OTRO ERROR -->
            <div v-else class="bg-red-900/20 border border-red-800/50 rounded-xl p-5">
              <h3 class="text-red-400 font-bold mb-2">Error Crítico</h3>
              <p class="text-red-200/80 mb-4">{{ result.data?.error || 'Excepción no controlada' }}</p>
              <pre class="bg-black/50 p-3 rounded text-xs text-slate-400 font-mono overflow-auto">{{ JSON.stringify(result.data, null, 2) }}</pre>
            </div>
            
          </div>

        </div>
      </section>
    </main>

  </div>
</template>

<style>
.animate-fade-in {
  animation: fadeIn 0.3s ease-out forwards;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
