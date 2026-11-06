package pack.engine;

import pack.student.Setup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Janela principal (ponto de entrada, como no SBCarsEngine). Rode esta classe.
 * Cria a janela, repassa teclado/mouse para a {@link Simulation} e mantém o laço do jogo
 * numa thread própria com triple buffering.
 */
public final class Window extends JFrame implements Runnable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public static final int INITIAL_WIDTH = 1460;
    public static final int INITIAL_HEIGHT = 840;

    private final Canvas canvas = new Canvas();
    private final transient Simulation simulation;

    private Window() {
        super("Pack AI 2D — Steering Behaviors");
        this.simulation = new Simulation(new Setup().createScenes());

        final var screen = Toolkit.getDefaultToolkit().getScreenSize();
        final int w = Math.min(INITIAL_WIDTH, (int) (screen.width * 0.94));
        final int h = Math.min(INITIAL_HEIGHT, (int) (screen.height * 0.88));
        canvas.setPreferredSize(new Dimension(w, h));
        canvas.setBackground(Color.BLACK);
        canvas.setIgnoreRepaint(true);
        canvas.setFocusTraversalKeysEnabled(false); // deixa o TAB chegar até o jogo
        setFocusTraversalKeysEnabled(false);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIgnoreRepaint(true);
        setResizable(true);
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                start();
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                canvas.requestFocusInWindow();
                simulation.mousePressed(e.getX(), e.getY(), e.getButton());
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                simulation.mouseMoved(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                simulation.mouseMoved(e.getX(), e.getY());
            }
        });
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
                simulation.keyPressed(e.getKeyCode(), e.isShiftDown());
            }
        });
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new Window().setVisible(true));
    }

    private void start() {
        canvas.createBufferStrategy(3);
        canvas.requestFocus();
        final var gameLoop = new Thread(this, "game-loop");
        gameLoop.setDaemon(true);
        gameLoop.start();
    }

    @Override
    public void run() {
        final var strategy = canvas.getBufferStrategy();
        long prev = System.nanoTime();
        try {
            while (true) {
                final long now = System.nanoTime();
                final double secs = (now - prev) / 1e9;
                prev = now;

                simulation.update(secs);

                do {
                    do {
                        final var g2d = (Graphics2D) strategy.getDrawGraphics();
                        simulation.render(g2d, canvas.getWidth(), canvas.getHeight());
                        g2d.dispose();
                    } while (strategy.contentsRestored());
                    strategy.show();
                } while (strategy.contentsLost());
                Toolkit.getDefaultToolkit().sync(); // helps on some systems

                // ~120 quadros por segundo no máximo
                final long elapsed = System.nanoTime() - now;
                final long sleepMs = Math.max(1, (8_000_000L - elapsed) / 1_000_000L);
                Thread.sleep(sleepMs);
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
        System.exit(0);
    }
}
