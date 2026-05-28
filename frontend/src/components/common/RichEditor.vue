<template>
  <div class="rich-editor">
    <!-- Quill mounts here -->
    <div ref="editorRef" class="rich-editor__quill"></div>

    <!-- Hidden file input for image upload from PC -->
    <input
      type="file"
      ref="imageInputRef"
      accept="image/png,image/jpeg,image/jpg,image/gif,image/webp,image/bmp,image/svg+xml"
      style="display:none"
      @change="onImageFileSelected"
    />

    <!-- Image insert dialog (URL or file) -->
    <div v-if="showImageDialog" class="rich-editor__dialog-overlay" @click.self="closeImageDialog">
      <div class="rich-editor__dialog">
        <div class="rich-editor__dialog-header">
          <span>Вставить изображение</span>
          <button class="rich-editor__dialog-close" @click="closeImageDialog">✕</button>
        </div>

        <!-- Tabs -->
        <div class="rich-editor__dialog-tabs">
          <button :class="['rich-editor__tab', { active: imageTab === 'url' }]" @click="imageTab = 'url'">
            🌐 По ссылке из интернета
          </button>
          <button :class="['rich-editor__tab', { active: imageTab === 'file' }]" @click="imageTab = 'file'">
            💻 Загрузить с компьютера
          </button>
        </div>

        <!-- URL tab -->
        <div v-if="imageTab === 'url'" class="rich-editor__dialog-body">
          <p class="rich-editor__dialog-hint">Вставьте прямую ссылку на изображение</p>
          <input
            v-model="imageUrl"
            type="url"
            class="rich-editor__dialog-input"
            placeholder="https://example.com/image.png"
            @keydown.enter="insertImageByUrl"
            ref="urlInputRef"
          />
          <div v-if="imageUrl" class="rich-editor__dialog-preview">
            <img :src="imageUrl" alt="preview" @error="urlPreviewError = true" @load="urlPreviewError = false" />
            <p v-if="urlPreviewError" class="rich-editor__dialog-error">⚠ Не удалось загрузить изображение по этой ссылке</p>
          </div>
          <div class="rich-editor__dialog-actions">
            <button class="rich-editor__btn-secondary" @click="closeImageDialog">Отмена</button>
            <button class="rich-editor__btn-primary" :disabled="!imageUrl || urlPreviewError" @click="insertImageByUrl">
              Вставить
            </button>
          </div>
        </div>

        <!-- File tab -->
        <div v-if="imageTab === 'file'" class="rich-editor__dialog-body">
          <p class="rich-editor__dialog-hint">PNG, JPG, GIF, WebP, BMP, SVG</p>

          <div v-if="!uploadImageFn" class="rich-editor__dialog-warning">
            ℹ Изображение будет встроено как base64 (сначала сохраните занятие, чтобы загружать на сервер)
          </div>

          <div
            class="rich-editor__drop-zone"
            @click="imageInputRef.click()"
            @dragover.prevent
            @drop.prevent="onDropImage"
          >
            <div v-if="!uploading">
              <div style="font-size:36px;margin-bottom:8px">🖼</div>
              <div>Перетащите фото или <span style="color:#6366f1;cursor:pointer">нажмите для выбора</span></div>
            </div>
            <div v-else class="rich-editor__uploading">
              <div class="rich-editor__spinner"></div>
              Загружаю...
            </div>
          </div>

          <div v-if="uploadError" class="rich-editor__dialog-error">{{ uploadError }}</div>

          <div class="rich-editor__dialog-actions">
            <button class="rich-editor__btn-secondary" @click="closeImageDialog">Отмена</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import Quill from 'quill'
import 'quill/dist/quill.snow.css'

const props = defineProps({
  modelValue:    { type: String,   default: '' },
  uploadImageFn: { type: Function, default: null },
  placeholder:   { type: String,   default: 'Введите содержание...' },
  minHeight:     { type: String,   default: '300px' }
})
const emit = defineEmits(['update:modelValue'])

