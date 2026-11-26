import { useEffect, useState } from 'react'
import { getCategories, startQuiz, submitAnswer } from './services/api'
import type { AnswerResponse, Category, Difficulty, Question, QuizResult } from './types/quiz'

type Screen = 'home' | 'settings' | 'quiz' | 'result'

export function App() {
  const [screen, setScreen] = useState<Screen>('home')
  const [categories, setCategories] = useState<Category[]>([])
  const [amount, setAmount] = useState(5)
  const [category, setCategory] = useState<number | null>(null)
  const [difficulty, setDifficulty] = useState<Difficulty>('')
  const [quizId, setQuizId] = useState('')
  const [question, setQuestion] = useState<Question | null>(null)
  const [totalQuestions, setTotalQuestions] = useState(0)
  const [selectedChoiceId, setSelectedChoiceId] = useState<string | null>(null)
  const [answer, setAnswer] = useState<AnswerResponse | null>(null)
  const [result, setResult] = useState<QuizResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (screen !== 'settings' || categories.length > 0) return
    setLoading(true)
    getCategories().then(setCategories).catch((e: Error) => setError(e.message)).finally(() => setLoading(false))
  }, [screen, categories.length])

  const openSettings = () => { setError(null); setScreen('settings') }
  const start = async () => {
    setLoading(true); setError(null)
    try {
      const response = await startQuiz(amount, category, difficulty)
      setQuizId(response.quizId); setTotalQuestions(response.totalQuestions); setQuestion(response.question)
      setSelectedChoiceId(null); setAnswer(null); setScreen('quiz')
    } catch (e) { setError((e as Error).message) } finally { setLoading(false) }
  }
  const choose = async (choiceId: string) => {
    if (!question || answer || loading) return
    setSelectedChoiceId(choiceId); setLoading(true); setError(null)
    try {
      const response = await submitAnswer(quizId, question.id, choiceId)
      setAnswer(response)
      if (response.completed && response.result) setResult(response.result)
    } catch (e) { setSelectedChoiceId(null); setError((e as Error).message) } finally { setLoading(false) }
  }
  const next = () => {
    if (!answer) return
    if (answer.completed) setScreen('result')
    else { setQuestion(answer.nextQuestion); setSelectedChoiceId(null); setAnswer(null) }
  }

  return <main className="shell">
    <header className="topbar"><button className="brand" onClick={() => setScreen('home')}>QUIZ <span>ARENA</span></button><span className="phase-label">PHASE 2</span></header>
    {screen === 'home' && <Home onStart={openSettings} />}
    {screen === 'settings' && <Settings amount={amount} setAmount={setAmount} category={category} setCategory={setCategory} difficulty={difficulty} setDifficulty={setDifficulty} categories={categories} loading={loading} error={error} onStart={start} onRetry={() => { setCategories([]); setError(null) }} />}
    {screen === 'quiz' && question && <Quiz question={question} totalQuestions={totalQuestions} selectedChoiceId={selectedChoiceId} answer={answer} loading={loading} error={error} onChoose={choose} onNext={next} />}
    {screen === 'result' && result && <Result result={result} onReplay={openSettings} onHome={() => setScreen('home')} />}
    <footer>Trivia questions provided by Open Trivia DB. Licensed under CC BY-SA 4.0.</footer>
  </main>
}

function Home({ onStart }: { onStart: () => void }) { return <section className="hero"><p className="eyebrow">KNOWLEDGE IS YOUR WEAPON</p><h1>QUIZ <span>ARENA</span></h1><p className="lead">世界のトリビアに挑戦して、知識の限界を試そう。</p><button className="primary-button" onClick={onStart}>ゲームを開始する <b>→</b></button></section> }

