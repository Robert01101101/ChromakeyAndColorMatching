import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class Operators {

	//Keying Values (These Values define the HSB value range that we would like to pick. Each array has 4 values: A, B, C, D. The range works as follows:
    // 					A:0% match, B: 100% match, C: 100% match, D: 0% match. Values between B-C are 100% matches. Values in the range A-B and C-D are
    //					partial matches. I calculated the ranges using the colour picker inside photoshop and guessing min & max values.)
    //					A	   B	  C	  	 D
    /*
    float[] range1H = {0.39f, 0.41f, 0.44f, 0.46f};
    float[] range1S = {0.5f, 0.52f,  0.8f,  0.85f};
    float[] range1B = {0.4f,  0.45f, 0.71f, 0.75f};
    
    float[] range2H = {0.39f, 0.41f, 0.44f, 0.46f};
    float[] range2S = {0.5f, 0.52f,  0.8f,  0.85f};
    float[] range2B = {0.4f,  0.45f, 0.71f, 0.75f};*/
    
    static float[][] rangesA = {{0.33f, 0.4f, 0.5f, 0.56f},		//H Green
								{0.42f, 0.55f,  0.7f,  0.82f},	//S
								{0.38f,  0.50f, 0.82f, 0.95f}};	//B
    
    static float[][] rangesB = {{0.80f, 0.85f, 0.93f, 0.97f},	//H	Magenta
								{0.25f, 0.4f,  0.58f,  0.75f},	//S
								{0.55f,  0.68f, 0.95f, 1f}};	//B
	
	
	
	
    // ===================================================================== Our Methods ===================================================================== //
    // Generate matte Image by using the chroma Key.
    // img is the image to apply chroma key to. imgCompare is it's pair image for 
    // purpose of detecting img's background color by comparison to imgCompare.
	public static BufferedImage chromaKey(BufferedImage img, BufferedImage imgCompare) {
    	int width = img.getWidth();
        int height = img.getHeight();

        WritableRaster wRaster = img.copyData(null);
        BufferedImage matteImg = new BufferedImage(img.getColorModel(), wRaster, img.isAlphaPremultiplied(), null);

        //IDENTIFY BACKGROUND COLOR
        //Step 1: Calculate Average hue of both images (in order to use difference to figure out active BG color)
        float avgHue = 0; float avgHueCompare = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
            	
            	int rgb = img.getRGB(i, j);
            	float [] hsbvals = Color.RGBtoHSB(getRed(rgb), getGreen(rgb), getBlue(rgb), null);
            	int rgbCompare = imgCompare.getRGB(i, j);
            	float [] hsbvalsCompare = Color.RGBtoHSB(getRed(rgbCompare), getGreen(rgbCompare), getBlue(rgbCompare), null);
            	avgHue += hsbvals[0];
            	avgHueCompare += hsbvalsCompare[0];
            }
        }
        //Divide sum of pixel's hue by pixel count (-> mean)
        avgHue = avgHue / width*height;
        avgHueCompare = avgHueCompare / width*height;
        
        //Step 2: if avgHue > avgHueCompare, image BG is magenta, otherwise it's green.
        int range = avgHue > avgHueCompare ? 1 : 0;
        
        //GET CHROMAKEY MATTE 
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
            	
            	int rgb = img.getRGB(i, j);
            	float [] hsbvals = Color.RGBtoHSB(getRed(rgb), getGreen(rgb), getBlue(rgb), null);
            	
            	//get value of the matte pixel
            	matteImg.setRGB(i, j, getChromaKeyValue(range, hsbvals));
            	
            }
        }
    	return matteImg;
    }
	
    
    //return color between 0x000000 (background: in chroma key range) and 0xFFFFFF (foreground: outside of chroma key range)
	public static int getChromaKeyValue(int range, float [] hsbvals) {
    	int rgb = 0x000000;
    	float mono = 0;
    	float[][] ranges = range == 0 ? rangesA : rangesB;
    	
    	//CALCULATE CHROMAKEY MATTE
    	//Calculate value between 0 & 1 for each channel (0 = in range, 1 = outside of range). Add each channel's value to mono. Mono ranges from 0-7.
    	//That is the case, because we do 3 calculations (1 per channel) and the hue channel is multiplied by 5 for extra weight. 5 + 1 + 1 = 7.
    	//Mono is remapped to a value between 0-1, to represent the brightness of the matte pixel.
    	for (int i=0; i<3; i++) {
    		float channelVal = hsbvals[i];
    		int channel;
    		
    		//grab the definitions for the range for the current channel (H, S or V).
    		switch (i) {
    			case 0: channel = 0; break;
    			case 1: channel = 1; break;
    			case 2: channel = 2; break;
    			default: channel = 0; break;
    		}
    		if (channelVal >= ranges[channel][0] && channelVal <= ranges[channel][3]) {
    			//inside range
    			if (channelVal >= ranges[channel][0] && channelVal < ranges[channel][1]) {
    				// in FadeIn
    				mono += mapNumber(channelVal, ranges[channel][0], ranges[channel][1], 1, 0);
    			} else if (channelVal >= ranges[channel][1] && channelVal < ranges[channel][2]) {
    				// in main
    				mono += 0;
    			} else if (channelVal >= ranges[channel][2] && channelVal <= ranges[channel][3]) {
    				// in FadeOut
    				mono += mapNumber(channelVal, ranges[channel][2], ranges[channel][3], 0, 1);
    			}
    		} else {
    			//outside range
    			mono += 1;
    		}
    		if (i == 0) mono *= 5; //give extra weight to hue. (h : s : b) has following weight ratio: (5 : 1 : 1)
    	}
    	
    	mono = mapNumber(mono, 0, 7, 0, 1); //remap sum of all 3 channels' key analysis. This number represents how much
    										// the pixel falls into all 3 channels in total, with more weight towards hue.
    	mono = (mono > 0.7f) ? 1 : mono;	// clip values
    	mono = (mono < 0.3f) ? 0 : mono;
    	rgb = new Color(mono, mono, mono).getRGB();
    	return rgb;
    }
    
    
    //return Premultiplied Image (O = A * M / 255), using the previously generated Matte
	public static BufferedImage generatePremultiplied(BufferedImage img, BufferedImage imgMatte) {
    	int width = img.getWidth();
        int height = img.getHeight();

        WritableRaster wRaster = img.copyData(null);
        BufferedImage preMultImg = new BufferedImage(img.getColorModel(), wRaster, img.isAlphaPremultiplied(), null);

        //get individual Pixels
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
            	
            	int rgb = img.getRGB(i, j);
            	int matteRGB = imgMatte.getRGB(i, j);
            	
            	preMultImg.setRGB(i, j, new Color(clipChannelValue(getRed(rgb) * getRed(matteRGB) / 255), 
            			clipChannelValue(getGreen(rgb) * getGreen(matteRGB) / 255), clipChannelValue(getBlue(rgb) * getBlue(matteRGB) / 255)).getRGB());
            }
        }
    	return preMultImg;
    }
    
    
    //return Keymix Image, using background image, premultiplied image, & Keymix operation: O = (A x M) + [(1 – M) x B]
	public static BufferedImage generateKeyMix(BufferedImage imgFG, BufferedImage imgBG, BufferedImage imgFGmatte) {
    	int width = imgBG.getWidth();
        int height = imgBG.getHeight();

        WritableRaster wRaster = imgBG.copyData(null);
        BufferedImage keymixImg = new BufferedImage(imgBG.getColorModel(), wRaster, imgBG.isAlphaPremultiplied(), null);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
            	
            	Color rgbOut = new Color(255,0,0);
            	Color colA = new Color (imgFG.getRGB(i, j));
            	int rgbB = imgBG.getRGB(i, j);
            	int matteRGB = imgFGmatte.getRGB(i, j);
            	
            	//prepare background image by using keymix operator with the foreground matte
            	Color colB = new Color (clipChannelValue(getRed(rgbB) - getRed(rgbB) * getRed(matteRGB) / 255), 
    					clipChannelValue(getGreen(rgbB) - getGreen(rgbB) * getGreen(matteRGB) / 255), 
    					clipChannelValue(getBlue(rgbB) - getBlue(rgbB) * getBlue(matteRGB) / 255));
            	
            	//Add Premultiplied image + prepped background image
            	rgbOut = new Color (clipChannelValue(colA.getRed() + colB.getRed()), 
    					clipChannelValue(colA.getGreen() + colB.getGreen()), clipChannelValue(colA.getBlue() + colB.getBlue())); 
            	
            	keymixImg.setRGB(i, j, rgbOut.getRGB());
            }
        }
    	return keymixImg;
    }
    
    public static BufferedImage blend(BufferedImage imgA, BufferedImage imgB, float visA) {
    	int width = imgA.getWidth();
        int height = imgA.getHeight();

        WritableRaster wRaster = imgA.copyData(null);
        BufferedImage outImg = new BufferedImage(imgA.getColorModel(), wRaster, imgA.isAlphaPremultiplied(), null);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
            
            	int rgb_A = imgA.getRGB(i, j); int rgb_B = imgB.getRGB(i, j);
            	//Do a 50/50 blend of the two images
                Color rgbOut = new Color((int)(getRed(rgb_A) * visA + getRed(rgb_B) * (1-visA)),
                		(int)(getGreen(rgb_A) * visA + getGreen(rgb_B) * (1-visA)),
                		(int)(getBlue(rgb_A) * visA + getBlue(rgb_B) * (1-visA)));
                outImg.setRGB(i, j, rgbOut.getRGB());
            }
        }
    	return outImg;
    }
    

    public static BufferedImage maximum(BufferedImage imgA, BufferedImage imgB) {
    	int width = imgA.getWidth();
        int height = imgA.getHeight();

        WritableRaster wRaster = imgA.copyData(null);
        BufferedImage outImg = new BufferedImage(imgA.getColorModel(), wRaster, imgA.isAlphaPremultiplied(), null);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
            
            	int rgb_A = imgA.getRGB(i, j); int rgb_B = imgB.getRGB(i, j);
                float [] hsv_A = Color.RGBtoHSB(getRed(rgb_A), getGreen(rgb_A), getBlue(rgb_A), null);
                float [] hsv_B = Color.RGBtoHSB(getRed(rgb_B), getGreen(rgb_B), getBlue(rgb_B), null);
                //use rgb value of the brighter pixel
                int rgbOut = hsv_A[2] > hsv_B[2] ? rgb_A : rgb_B;
                outImg.setRGB(i, j, rgbOut);
            }
        }
    	return outImg;
    }
    
    public static BufferedImage blur(BufferedImage image) {
        float data[] = { 0.0625f, 0.150f, 0.0625f, 0.150f, 0.150f, 0.150f, 0.0625f, 0.150f, 0.0625f };
        Kernel kernel = new Kernel(3, 3, data);
        ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return convolve.filter(image, null);
	}
    
    
    
    // Erosion using a Kernel (shrinks subject in matte)
 	public static BufferedImage erode(BufferedImage image) {
 		
 		int width = image.getWidth(); 
 		int height = image.getHeight(); 

 		WritableRaster wRaster = image.copyData(null);
 		BufferedImage copy = new BufferedImage(image.getColorModel(), wRaster, image.isAlphaPremultiplied(), null);

 		float [][] kernel = new float [3][3];
 		
 		//apply the operation to each pixel
 		for (int i = 0; i < width; i++){ 
 			for (int j = 0; j < height; j++) {
 				float [] curPixel;
 				kernel[0][0] = getBrightness(image.getRGB(Math.max(i-1, 0), Math.max(j-1, 0)));
 				kernel[0][1] = getBrightness(image.getRGB(i, Math.max(j-1, 0)));
 				kernel[0][2] = getBrightness(image.getRGB(Math.min(i+1, width-1), Math.max(j-1, 0)));
 				
 				kernel[1][0] = getBrightness(image.getRGB(Math.max(i-1, 0), j));
 				kernel[1][1] = getBrightness(image.getRGB(i, j));
 				kernel[1][2] = getBrightness(image.getRGB(Math.min(i+1, width-1), j));
 				
 				kernel[2][0] = getBrightness(image.getRGB(Math.max(i-1, 0), Math.min(j+1, height-1)));
 				kernel[2][1] = getBrightness(image.getRGB(i, Math.min(j+1, height-1)));
 				kernel[2][2] = getBrightness(image.getRGB(Math.min(i+1, width-1), Math.min(j+1, height-1)));
 				
 				copy.setRGB(i, j, calcConvolve(kernel));
 			}
 		}
 		return copy; 
 	}
 	
 	public static int calcConvolve(float [][] kernel) {
		float outBrightness;
		
		//Get smallest value within kernel and return it
		Arrays.sort(kernel[0]); Arrays.sort(kernel[1]); Arrays.sort(kernel[2]);
		float [] pixelRows = { kernel[0][0], kernel[1][0], kernel[2][0] };
		Arrays.sort(pixelRows);
		
		outBrightness = pixelRows[0];

		return Color.HSBtoRGB(0, 0, outBrightness);
	}
    
    
    
    
    
    
    
    
    
    
    // =============================================================== Color Transfer =================================//
    // Transfer color from imgA to imgB and return imgB with imgA's color profile
    public static BufferedImage transferColor(BufferedImage imgA, BufferedImage imgB) {
    	int width = imgB.getWidth();
        int height = imgB.getHeight();

        WritableRaster wRaster = imgB.copyData(null);
        BufferedImage outImg = new BufferedImage(imgB.getColorModel(), wRaster, imgB.isAlphaPremultiplied(), null);

        Color_Space_Converter color_Space_Converter = new Color_Space_Converter();
        Color_Space_Converter.ColorSpaceConverter colorSpaceConverter = color_Space_Converter.new ColorSpaceConverter();
        
        //create variables to store XYZ values of each pixel in both pictures, and calculate mean + SD.
        double[] L_imgA = new double[width*height];
        double[] A_imgA = new double[width*height];
        double[] B_imgA = new double[width*height];
        double[] L_imgB = new double[width*height];
        double[] A_imgB = new double[width*height];
        double[] B_imgB = new double[width*height];
        double mean_X_imgA, sd_X_imgA, mean_Y_imgA, sd_Y_imgA, mean_Z_imgA, sd_Z_imgA;
        double mean_X_imgB, sd_X_imgB, mean_Y_imgB, sd_Y_imgB, mean_Z_imgB, sd_Z_imgB;
        int count = 0;
        
        //Get color values of all pixels of imgA + imgB, using XYZ color format
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
            	
            	int rgbA = imgA.getRGB(i, j);
            	int rgbB = imgB.getRGB(i, j);
            	
            	//Convert RGB to XYZ
            	double[] XYZA = colorSpaceConverter.RGBtoXYZ(getRed(rgbA), getGreen(rgbA), getBlue(rgbA));
            	double[] XYZB = colorSpaceConverter.RGBtoXYZ(getRed(rgbB), getGreen(rgbB), getBlue(rgbB));
            	
            	//Store Values
            	L_imgA[count] = XYZA[0];	A_imgA[count] = XYZA[1]; 	B_imgA[count] = XYZA[2];
            	L_imgB[count] = XYZB[0]; 	A_imgB[count] = XYZB[1]; 	B_imgB[count] = XYZB[2];
    			count++;
            }
        }
        
        //Analyze Color of imgA + imgB: Calculate Mean & SD for channels l,a,b
        mean_X_imgA = mean(L_imgA); mean_Y_imgA = mean(A_imgA); mean_Z_imgA = mean(B_imgA);
        sd_X_imgA = calculateSD(L_imgA); sd_Y_imgA = calculateSD(A_imgA); sd_Z_imgA = calculateSD(B_imgA);
        
        mean_X_imgB = mean(L_imgB); mean_Y_imgB = mean(A_imgB); mean_Z_imgB = mean(B_imgB);
        sd_X_imgB = calculateSD(L_imgB); sd_Y_imgB = calculateSD(A_imgB); sd_Z_imgB = calculateSD(B_imgB);
        
        //Copy Color Pofile from Image A to B
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
            	
            	int rgbA = imgA.getRGB(i, j);
            	int rgbB = imgB.getRGB(i, j);

            	//Convert RGB to XYZ
            	double[] XYZA = colorSpaceConverter.RGBtoXYZ(getRed(rgbA), getGreen(rgbA), getBlue(rgbA));
            	double[] XYZB = colorSpaceConverter.RGBtoXYZ(getRed(rgbB), getGreen(rgbB), getBlue(rgbB));
            	
            	//Offset XYZ value: valueB - meanB * SDa / SDb
            	double lOffset = (XYZB[0]-mean_X_imgB)*(sd_X_imgA/sd_X_imgB);
            	double aOffset = (XYZB[1]-mean_Y_imgB)*(sd_Y_imgA/sd_Y_imgB);
            	double bOffset = (XYZB[2]-mean_Z_imgB)*(sd_Z_imgA/sd_Z_imgB);
            	
            	//adjust XYZ value by offset
            	XYZB[0] = mean_X_imgB+lOffset;
            	XYZB[1] = mean_Y_imgB+aOffset;
            	XYZB[2] = mean_Z_imgB+bOffset;
            	
            	int rgbOut[] = colorSpaceConverter.XYZtoRGB(XYZB);
            	outImg.setRGB(i, j, new Color(clipChannelValue(rgbOut[0]), clipChannelValue(rgbOut[1]), clipChannelValue(rgbOut[2])).getRGB());
            }
        }
        
        
    	return outImg;
    }
    

    
    // ===================================================================== helpers ==================================================================== //
    public static int getRed(int rgb) {  return new Color(rgb).getRed();  }

    public static int getGreen(int rgb) {  return new Color(rgb).getGreen();   }

    public static int getBlue(int rgb) {  return new Color(rgb).getBlue();  }

    public static float getBrightness (int rgb) {
 		int r = getRed(rgb); int g = getGreen(rgb); int b = getBlue(rgb);
 		float [] hsb = Color.RGBtoHSB(r, g, b, null);
 		return hsb[2];
 	}
    
    public static int clipChannelValue(int v) {
        v = v > 255 ? 255 : v;
        v = v < 0 ? 0 : v;
        return v;
    }
    
    //Map number x from Range A-B to Range C-D
    public static float mapNumber (float x, float a, float b, float c, float d) {
    	return (x-a)/(b-a) * (d-c) + c;
    }
    
    //Source: https://stackoverflow.com/questions/4191687/how-to-calculate-mean-median-mode-and-range-from-a-set-of-numbers
    public static double mean(double[] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }
    
    //Source: https://www.programiz.com/java-programming/examples/standard-deviation
    public static double calculateSD(double numArray[])
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for(double num : numArray) {
            sum += num;
        }

        double mean = sum/length;

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/length);
    }
	
}