const editorRef     = ref(null)
const imageInputRef = ref(null)
const urlInputRef   = ref(null)

// Dialog state
const showImageDialog = ref(false)
const imageTab        = ref('url')
const imageUrl        = ref('')
const urlPreviewError = ref(false)
const uploading       = ref(false)
const uploadError     = ref('')

let quill         = null
let pendingRange  = null   // cursor position when image dialog was opened
let suppressEmit  = false

// ─── Quill init ──────────────────────────────────────────────────────────────
onMounted(() => {
  quill = new Quill(editorRef.value, {
    theme: 'snow',
    placeholder: props.placeholder,
    modules: {
      toolbar: {
        container: [
          [{ header: [1, 2, 3, false] }],
          ['bold', 'italic', 'underline', 'strike'],
          [{ color: [] }, { background: [] }],
          [{ align: [] }],
          [{ list: 'ordered' }, { list: 'bullet' }],
          ['link', 'image'],
          ['clean']
        ],
        handlers: { image: openImageDialog }
      }
    }
  })

  setHTML(props.modelValue || '')

  quill.on('text-change', () => {
    if (!suppressEmit) emit('update:modelValue', quill.root.innerHTML)
  })
})

onBeforeUnmount(() => { quill = null })

watch(() => props.modelValue, (val) => {
  if (!quill) return
  if (val !== quill.root.innerHTML) setHTML(val || '')
})

function setHTML(html) {
  suppressEmit = true
  quill.root.innerHTML = html
  suppressEmit = false
}

// ─── Image dialog ─────────────────────────────────────────────────────────────
function openImageDialog() {
  pendingRange  = quill.getSelection() ?? { index: quill.getLength(), length: 0 }
  imageTab.value       = 'url'
  imageUrl.value       = ''
  urlPreviewError.value = false
  uploadError.value    = ''
  showImageDialog.value = true
  nextTick(() => urlInputRef.value?.focus())
}

function closeImageDialog() {
  showImageDialog.value = false
}

function insertImageByUrl() {
  if (!imageUrl.value || urlPreviewError.value) return
  insertImage(imageUrl.value)
  closeImageDialog()
}

function insertImage(url) {
  const index = pendingRange?.index ?? quill.getLength()
  quill.insertEmbed(index, 'image', url)
  quill.setSelection(index + 1)
}

// ─── File upload ──────────────────────────────────────────────────────────────
function onImageFileSelected(e) {
  const file = e.target.files[0]
  e.target.value = ''
  if (file) processImageFile(file)
}

function onDropImage(e) {
  const file = e.dataTransfer.files[0]
  if (file && file.type.startsWith('image/')) processImageFile(file)
}

async function processImageFile(file) {
  uploading.value   = true
  uploadError.value = ''

  if (props.uploadImageFn) {
    try {
      const url = await props.uploadImageFn(file)
      insertImage(url)
      closeImageDialog()
    } catch (err) {
      uploadError.value = 'Ошибка загрузки: ' + (err?.message || 'попробуйте ещё раз')
    } finally {
      uploading.value = false
    }
  } else {
    // Fallback: base64
    const reader = new FileReader()
    reader.onload = (ev) => {
      insertImage(ev.target.result)
      closeImageDialog()
      uploading.value = false
    }
    reader.onerror = () => {
      uploadError.value = 'Не удалось прочитать файл'
      uploading.value = false
    }
    reader.readAsDataURL(file)
  }
}
</script>

<style>
/* ── Quill snow overrides ───────────────────────────────────────────────────── */
.rich-editor .ql-toolbar.ql-snow {
  border: 1px solid #e2e8f0;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  background: #f8fafc;
  padding: 8px 12px;
  flex-wrap: wrap;
}

