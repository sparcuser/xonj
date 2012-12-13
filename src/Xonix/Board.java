package Xonix;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Deque;
import java.util.LinkedList;

class Board extends JPanel implements ActionListener {
    int sizex, sizey, unitsize;
    enum Entity { Empty, Wall, Dot, BDot, Xonix }
    Entity map[][];
    ArrayList<Dot> dots = new ArrayList<Dot>();
    int score, xonleft, level;
    int xonx, xony;
    boolean ingame = false;
    boolean dying = false;
    boolean onsea = false;
    int reqdx, reqdy, xondx, xondy;
    Font smallfont = new Font("Helvetica", Font.BOLD, 12);
    Timer timer;

    public Board( Xonix xn ) {
        this.sizex = xn.sizex;
        this.sizey = xn.sizey;
        this.unitsize = xn.unitsize;
        addKeyListener(new TAdapter());
        setBackground(Color.black);
        setDoubleBuffered(true);
        setFocusable(true);  // so that can receive key-events
        requestFocus();
        timer = new Timer(10, this);
        timer.start();
    }
    
    public void addNotify() {
        super.addNotify();
        GameInit();
    }

    class Dot {
        public int x, y, dx, dy;
        public Entity type;
        public Dot( int x, int y, int dx, int dy, Entity type ) {
            this.x = x; this.dx = dx;
            this.y = y; this.dy = dy;
            this.type = type;
        }
    }

    void GameInit() {
        xonleft = 3;
        level = 5;
        LevelInit();
    }
    
    void LevelInit() {
        score = 0;
        map = new Entity[sizex+1][sizey+1];
        for( int i = 0; i < sizey; i++ )
            for( int j = 0; j < sizex; j++ ) {
                if( i == 0 || i == 1 || i == sizey-2 || i == sizey-1 )
                    map[j][i] = Entity.Wall;
                else if ( j == 0 || j ==1 || j == sizex-2 || j == sizex-1 )
                    map[j][i] = Entity.Wall;
                else map[j][i] = Entity.Empty;
            }
        Random rand = new Random();
        int xdot, ydot, dx, dy;
        
        dots.clear();
        if( ingame )
            for( int i = 0; i <= level; i++ ) {
                do {
                    xdot = rand.nextInt( sizex-1 ) + 1;
                    ydot = rand.nextInt( sizey-1 ) + 1;
                } while( map[xdot][ydot] != Entity.Empty );
                do {
                    dx = rand.nextInt(3) - 1;
                } while( dx == 0 );
                do {
                    dy = rand.nextInt(3) - 1;
                } while( dy == 0 );
                
                map[xdot][ydot] = Entity.Dot;
                dots.add( new Dot(xdot,ydot, dx, dy, Entity.Dot ) );
            }
            for( int i = 1; i <= level-1; i++ ) {
                do {
                    xdot = rand.nextInt( sizex-1 ) + 1;
                    ydot = sizey-1;
                } while( map[xdot][ydot] != Entity.Wall );
                do {
                    dx = rand.nextInt(3) - 1;
                } while( dx == 0 );
                do {
                    dy = rand.nextInt(3) - 1;
                } while( dy == 0 );
                
                map[xdot][ydot] = Entity.BDot;
                dots.add( new Dot(xdot,ydot, dx, dy, Entity.BDot ) );
            }
        LevelContinue();
    }
    
    void LevelContinue() {
        xonx = 0; xony = 0;
        xondx = 0; xondy = 0;
        reqdx = 0; reqdy = 0;
        dying = false;
    }
    
    void Death() {
        xonleft--;
        if (xonleft == 0)
            ingame = false;
        LevelContinue();
    }

    void CheckLevel() {
        if( score > 75 ) {  // fill 75 percent to complete level
            level += 1;
            LevelInit();
        }
    }
    
