const hands = {
  rock: { label: "グー", emoji: "✊", beats: "scissors" },
  scissors: { label: "チョキ", emoji: "✌️", beats: "paper" },
  paper: { label: "パー", emoji: "🖐️", beats: "rock" },
};

const score = { wins: 0, losses: 0, draws: 0, currentStreak: 0, bestStreak: 0 };
const handKeys = Object.keys(hands);
const elements = {
  player: document.querySelector("#player-hand"),
  computer: document.querySelector("#computer-hand"),
  result: document.querySelector("#result"),
  wins: document.querySelector("#wins"),
  losses: document.querySelector("#losses"),
  draws: document.querySelector("#draws"),
  bestStreak: document.querySelector("#best-streak"),
};

function renderScore() {
  elements.wins.textContent = score.wins;
  elements.losses.textContent = score.losses;
  elements.draws.textContent = score.draws;
  elements.bestStreak.textContent = score.bestStreak;
}

function play(playerKey) {
  const computerKey = handKeys[Math.floor(Math.random() * handKeys.length)];
  elements.player.textContent = hands[playerKey].emoji;
  elements.computer.textContent = hands[computerKey].emoji;

  if (playerKey === computerKey) {
    score.draws += 1;
    score.currentStreak = 0;
    elements.result.textContent = "あいこです！もう一度！";
  } else if (hands[playerKey].beats === computerKey) {
    score.wins += 1;
    score.currentStreak += 1;
    score.bestStreak = Math.max(score.bestStreak, score.currentStreak);
    elements.result.textContent = "あなたの勝ち！";
  } else {
    score.losses += 1;
    score.currentStreak = 0;
    elements.result.textContent = "コンピュータの勝ち…";
  }
  renderScore();
}

document.querySelectorAll("[data-hand]").forEach((button) => {
  button.addEventListener("click", () => play(button.dataset.hand));
});

document.querySelector("#reset").addEventListener("click", () => {
  Object.assign(score, { wins: 0, losses: 0, draws: 0, currentStreak: 0, bestStreak: 0 });
  elements.player.textContent = "？";
  elements.computer.textContent = "？";
  elements.result.textContent = "手を選んでください";
  renderScore();
});
