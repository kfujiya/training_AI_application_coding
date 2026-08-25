const canvas = document.getElementById("gameCanvas");
const ctx = canvas.getContext("2d");

const canvasWidth = canvas.width;
const canvasHeight = canvas.height;
const groundHeight = 60;
const groundY = canvasHeight - groundHeight;

let animationFrameId = null;
let lastTime = 0;
let gameStarted = false;
let gameOver = false;
let score = 0;
let obstacleSpawnTimer = 0;
let nextObstacleSpawnTime = 0;
let obstacleSpeed = 260;

const player = {
  x: 110,
  y: 0,
  width: 40,
  height: 56,
  velocityY: 0,
  jumpPower: 560,
  gravity: 1600,
  onGround: true,
};

let obstacles = [];

player.y = groundY - player.height;

function startGame() {
  resetGame();
  gameStarted = true;
  gameOver = false;
  lastTime = performance.now();
  animationFrameId = requestAnimationFrame(gameLoop);
}

function resetGame() {
  score = 0;
  obstacleSpawnTimer = 0;
  nextObstacleSpawnTime = getRandomSpawnTime();
  obstacleSpeed = 260;
  obstacles = [];
  player.y = groundY - player.height;
  player.velocityY = 0;
  player.onGround = true;
}

function getRandomSpawnTime() {
  return 0.9 + Math.random() * 1.1;
}

function gameLoop(currentTime) {
  const deltaTime = (currentTime - lastTime) / 1000;
  lastTime = currentTime;

  update(deltaTime);
  draw();

  if (!gameOver) {
    animationFrameId = requestAnimationFrame(gameLoop);
  }
}

function update(deltaTime) {
  score += deltaTime;
  obstacleSpawnTimer += deltaTime;

  obstacleSpeed += 18 * deltaTime;

  updatePlayer(deltaTime);
  updateObstacles(deltaTime);
  spawnObstacleIfNeeded();
  checkCollision();
}

function updatePlayer(deltaTime) {
  player.velocityY += player.gravity * deltaTime;
  player.y += player.velocityY * deltaTime;

  if (player.y >= groundY - player.height) {
    player.y = groundY - player.height;
    player.velocityY = 0;
    player.onGround = true;
  } else {
    player.onGround = false;
  }
}

function updateObstacles(deltaTime) {
  for (let i = obstacles.length - 1; i >= 0; i--) {
    obstacles[i].x -= obstacleSpeed * deltaTime;

    if (obstacles[i].x + obstacles[i].width < 0) {
      obstacles.splice(i, 1);
    }
  }
}

function spawnObstacleIfNeeded() {
  if (obstacleSpawnTimer >= nextObstacleSpawnTime) {
    obstacleSpawnTimer = 0;
    nextObstacleSpawnTime = getRandomSpawnTime();

    const height = 30 + Math.random() * 40;
    obstacles.push({
      x: canvasWidth + 20,
      y: groundY - height,
      width: 28 + Math.random() * 12,
      height,
    });
  }
}

function checkCollision() {
  for (const obstacle of obstacles) {
    if (
      player.x < obstacle.x + obstacle.width &&
      player.x + player.width > obstacle.x &&
      player.y < obstacle.y + obstacle.height &&
      player.y + player.height > obstacle.y
    ) {
      endGame();
      break;
    }
  }
}

function endGame() {
  gameOver = true;
  cancelAnimationFrame(animationFrameId);
  draw();
}

function draw() {
  drawBackground();
  drawGround();
  drawPlayer();
  drawObstacles();
  drawScore();

  if (gameOver) {
    drawGameOverMessage();
  }
}

function drawBackground() {
  const gradient = ctx.createLinearGradient(0, 0, 0, canvasHeight);
  gradient.addColorStop(0, "#8fd3ff");
  gradient.addColorStop(0.72, "#dff5ff");
  gradient.addColorStop(1, "#d8f0c6");
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, canvasWidth, canvasHeight);

  ctx.fillStyle = "rgba(255, 255, 255, 0.7)";
  ctx.beginPath();
  ctx.arc(680, 70, 28, 0, Math.PI * 2);
  ctx.arc(710, 70, 36, 0, Math.PI * 2);
  ctx.arc(740, 70, 26, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = "#7fd37c";
  ctx.fillRect(0, groundY, canvasWidth, groundHeight);
}

function drawGround() {
  ctx.fillStyle = "#5aaa52";
  ctx.fillRect(0, groundY, canvasWidth, 10);

  ctx.strokeStyle = "rgba(66, 120, 64, 0.25)";
  ctx.lineWidth = 2;
  for (let x = 0; x < canvasWidth; x += 24) {
    ctx.beginPath();
    ctx.moveTo(x, groundY + 18);
    ctx.lineTo(x + 12, groundY + 18);
    ctx.stroke();
  }
}

function drawPlayer() {
  ctx.fillStyle = "#ff7a59";
  ctx.fillRect(player.x, player.y, player.width, player.height);

  ctx.fillStyle = "#ffffff";
  ctx.fillRect(player.x + 8, player.y + 10, 8, 8);
  ctx.fillRect(player.x + 24, player.y + 10, 8, 8);

  ctx.fillStyle = "#1c2430";
  ctx.fillRect(player.x + 12, player.y + 28, 16, 6);
}

function drawObstacles() {
  for (const obstacle of obstacles) {
    ctx.fillStyle = "#5a3d2b";
    ctx.fillRect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);

    ctx.fillStyle = "#7a5338";
    ctx.fillRect(obstacle.x + 4, obstacle.y + 4, obstacle.width - 8, 8);
  }
}

function drawScore() {
  ctx.fillStyle = "#16324f";
  ctx.font = "20px Arial";
  ctx.textAlign = "left";
  ctx.fillText(`Score: ${Math.floor(score)}s`, 16, 30);
}

function drawMessage(text) {
  ctx.fillStyle = "rgba(0, 0, 0, 0.45)";
  ctx.fillRect(0, 0, canvasWidth, canvasHeight);

  ctx.fillStyle = "#ffffff";
  ctx.font = "bold 28px Arial";
  ctx.textAlign = "center";
  ctx.fillText(text, canvasWidth / 2, canvasHeight / 2 - 10);

  ctx.font = "16px Arial";
  ctx.fillText("Spaceキーでジャンプ", canvasWidth / 2, canvasHeight / 2 + 24);
}

function drawGameOverMessage() {
  ctx.fillStyle = "rgba(0, 0, 0, 0.45)";
  ctx.fillRect(0, 0, canvasWidth, canvasHeight);

  ctx.fillStyle = "#ffffff";
  ctx.font = "bold 32px Arial";
  ctx.textAlign = "center";
  ctx.fillText("GAME OVER", canvasWidth / 2, canvasHeight / 2 - 20);

  ctx.font = "18px Arial";
  ctx.fillText(`Score: ${Math.floor(score)}s`, canvasWidth / 2, canvasHeight / 2 + 14);
  ctx.fillText("Spaceキーでリスタート", canvasWidth / 2, canvasHeight / 2 + 44);
}

function jump() {
  if (player.onGround) {
    player.velocityY = -player.jumpPower;
    player.onGround = false;
  }
}

function handleSpaceKey(event) {
  if (event.code !== "Space") {
    return;
  }

  event.preventDefault();

  if (gameOver) {
    startGame();
    return;
  }

  jump();
}

window.addEventListener("keydown", handleSpaceKey);

startGame();
