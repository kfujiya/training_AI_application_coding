const boardElement = document.querySelector('#board');
const statusElement = document.querySelector('#status');
const restartButton = document.querySelector('#restart');

const marks = {
  X: 'item/青いレトロ風ピクセルXアイコン.png',
  O: 'item/赤いピクセルアートの輪.png',
};

const winningLines = [
  [0, 1, 2], [3, 4, 5], [6, 7, 8],
  [0, 3, 6], [1, 4, 7], [2, 5, 8],
  [0, 4, 8], [2, 4, 6],
];

document.querySelector('#page-title').textContent = '三目並べ';

let cells = [];
let currentPlayer = 'X';
let gameOver = false;

function createBoard() {
  boardElement.replaceChildren();
  cells = Array(9).fill('');

  cells.forEach((_, index) => {
    const cell = document.createElement('button');
    cell.className = 'cell';
    cell.type = 'button';
    cell.style.setProperty('--left', `${17.4 + ((index % 3) * 22.25)}%`);
    cell.style.setProperty('--top', `${17.4 + (Math.floor(index / 3) * 22.25)}%`);
    cell.setAttribute('role', 'gridcell');
    cell.setAttribute('aria-label', `${index + 1} 番目のマス`);
    cell.addEventListener('click', () => placeMark(index, cell));
    boardElement.append(cell);
  });
}

function placeMark(index, cell) {
  if (gameOver || cells[index]) return;

  const player = currentPlayer;
  cells[index] = player;
  cell.dataset.mark = player;
  cell.disabled = true;
  cell.setAttribute('aria-label', `${index + 1} 番目のマス: ${player}`);

  const image = document.createElement('img');
  image.src = marks[player];
  image.alt = player;
  cell.append(image);

  const winner = getWinner();
  if (winner) {
    finishGame(`${winner} の勝ちです！`);
  } else if (cells.every(Boolean)) {
    finishGame('引き分けです！');
  } else {
    currentPlayer = currentPlayer === 'X' ? 'O' : 'X';
    statusElement.textContent = `${currentPlayer} のターンです`;
  }
}

function getWinner() {
  const line = winningLines.find(([a, b, c]) => cells[a] && cells[a] === cells[b] && cells[a] === cells[c]);
  return line ? cells[line[0]] : null;
}

function finishGame(message) {
  gameOver = true;
  statusElement.textContent = message;
  restartButton.hidden = false;
  boardElement.querySelectorAll('.cell').forEach((cell) => { cell.disabled = true; });
}

function restartGame() {
  currentPlayer = 'X';
  gameOver = false;
  statusElement.textContent = 'X のターンです';
  restartButton.hidden = true;
  createBoard();
}

restartButton.addEventListener('click', restartGame);
restartGame();
