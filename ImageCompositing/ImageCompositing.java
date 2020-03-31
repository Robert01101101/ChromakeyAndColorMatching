
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
	//Process Images shown
    BufferedImage foregroundA, matteA, premultA, compA;
    BufferedImage foregroundB, matteB, premultB, compB;
    BufferedImage background;
    BufferedImage matteMax, matteErode, matteBlur;
    BufferedImage premultAB, blendAB, compAB;
    BufferedImage colorTransferAB;
    BufferedImage finalOutput;
    //Not shown as process images
    BufferedImage colorTransferABpremult;
    
    
    int width; // width of the image
    int height; // height of the image
    

    // ===================================================================== CONSTRUCTOR ===================================================================== //
    public ImageCompositing() {
        // constructor
        // Get an image from the specified file in the current directory on the
        // local hard disk.
        try {
        	foregroundA = ImageIO.read(new File("frameA.jpg"));
        	foregroundB = ImageIO.read(new File("frameB.jpg"));
            background = ImageIO.read(new File("background.jpg"));

        } catch (Exception e) {
            System.out.println("Cannot load the provided image");
        }
        this.setTitle("RobertMichelsA1");
        this.setVisible(true);

        width = background.getWidth();
        height = background.getHeight();

        // ======= Apply Operators to create process images & final image ======== //

        matteA = Operators.chromaKey(foregroundA, foregroundB);
        premultA = Operators.generatePremultiplied(foregroundA, matteA);
        compA = Operators.generateKeyMix(premultA, background, matteA);
        
        matteB = Operators.chromaKey(foregroundB, foregroundA);
        premultB = Operators.generatePremultiplied(foregroundB, matteB);
        compB = Operators.generateKeyMix(premultB, background, matteB);

        matteMax = Operators.maximum(matteA, matteB);
        matteErode = Operators.erode(matteMax);
        matteBlur = Operators.blur(matteErode);
        
        blendAB = Operators.blend(foregroundB, foregroundA, .5f);
        premultAB = Operators.generatePremultiplied(blendAB, matteBlur);
        compAB = Operators.generateKeyMix(premultAB, background, matteBlur);
        
        colorTransferAB = Operators.transferColor(background, blendAB);
        colorTransferABpremult = Operators.generatePremultiplied(colorTransferAB, matteBlur);
        finalOutput = Operators.generateKeyMix(colorTransferABpremult, background, matteBlur);
        
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
        g.drawString("Foreground A", xOffset, yOffset+textOffset);
        g.drawImage(foregroundA, xOffset, yOffset, width, height, this);
        g.drawString("Matte A", w+xOffset*2, yOffset+textOffset);
        g.drawImage(matteA, w+xOffset*2, yOffset, width, height, this);
        g.drawString("Premultiplied A", 75 + w * 2, yOffset+textOffset);
        g.drawImage(premultA, w*2+xOffset*3, yOffset, width, height, this);

        //row 2
        g.drawString("Foreground B", xOffset, h+yOffset*2+textOffset);
        g.drawImage(foregroundB, xOffset, h+yOffset*2, background.getWidth(), background.getHeight(), this);
        g.drawString("Matte B", w+xOffset*2, h+yOffset*2+textOffset);
        g.drawImage(matteB, w+xOffset*2, h+yOffset*2, width, height, this);
        g.drawString("Premultiplied B", 75 + w * 2, h+yOffset*2+textOffset);
        g.drawImage(premultB, w*2+xOffset*3, h+yOffset*2, width, height, this);
        
        //right column
        g.drawString("Matte Max", w*3+xOffset*7, h/2+yOffset*2+textOffset);
        g.drawImage(matteMax, w*3+xOffset*7, h/2+yOffset*2, background.getWidth(), background.getHeight(), this);
        g.drawString("Blend AB", w*4+xOffset*8, h/2+yOffset*2+textOffset);
        g.drawImage(blendAB, w*4+xOffset*8, h/2+yOffset*2, background.getWidth(), background.getHeight(), this);
        //g.drawString("Keymix w/o color t.", w*5+xOffset*9, h/2+yOffset*2+textOffset);
        //g.drawImage(keymixRaw, w*5+xOffset*9, h/2+yOffset*2, background.getWidth(), background.getHeight(), this);
        g.drawString("Final Output", w*6+xOffset*10, h/2+yOffset*2+textOffset);
        g.drawImage(finalOutput, w*6+xOffset*10, h/2+yOffset*2, background.getWidth(), background.getHeight(), this);
        
        g.drawString("Matte erosion", w*3+xOffset*7,(int)(h*1.7f+yOffset*2+textOffset));
        g.drawImage(matteErode, w*3+xOffset*7, (int)(1.7f*h+yOffset*2), background.getWidth(), background.getHeight(), this);
        g.drawString("Matte blur", w*4+xOffset*8,(int)(h*1.7f+yOffset*2+textOffset));
        g.drawImage(matteBlur, w*4+xOffset*8, (int)(1.7f*h+yOffset*2), background.getWidth(), background.getHeight(), this);
        g.drawString("Background", w*5+xOffset*9,(int)(h*1.7f+yOffset*2+textOffset));
        g.drawImage(background, w*5+xOffset*9, (int)(1.7f*h+yOffset*2), background.getWidth(), background.getHeight(), this);
        g.drawString("Color Transfer AB", w*6+xOffset*10,(int)(h*1.7f+yOffset*2+textOffset));
        g.drawImage(colorTransferAB, w*6+xOffset*10, (int)(1.7f*h+yOffset*2), background.getWidth(), background.getHeight(), this);
        
    }
    
    
    
    
    // =======================================================//

    public static void main(String[] args) {

    	ImageCompositing img = new ImageCompositing();// instantiate this object
        img.repaint();// render the window

    }
}
// =======================================================//






