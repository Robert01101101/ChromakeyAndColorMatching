
/*File BasicImageCompositing.java
 IAT455 - Workshop week 4
 Basic Image Compositing
 **********************************************************/
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import javax.imageio.ImageIO;

class ImageCompositing extends Frame {
    BufferedImage frameAImg;
    BufferedImage frameBImg;
    BufferedImage backgroundImg;
    BufferedImage matteAImg;
    BufferedImage premultipliedAImg;
    BufferedImage matteBImg;
    BufferedImage premultipliedBImg;
    BufferedImage keymixImg, keymixRaw;
    BufferedImage premultipliedBlendImg, premultipliedRaw;
    BufferedImage maxMattesImg, maxMatteErode, maxMatteBlur;
    BufferedImage blendImg;
    BufferedImage transferColor, transferColorBlend;
    
    int width; // width of the image
    int height; // height of the image
    

    // ===================================================================== CONSTRUCTOR ===================================================================== //
    public ImageCompositing() {
        // constructor
        // Get an image from the specified file in the current directory on the
        // local hard disk.
        try {
        	frameAImg = ImageIO.read(new File("frameA.jpg"));
        	frameBImg = ImageIO.read(new File("frameB.jpg"));
            backgroundImg = ImageIO.read(new File("background.jpg"));

        } catch (Exception e) {
            System.out.println("Cannot load the provided image");
        }
        this.setTitle("RobertMichelsA1");
        this.setVisible(true);

        width = backgroundImg.getWidth();
        height = backgroundImg.getHeight();

        // ======= Create Matte, Premultiplied, Keymix ======== //

        matteAImg = Operators.chromaKey(frameAImg, frameBImg);
        premultipliedAImg = Operators.generatePremultiplied(frameAImg, matteAImg);
        
        matteBImg = Operators.chromaKey(frameBImg, frameAImg);
        premultipliedBImg = Operators.generatePremultiplied(frameBImg, matteBImg);

        blendImg = Operators.blend(frameBImg, frameAImg, .5f);
        maxMattesImg = Operators.maximum(matteAImg, matteBImg);
        //maxMatteErode = Operators.erode(maxMattesImg);
        //maxMatteBlur = Operators.blur(maxMatteErode);
        transferColor = Operators.transferColor(backgroundImg, blendImg);
        transferColorBlend = Operators.blend(transferColor, blendImg, .8f);
        premultipliedBlendImg = Operators.generatePremultiplied(transferColorBlend, maxMattesImg);
        premultipliedRaw = Operators.generatePremultiplied(blendImg, maxMattesImg);
        keymixImg = Operators.generateKeyMix(premultipliedBlendImg, backgroundImg, maxMattesImg);
        keymixRaw = Operators.generateKeyMix(premultipliedRaw, backgroundImg, maxMattesImg);
        
        // ================================= //

        // Anonymous inner-class listener to terminate program
        this.addWindowListener(new WindowAdapter() {// anonymous class definition
            public void windowClosing(WindowEvent e) {
                System.exit(0);// terminate the program
            }// end windowClosing()
        }// end WindowAdapter
        );// end addWindowListener
    }// end constructor
    
    
    // ===================================================================== PAINT () ===================================================================== //

    public void paint(Graphics g) {

        // Draw all Images + Captions
        int w = width;
        int h = height;
        int xOffset = 25;
        int yOffset = 50;
        int textOffset = -5;

        this.setSize(w * 9, (int)(h * 3.2f));

        g.setColor(Color.BLACK);
        Font f1 = new Font("Verdana", Font.PLAIN, 13);
        g.setFont(f1);
        
        //row 1
        g.drawString("Foreground Image Fa", xOffset, yOffset+textOffset);
        g.drawImage(frameAImg, xOffset, yOffset, width, height, this);
        g.drawString("Matte Image Ma", w+xOffset*2, yOffset+textOffset);
        g.drawImage(matteAImg, w+xOffset*2, yOffset, width, height, this);
        g.drawString("Premultiplied Image PMa", 75 + w * 2, yOffset+textOffset);
        g.drawImage(premultipliedAImg, w*2+xOffset*3, yOffset, width, height, this);

        //row 2
        g.drawString("Foreground Image Fb", xOffset, h+yOffset*2+textOffset);
        g.drawImage(frameBImg, xOffset, h+yOffset*2, backgroundImg.getWidth(), backgroundImg.getHeight(), this);
        g.drawString("Matte Image Mb", w+xOffset*2, h+yOffset*2+textOffset);
        g.drawImage(matteBImg, w+xOffset*2, h+yOffset*2, width, height, this);
        g.drawString("Premultiplied Image PMb", 75 + w * 2, h+yOffset*2+textOffset);
        g.drawImage(premultipliedBImg, w*2+xOffset*3, h+yOffset*2, width, height, this);
        
        //right column
        g.drawString("Maximum Ma + Mb -> Mmax", w*3+xOffset*7, h/2+yOffset*2+textOffset);
        g.drawImage(maxMattesImg, w*3+xOffset*7, h/2+yOffset*2, backgroundImg.getWidth(), backgroundImg.getHeight(), this);
        g.drawString("Blend Image Fa + Fb -> Fab", w*4+xOffset*8, h/2+yOffset*2+textOffset);
        g.drawImage(blendImg, w*4+xOffset*8, h/2+yOffset*2, backgroundImg.getWidth(), backgroundImg.getHeight(), this);
        
        g.drawString("Composite Fab + Mmax", w*5+xOffset*9, h/2+yOffset*2+textOffset);
        g.drawImage(keymixRaw, w*5+xOffset*9, h/2+yOffset*2, backgroundImg.getWidth(), backgroundImg.getHeight(), this);
        g.drawString("Keymix: AB(p.m.) + Mmax + B", w*6+xOffset*10, h/2+yOffset*2+textOffset);
        g.drawImage(keymixImg, w*6+xOffset*10, h/2+yOffset*2, backgroundImg.getWidth(), backgroundImg.getHeight(), this);/*
        g.drawString("Matte erosion", w*3+xOffset*7,(int)(h*1.7f+yOffset*2+textOffset));
        g.drawImage(maxMatteErode, w*3+xOffset*7, (int)(1.7f*h+yOffset*2), backgroundImg.getWidth(), backgroundImg.getHeight(), this);
        g.drawString("Matte blur", w*4+xOffset*8,(int)(h*1.7f+yOffset*2+textOffset));
        g.drawImage(maxMatteBlur, w*4+xOffset*8, (int)(1.7f*h+yOffset*2), backgroundImg.getWidth(), backgroundImg.getHeight(), this);*/
        g.drawString("Background Image", w*5+xOffset*9,(int)(h*1.7f+yOffset*2+textOffset));
        g.drawImage(backgroundImg, w*5+xOffset*9, (int)(1.7f*h+yOffset*2), backgroundImg.getWidth(), backgroundImg.getHeight(), this);
        g.drawString("Color Transfer A -> B", w*6+xOffset*10,(int)(h*1.7f+yOffset*2+textOffset));
        g.drawImage(transferColor, w*6+xOffset*10, (int)(1.7f*h+yOffset*2), backgroundImg.getWidth(), backgroundImg.getHeight(), this);
        
    }
    
    
    
    
    // =======================================================//

    public static void main(String[] args) {

    	ImageCompositing img = new ImageCompositing();// instantiate this object
        img.repaint();// render the window

    }
}
// =======================================================//






