import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

public class SnakeGame extends JPanel implements ActionListener, KeyListener {
    private class Tile {
        int x;
        int y;
        Tile(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    int boardWidth;
    int boardHeight;
    int tileSize = 25;
    Tile snakeHead;
    Tile food;
    Tile starPoint; // Star point tile
    Random random;
    Timer gameLoop;
    int velocityX;
    int velocityY;
    int score = 0;
    ArrayList<Tile> snakeBody;
    boolean gameOver = false;
    Thread starPointThread; // Thread to handle star point spawning

    SnakeGame(int boardHeight, int boardWidth) {
        this.boardHeight = boardHeight;
        this.boardWidth = boardWidth;
        setPreferredSize(new Dimension(this.boardWidth, this.boardHeight));
        setBackground(Color.black);
        addKeyListener(this);
        setFocusable(true);
        snakeHead = new Tile(5, 5);
        food = new Tile(15, 15);
        starPoint = null; // Initialize star point as null
        random = new Random();
        placeFood();
        gameLoop = new Timer(100, this);
        gameLoop.start();
        velocityX = 0;
        velocityY = 0;
        snakeBody = new ArrayList<>();

        // Start the star point management thread
        startStarPointThread();
    }

    public boolean collision(Tile tile1, Tile tile2) {
        return tile1.x == tile2.x && tile1.y == tile2.y;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        for (int i = 0; i < boardWidth / tileSize; i++) {
            for (int j = 0; j < boardHeight / tileSize; j++) {
                g.drawRect(i * tileSize, j * tileSize, tileSize, tileSize);
            }
        }

        g.setColor(Color.green);
        g.fillRect(snakeHead.x * tileSize, snakeHead.y * tileSize, tileSize, tileSize);

        g.setColor(Color.red);
        g.fillRect(food.x * tileSize, food.y * tileSize, tileSize, tileSize);

        if (starPoint != null) { // Draw the star point if it exists
            g.setColor(Color.yellow);
            g.fillOval(starPoint.x * tileSize, starPoint.y * tileSize, tileSize, tileSize);
        }

        for (Tile snakePart : snakeBody) {
            g.setColor(Color.green);
            g.fillRect(snakePart.x * tileSize, snakePart.y * tileSize, tileSize, tileSize);
        }

        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        if (!gameOver) {
            g.drawString("Score: " + score, 10, 20);
        } else {
            g.drawString("Game over", 10, 20);
        }
    }

    public void placeFood() {
        food.x = random.nextInt(boardWidth / tileSize);
        food.y = random.nextInt(boardHeight / tileSize);
    }

    public void placeStarPoint() {
        starPoint = new Tile(random.nextInt(boardWidth / tileSize), random.nextInt(boardHeight / tileSize));
    }

    public void move() {
        if (!snakeBody.isEmpty()) {
            int prevX = snakeHead.x;
            int prevY = snakeHead.y;

            for (int i = 0; i < snakeBody.size(); i++) {
                int tempX = snakeBody.get(i).x;
                int tempY = snakeBody.get(i).y;
                snakeBody.get(i).x = prevX;
                snakeBody.get(i).y = prevY;
                prevX = tempX;
                prevY = tempY;
            }
        }

        snakeHead.x += velocityX;
        snakeHead.y += velocityY;

        // Check collision with normal food
        if (collision(snakeHead, food)) {
            snakeBody.add(new Tile(snakeHead.x - velocityX, snakeHead.y - velocityY));
            placeFood();
            score++;

            // Start checking for star point spawning
            synchronized (this) {
                this.notify(); // Notify the star point thread
            }
        }

        // Check collision with star point
        if (starPoint != null && collision(snakeHead, starPoint)) {
            snakeBody.add(new Tile(snakeHead.x - velocityX, snakeHead.y - velocityY));
            score += 5; // Star point increases score by 5
            starPoint = null; // Remove star point after consuming it
        }

        // Check for collision with the snake's body
        for (Tile part : snakeBody) {
            if (collision(snakeHead, part)) {
                gameOver = true;
                break;
            }
        }

        // Check for boundary collisions
        if (snakeHead.x < 0 || snakeHead.x >= boardWidth / tileSize || snakeHead.y < 0 || snakeHead.y >= boardHeight / tileSize) {
            gameOver = true;
        }
    }

    private long starPointCreationTime; 
    private void startStarPointThread() {
        starPointThread = new Thread(() -> {
            while (!gameOver) {
                try {
                    Thread.sleep(10); 
                } catch (InterruptedException e) {
                    if (gameOver) {
                        return;
                    }
                }
                // If score is divisible by 5, spawn a star point
                if (score % 5 == 0 && starPoint == null && score!=0) {
                    placeStarPoint();
                    starPointCreationTime=System.currentTimeMillis();
                }

                if(starPoint!=null && (System.currentTimeMillis()-starPointCreationTime)>2000){
                    starPoint = null;
                }
            }
        });
        starPointThread.start();

    }
    

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
            starPointThread.interrupt(); // Stop the thread when game is over
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP && velocityY != 1) {
            velocityX = 0;
            velocityY = -1;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN && velocityY != -1) {
            velocityX = 0;
            velocityY = 1;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && velocityX != -1) {
            velocityX = 1;
            velocityY = 0;
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT && velocityX != 1) {
            velocityX = -1;
            velocityY = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}
