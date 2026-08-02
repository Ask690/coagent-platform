// ============ CoAgent 前端 API 客户端 ============
const BASE = '/api'

// ---------- 会话 ----------
export async function getSessions() {
  return (await fetch(`${BASE}/sessions`)).json()
}

export async function createSession() {
  const res = await fetch(`${BASE}/sessions`, { method: 'POST' })
  return res.json()
}

export async function getMessages(sessionId) {
  return (await fetch(`${BASE}/sessions/${sessionId}/messages`)).json()
}

// ---------- 工单 ----------
export async function getTickets() {
  return (await fetch(`${BASE}/tickets`)).json()
}

// ---------- 知识库 ----------
export async function getDocuments() {
  return (await fetch(`${BASE}/documents`)).json()
}

export async function uploadDocument(file) {
  const fd = new FormData()
  fd.append('file', file)
  return fetch(`${BASE}/documents/upload`, { method: 'POST', body: fd })
}

export async function deleteDocument(id) {
  return fetch(`${BASE}/documents/${id}`, { method: 'DELETE' })
}

/**
 * SSE 流式对话：解析 text/event-stream，逐事件回调。
 * @param {string} sessionId
 * @param {string} message
 * @param {(ev: object) => void} onEvent  事件回调 {type, data}
 * @param {(err: Error) => void} onError  异常回调
 */
export async function chatStream(sessionId, message, onEvent, onError) {
  try {
    const res = await fetch(`${BASE}/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, message }),
    })
    if (!res.ok || !res.body) {
      throw new Error(`请求失败 HTTP ${res.status}`)
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let sep
      // SSE 事件以空行分隔
      while ((sep = buffer.indexOf('\n\n')) >= 0) {
        const block = buffer.slice(0, sep)
        buffer = buffer.slice(sep + 2)
        const line = block
          .split('\n')
          .find((l) => l.startsWith('data:'))
        if (line) {
          try {
            onEvent(JSON.parse(line.slice(5).trim()))
          } catch {
            /* 忽略无法解析的事件 */
          }
        }
      }
    }
  } catch (err) {
    if (onError) onError(err)
  }
}
