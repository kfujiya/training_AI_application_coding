export type Difficulty = '' | 'easy' | 'medium' | 'hard'
export type Category = { id: number; name: string }
export type Choice = { id: string; text: string }
export type Question = { id: string; number: number; category: string; difficulty: string; question: string; choices: Choice[] }
export type QuizStartResponse = { quizId: string; totalQuestions: number; question: Question }
export type QuizResult = { totalQuestions: number; correctCount: number; incorrectCount: number; accuracyPercent: number }
export type AnswerResponse = { correct: boolean; correctChoice: Choice; correctCount: number; incorrectCount: number; completed: boolean; nextQuestion: Question | null; result: QuizResult | null }