.rich-editor .ql-container.ql-snow {
  border: 1px solid #e2e8f0;
  border-radius: 0 0 8px 8px;
  font-family: inherit;
  font-size: 15px;
}

.rich-editor .ql-editor {
  min-height: v-bind(minHeight);
  line-height: 1.75;
  color: #1e293b;
  padding: 16px 18px;
}

.rich-editor .ql-editor.ql-blank::before {
  color: #94a3b8;
  font-style: normal;
}

.rich-editor .ql-snow .ql-stroke { stroke: #475569; }
.rich-editor .ql-snow .ql-fill   { fill: #475569; }
.rich-editor .ql-snow button:hover .ql-stroke,
.rich-editor .ql-snow button.ql-active .ql-stroke { stroke: #6366f1; }
.rich-editor .ql-snow button:hover .ql-fill,
.rich-editor .ql-snow button.ql-active .ql-fill   { fill: #6366f1; }
.rich-editor .ql-snow .ql-picker-label { color: #475569; }
.rich-editor .ql-snow .ql-picker-label:hover { color: #6366f1; }

.rich-editor .ql-editor img {
  max-width: 100%;
  border-radius: 6px;
  cursor: default;
}

/* ── Image dialog ───────────────────────────────────────────────────────────── */
.rich-editor__dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.45);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rich-editor__dialog {
  background: #fff;
  border-radius: 12px;
  width: 480px;
  max-width: 95vw;
  box-shadow: 0 20px 60px rgba(0,0,0,.2);
  overflow: hidden;
}

.rich-editor__dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  font-weight: 600;
  font-size: 16px;
}

.rich-editor__dialog-close {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #94a3b8;
  line-height: 1;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background .15s;
}
.rich-editor__dialog-close:hover { background: #f1f5f9; color: #1e293b; }

.rich-editor__dialog-tabs {
  display: flex;
  border-bottom: 1px solid #e2e8f0;
}

.rich-editor__tab {
  flex: 1;
  padding: 12px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all .15s;
}
.rich-editor__tab.active { color: #6366f1; border-bottom-color: #6366f1; background: #fafafa; }
.rich-editor__tab:hover:not(.active) { background: #f8fafc; }

.rich-editor__dialog-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rich-editor__dialog-hint {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.rich-editor__dialog-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border .15s;
}
.rich-editor__dialog-input:focus { border-color: #6366f1; }

.rich-editor__dialog-preview {
  text-align: center;
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px;
  min-height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.rich-editor__dialog-preview img {
  max-width: 100%;
  max-height: 160px;
  border-radius: 6px;
}

.rich-editor__dialog-warning {
  background: #fef3c7;
  border: 1px solid #fcd34d;
  color: #92400e;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.rich-editor__dialog-error {
  color: #dc2626;
  font-size: 13px;
  text-align: center;
}

.rich-editor__drop-zone {
  border: 2px dashed #e2e8f0;
  border-radius: 10px;
  padding: 32px 20px;
  text-align: center;
  cursor: pointer;
  font-size: 14px;
  color: #64748b;
  transition: all .15s;
}
.rich-editor__drop-zone:hover { border-color: #6366f1; background: #f5f3ff; }

.rich-editor__uploading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #6366f1;
  font-weight: 500;
}

.rich-editor__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e0e7ff;
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: re-spin .7s linear infinite;
}
@keyframes re-spin { to { transform: rotate(360deg); } }

.rich-editor__dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.rich-editor__btn-primary {
  padding: 9px 20px;
  background: #6366f1;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background .15s;
}
.rich-editor__btn-primary:hover:not(:disabled) { background: #4f46e5; }
.rich-editor__btn-primary:disabled { opacity: .5; cursor: not-allowed; }

.rich-editor__btn-secondary {
  padding: 9px 20px;
  background: #f1f5f9;
  color: #475569;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background .15s;
}
.rich-editor__btn-secondary:hover { background: #e2e8f0; }
</style>
