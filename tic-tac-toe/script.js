"use strict";

const titleScreen = document.querySelector("#title-screen");
const gameScreen = document.querySelector("#game-screen");
const boardElement = document.querySelector("#board");
const statusElement = document.querySelector("#status");
const backButton = document.querySelector("#back-button");
const modeButtons = document.querySelectorAll("[data-mode]");

const winningLines = [
  [0, 1, 2], [3, 4, 5], [6, 7, 8],
  [0, 3, 6], [1, 4, 7], [2, 5, 8],
  [0, 4, 8], [2, 4, 6],
];

let board = Array(9).fill("");
let currentPlayer = "○";
let gameMode = "player";
let gameOver = false;
let cpuThinking = false;

modeButtons.forEach((button) => {
  button.addEventListener("click", () => startGame(button.dataset.mode));
});
backButton.addEventListener("click", showTitle);

function startGame(mode) {
  gameMode = mode;
  board = Array(9).fill("");
  currentPlayer = "○";
  gameOver = false;
  cpuThinking = false;
  backButton.hidden = true;
  boardElement.replaceChildren();

  board.forEach((_, index) => {
    const cell = document.createElement("button");
    cell.type = "button";
    cell.className = "cell";
    cell.dataset.index = index;
    cell.setAttribute("role", "gridcell");
    cell.setAttribute("aria-label", `${index + 1}番のマス、空き`);
    cell.addEventListener("click", handleCellClick);
    boardElement.appendChild(cell);
  });

  titleScreen.hidden = true;
  gameScreen.hidden = false;
  updateStatus();
}

function handleCellClick(event) {
  const index = Number(event.currentTarget.dataset.index);
  if (gameOver || cpuThinking || board[index] !== "") return;

  placeMark(index, currentPlayer);
  if (finishTurn()) return;

  currentPlayer = currentPlayer === "○" ? "×" : "○";
  updateStatus();

  if (gameMode === "cpu" && currentPlayer === "×") {
    cpuThinking = true;
    statusElement.textContent = "CPUが考えています…";
    window.setTimeout(playCpuTurn, 450);
  }
}

function placeMark(index, mark) {
  board[index] = mark;
  const cell = boardElement.children[index];
  cell.textContent = mark;
  cell.classList.add(mark === "○" ? "circle" : "cross");
  cell.setAttribute("aria-label", `${index + 1}番のマス、${mark}`);
  cell.disabled = true;
}

function finishTurn() {
  const winningLine = findWinningLine(currentPlayer);
  if (winningLine) {
    endGame(`${currentPlayer} の勝ち！`, winningLine);
    return true;
  }
  if (board.every(Boolean)) {
    endGame("引き分けです");
    return true;
  }
  return false;
}

function findWinningLine(mark, state = board) {
  return winningLines.find((line) => line.every((index) => state[index] === mark));
}

function playCpuTurn() {
  if (gameOver) return;
  const index = chooseCpuMove();
  placeMark(index, "×");
  cpuThinking = false;

  if (finishTurn()) return;
  currentPlayer = "○";
  updateStatus();
}

function chooseCpuMove() {
  const emptyCells = board
    .map((value, index) => value === "" ? index : -1)
    .filter((index) => index !== -1);

  const winningMove = findMoveThatCompletesLine("×", emptyCells);
  if (winningMove !== null) return winningMove;

  const blockingMove = findMoveThatCompletesLine("○", emptyCells);
  if (blockingMove !== null) return blockingMove;

  if (board[4] === "") return 4;

  const openCorners = [0, 2, 6, 8].filter((index) => board[index] === "");
  if (openCorners.length) return randomItem(openCorners);
  return randomItem(emptyCells);
}

function findMoveThatCompletesLine(mark, emptyCells) {
  for (const index of emptyCells) {
    const testBoard = [...board];
    testBoard[index] = mark;
    if (findWinningLine(mark, testBoard)) return index;
  }
  return null;
}

function randomItem(items) {
  return items[Math.floor(Math.random() * items.length)];
}

function updateStatus() {
  const playerName = gameMode === "cpu" && currentPlayer === "×" ? "CPU" : `プレイヤー ${currentPlayer}`;
  statusElement.textContent = `${playerName} のターン`;
}

function endGame(message, winningLine = []) {
  gameOver = true;
  cpuThinking = false;
  statusElement.textContent = message;
  winningLine.forEach((index) => boardElement.children[index].classList.add("winner"));
  [...boardElement.children].forEach((cell) => { cell.disabled = true; });
  backButton.hidden = false;
}

function showTitle() {
  gameOver = true;
  cpuThinking = false;
  gameScreen.hidden = true;
  titleScreen.hidden = false;
}