    void DrawField( Graphics g2d ) {
        for( int i = 0; i < sizey; i++ )
            for( int j = 0; j < sizex; j++ )
                switch( map[j][i] ) {
                    case Wall:
                        g2d.setColor( Color.white );
                        g2d.fillRect( j*unitsize, i*unitsize, unitsize, unitsize );
                        break;
                    case Xonix:
                        g2d.setColor( Color.red );
                        g2d.fillRect( j*unitsize, i*unitsize, unitsize, unitsize );
                        break;
                    case Dot:
                        g2d.setColor( Color.yellow );
                        g2d.fillOval( j*unitsize, i*unitsize, unitsize, unitsize );
                        break;
                    case BDot:
                        g2d.setColor( Color.black );
                        g2d.fillOval( j*unitsize, i*unitsize, unitsize, unitsize );
                        break;
                }
    }

    void PlayGame(Graphics2D g2d) {
        if (dying) {
            Death();
        } else {
            MoveXonix();
            DrawXonix( g2d );
            moveDots();
            CheckLevel();
        }
    }

    void MoveXonix() {
        if( reqdx != 0 && xony % unitsize == 0 ) { xondx = reqdx; xondy = 0; }
        if( reqdy != 0 && xonx % unitsize == 0 ) { xondy = reqdy; xondx = 0; }
        xonx += xondx;
        xony += xondy;
        if( xonx < 0 ) { xonx = 0; xondx = 0; }
        if( xonx > (sizex-1)*unitsize ) { xonx = (sizex-1)*unitsize; xondx = 0; }
        if( xony < 0 ) { xony = 0; xondy = 0; }
        if( xony > (sizey-1)*unitsize ) { xony = (sizey-1)*unitsize; xondy = 0; }
        if( (xondx != 0 || xondy != 0) && (xonx % unitsize == 0 && xony % unitsize == 0) ) 
            if( map[xonx/unitsize][xony/unitsize] == Entity.Empty ) {
                map[xonx/unitsize][xony/unitsize] = Entity.Xonix;
                onsea = true;
            } 
            else if( map[xonx/unitsize][xony/unitsize] == Entity.Wall && onsea ) {
                FillArea();
                xondx = reqdx = 0;
                xondy = reqdy = 0;
                onsea = false;
            }
    }

    void floodFill( Entity[][] mapt, int x, int y ) {
        Integer xy = y * sizex + x;

        if ( mapt[x][y] == Entity.Empty || mapt[x][y] == Entity.Dot ) {
            Deque<Integer> queue = new LinkedList<Integer>();
            do {
                x = xy % sizex;
                y = ( xy - x ) / sizex;
                while (x > 0 && mapt[x-1][y] == Entity.Empty ) {
                    x--;
                }
                boolean spanUp = false;
                boolean spanDown = false;
                while (x < sizex && mapt[x][y] == Entity.Empty ) {
                    mapt[x][y] = Entity.Wall;
                    if( !spanUp && y > 0 && mapt[x][y-1] == Entity.Empty ) {
                        xy = (y - 1) * sizex + x;
                        queue.add( xy );
                        spanUp = true;
                    } else if( spanUp && y > 0 && mapt[x][y-1] != Entity.Empty ) {
                        spanUp = false;
                    }   
                    if( !spanDown && y < sizey - 1 && mapt[x][y+1] == Entity.Empty ) {
                        xy = (y + 1) * sizex + x;
                        queue.add( xy );
                        spanDown = true;
                    } else if( spanDown && y < sizey - 1 && mapt[x][y+1] != Entity.Empty ) {
                        spanDown = false;
                    }
                    x++;
                }
            } while ((xy = queue.pollFirst()) != null);
        }
    }

    void FillArea() {
        Entity[][] mapt = new Entity[sizex+1][sizey+1];
        for( int i = 0; i <= sizey; i++ )
            for( int j = 0; j <= sizex; j++ ) {
                if( map[j][i] == Entity.Xonix ) map[j][i] = Entity.Wall;
                mapt[j][i] = map[j][i];
            }
        for( Dot i : dots )
            floodFill( mapt, i.x, i.y );
        int area = 0;
        for( int i = 0; i <= sizey; i++ )
            for( int j = 0; j <= sizex; j++ ) {
                if( mapt[j][i] == Entity.Empty ) map[j][i] = Entity.Wall;
                if( map[j][i] == Entity.Wall ) area += 1;
            }
        score = Math.round((float)area/(float)((sizex-4)*(sizey-4))*100f);
    }
    
