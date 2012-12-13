package Xonix;

import javax.swing.*;

import Xonix.Board;

class Xonix extends JFrame{

        int sizex = 100;
        int sizey = 50;
        int unitsize = 5;
        static int offx = 6;    // to fit in window
        static int offy = 40;

        
    public Xonix() {
        add( new Board(this) );
        setTitle( "Xonj" );
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize( sizex*unitsize+offx, sizey*unitsize+offy );
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible( true );
    }

    public static void main(String[] argS){
//      Run GUI codes in Event-Dispatching thread for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Xonix();
            }
        });
    }
}
