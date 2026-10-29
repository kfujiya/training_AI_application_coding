const boardElement = document.querySelector('#board');
const statusElement = document.querySelector('#status');
const restartButton = document.querySelector('#restart');

const winningLines = [
  [0, 1, 2], [3, 4, 5], [6, 7, 8],
  [0, 3, 6], [1, 4, 7], [2, 5, 8],
  [0, 4, 8], [2, 4, 6],
];

let cells = [];
let currentPlayer = '○';
let gameOver = false;

function createBoard() {
  boardElement.replaceChildren();
  cells = Array(9).fill('');

  cells.forEach((_, index) => {
    const cell = document.createElement('button');
    cell.className = 'cell';
    cell.type = 'button';
    cell.setAttribute('role', 'gridcell');
    cell.setAttribute('aria-label', `${index + 1}番目のマス`);
    cell.addEventListener('click', () => placeMark(index, cell));
    boardElement.append(cell);
  });
}

function placeMark(index, cell) {
  if (gameOver || cells[index]) return;

  cells[index] = currentPlayer;
  cell.textContent = currentPlayer;
  cell.dataset.mark = currentPlayer;
  cell.disabled = true;
  cell.setAttribute('aria-label', `${index + 1}番目のマス: ${currentPlayer}`);

  const winner = getWinner();
  if (winner) {
    finishGame(`${winner} の勝ちです！`);
  } else if (cells.every(Boolean)) {
    finishGame('引き分けです！');
  } else {
    currentPlayer = currentPlayer === '○' ? '×' : '○';
    statusElement.textContent = `${currentPlayer} のターンです`;
  }
}

function getWinner() {
  return winningLines.find(([a, b, c]) => cells[a] && cells[a] === cells[b] && cells[a] === cells[c])
    ? cells[winningLines.find(([a, b, c]) => cells[a] && cells[a] === cells[b] && cells[a] === cells[c])[0]]
    : null;
}

function finishGame(message) {
  gameOver = true;
  statusElement.textContent = message;
  restartButton.hidden = false;
  boardElement.querySelectorAll('.cell').forEach((cell) => { cell.disabled = true; });
}

function restartGame() {
  currentPlayer = '○';
  gameOver = false;
  statusElement.textContent = '○ のターンです';
  restartButton.hidden = true;
  createBoard();
}

restartButton.addEventListener('click', restartGame);
restartGame();