    void moveDots() {
        // TODO: Fix a bug with dots movement
        for( Dot i : dots ) {
            int nx = i.x + i.dx;
            int ny = i.y + i.dy;
            Entity freepoint;
            boolean outx = false, outy = false;

            if( i.type == Entity.Dot )
                freepoint = Entity.Empty;
            else
                freepoint = Entity.Wall;

            if( nx+i.dx<0 || nx+i.dx>sizex || map[nx+i.dx][ny] != freepoint ) { i.dx *= -1; outx = true; }
            if( ny+i.dy<0 || ny+i.dy>sizey || map[nx][ny+i.dy] != freepoint ) { i.dy *= -1; outy = true; }
                
            if( !outx && !outy && map[nx+i.dx][ny+i.dy] != freepoint ) {
                i.dx *= -1;
                i.dy *= -1;
            }

            // TODO: check why is this 'if' is needed
            if( nx > 0 && nx <= sizex && ny >0 && ny <= sizey && map[nx][ny] == freepoint ) {
                map[i.x][i.y] = freepoint;
                map[nx][ny] = i.type;
                i.x = nx;
                i.y = ny;
            }
        }
    }
    
    void DrawXonix( Graphics2D g2d ) {
        g2d.setColor( Color.green );
        g2d.fillRect( xonx, xony, unitsize, unitsize ); 
    }

    public void DrawScore(Graphics2D g) {
        String s;

        g.setFont(smallfont);
        g.setColor(new Color(96, 128, 255));
        s = "Xonix: " + xonleft + "     Level: " + level + "     Area: " + score;
        g.drawString(s, unitsize*2, sizey*unitsize+unitsize*2 );
    }
    
    public void ShowIntroScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 32, 48));
        g2d.fillRect(50, sizey*unitsize / 2 - 30, sizex*unitsize - 100, 50);
        g2d.setColor(Color.white);
        g2d.drawRect(50, sizey*unitsize / 2 - 30, sizex*unitsize - 100, 50);

        String s = "Press S to start.";
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = this.getFontMetrics(small);

        g2d.setColor(Color.white);
        g2d.setFont(small);
        g2d.drawString(s, (sizex*unitsize - metr.stringWidth(s)) / 2, sizey*unitsize / 2);
    }

    // Override paintComponent to do custom drawing.
    // Called back by repaint().
    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = ( Graphics2D )g;
        super.paint( g2d );
        g.setColor( Color.white );
        DrawField( g2d );
        DrawScore( g2d );
        if ( ingame )
            PlayGame( g2d );
        else
            ShowIntroScreen(g2d);
        Toolkit.getDefaultToolkit().sync();
        g.dispose();
    }
    
    class TAdapter extends KeyAdapter {
        public void keyPressed(KeyEvent e) {

            int key = e.getKeyCode();

            if (ingame) {
                if (key == KeyEvent.VK_LEFT) {
                    reqdx=-1;
                    reqdy=0;
                }
                else if (key == KeyEvent.VK_RIGHT) {
                    reqdx=1;
                    reqdy=0;
                }
                else if (key == KeyEvent.VK_UP) {
                    reqdx=0;
                    reqdy=-1;
                }
                else if (key == KeyEvent.VK_DOWN) {
                    reqdx=0;
                    reqdy=1;
                }
                else if (key == KeyEvent.VK_ESCAPE && timer.isRunning()) {
                    ingame=false;
                    System.exit(0);
                }
                else if (key == KeyEvent.VK_PAUSE) {
                    if (timer.isRunning())
                        timer.stop();
                    else timer.start();
                }
            }
            else {        
                if (key == 's' || key == 'S') {
                    ingame=true;
                    GameInit();
                }
            }
        }
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();

            if (key == Event.LEFT || key == Event.RIGHT || 
                key == Event.UP ||  key == Event.DOWN) {
                    reqdx=0;
                    reqdy=0;
            }
        }
    }
    public void actionPerformed(ActionEvent e) {
        repaint();  
    }
}