type SettingsProps = { amount: number; setAmount: (v: number) => void; category: number | null; setCategory: (v: number | null) => void; difficulty: Difficulty; setDifficulty: (v: Difficulty) => void; categories: Category[]; loading: boolean; error: string | null; onStart: () => void; onRetry: () => void }
function Settings(p: SettingsProps) { return <section className="panel"><div className="section-heading"><p className="eyebrow">CONFIGURE YOUR RUN</p><h2>クイズ設定</h2><p>挑戦したい条件を選択してください。</p></div>{p.error && <ErrorBox message={p.error} onRetry={p.onRetry} />}
  <label>問題数<div className="choice-row">{[5, 10, 20].map(n => <button type="button" key={n} className={p.amount === n ? 'choice-pill active' : 'choice-pill'} onClick={() => p.setAmount(n)}>{n}問</button>)}</div></label>
  <label>カテゴリ<select value={p.category ?? ''} onChange={e => p.setCategory(e.target.value ? Number(e.target.value) : null)} disabled={p.loading}><option value="">全カテゴリ</option>{p.categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
  <label>難易度<select value={p.difficulty} onChange={e => p.setDifficulty(e.target.value as Difficulty)}><option value="">指定なし</option><option value="easy">EASY</option><option value="medium">MEDIUM</option><option value="hard">HARD</option></select></label>
  <button className="primary-button full" onClick={p.onStart} disabled={p.loading}>{p.loading ? '読み込み中…' : 'クイズを開始する'} <b>→</b></button>
</section> }

type QuizProps = { question: Question; totalQuestions: number; selectedChoiceId: string | null; answer: AnswerResponse | null; loading: boolean; error: string | null; onChoose: (id: string) => void; onNext: () => void }
function Quiz(p: QuizProps) { const progress = p.question.number / p.totalQuestions * 100; return <section className="panel quiz-panel"><div className="quiz-meta"><span>QUESTION {String(p.question.number).padStart(2, '0')} / {p.totalQuestions}</span><span>{Math.round(progress)}%</span></div><div className="progress"><i style={{ width: `${progress}%` }} /></div><div className="tags"><span>{p.question.category}</span><span>{p.question.difficulty}</span></div><h2 className="question">{p.question.question}</h2>{p.error && <ErrorBox message={p.error} />}
  <div className="answers">{p.question.choices.map((choice, i) => { const selected = p.selectedChoiceId === choice.id; const correct = p.answer?.correctChoice.id === choice.id; return <button type="button" key={choice.id} className={`answer ${selected ? 'selected' : ''} ${p.answer && correct ? 'correct' : ''} ${p.answer && selected && !p.answer.correct ? 'incorrect' : ''}`} disabled={!!p.answer || p.loading} onClick={() => p.onChoose(choice.id)}><b>{String.fromCharCode(65 + i)}</b><span>{choice.text}</span>{p.answer && correct && <em>正解</em>}</button> })}</div>
  {p.answer && <div className={`feedback ${p.answer.correct ? 'success' : 'failure'}`}><strong>{p.answer.correct ? '正解！' : '不正解'}</strong><span>{p.answer.correct ? '見事な回答です。' : `正解は「${p.answer.correctChoice.text}」でした。`}</span></div>}
  {p.answer && <button className="primary-button full" onClick={p.onNext}>{p.answer.completed ? '結果を見る' : '次の問題へ'} <b>→</b></button>}
</section> }

function Result({ result, onReplay, onHome }: { result: QuizResult; onReplay: () => void; onHome: () => void }) { return <section className="panel result-panel"><p className="eyebrow">RUN COMPLETE</p><h2>クイズ結果</h2><div className="score">{result.accuracyPercent}<small>%</small></div><p className="result-label">正答率</p><div className="result-grid"><div><strong>{result.correctCount}</strong><span>正解</span></div><div><strong>{result.incorrectCount}</strong><span>不正解</span></div><div><strong>{result.totalQuestions}</strong><span>総問題数</span></div></div><div className="button-row"><button className="primary-button" onClick={onReplay}>もう一度プレイ</button><button className="secondary-button" onClick={onHome}>ホームへ戻る</button></div></section> }

function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) { return <div className="error-box"><span>{message}</span>{onRetry && <button type="button" onClick={onRetry}>再試行</button>}</div> }
