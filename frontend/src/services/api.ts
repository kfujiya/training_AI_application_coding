import type { AnswerResponse, Category, Difficulty, QuizStartResponse } from '../types/quiz'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...init })
  const body = await response.json().catch(() => ({})) as { message?: string }
  if (!response.ok) throw new Error(body.message ?? '通信に失敗しました。')
  return body as T
}

export function getCategories() { return request<Category[]>('/api/categories') }
export function startQuiz(amount: number, category: number | null, difficulty: Difficulty) { return request<QuizStartResponse>('/api/quizzes', { method: 'POST', body: JSON.stringify({ amount, category, difficulty: difficulty || null }) }) }
export function submitAnswer(quizId: string, questionId: string, selectedChoiceId: string) { return request<AnswerResponse>(`/api/quizzes/${quizId}/answers`, { method: 'POST', body: JSON.stringify({ questionId, selectedChoiceId }) }) }
