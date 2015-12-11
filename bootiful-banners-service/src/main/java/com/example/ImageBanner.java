package com.example;/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Banner implementation that prints ASCII art generated from an image file
 *
 * @author Craig Burke
 * @author Christophe Bornet
 */
public class ImageBanner {

    private static final double RED_WEIGHT = 0.2126d;
    private static final double GREEN_WEIGHT = 0.7152d;
    private static final double BLUE_WEIGHT = 0.0722d;

    private File image;
    private Map<String, Color> colors = new HashMap<String, Color>();

    public ImageBanner(File image) {
        if (image == null || !image.exists()) {
            throw new RuntimeException("Image not found !");
        }
        this.image = image;
        colorsInit();
    }

    private void colorsInit() {
        this.colors.put("BLACK", new Color(0, 0, 0));
        this.colors.put("RED", new Color(170, 0, 0));
        this.colors.put("GREEN", new Color(0, 170, 0));
        this.colors.put("YELLOW", new Color(170, 85, 0));
        this.colors.put("BLUE", new Color(0, 0, 170));
        this.colors.put("MAGENTA", new Color(170, 0, 170));
        this.colors.put("CYAN", new Color(0, 170, 170));
        this.colors.put("WHITE", new Color(170, 170, 170));

        this.colors.put("BRIGHT_BLACK", new Color(85, 85, 85));
        this.colors.put("BRIGHT_RED", new Color(255, 85, 85));
        this.colors.put("BRIGHT_GREEN", new Color(85, 255, 85));
        this.colors.put("BRIGHT_YELLOW", new Color(255, 255, 85));
        this.colors.put("BRIGHT_BLUE", new Color(85, 85, 255));
        this.colors.put("BRIGHT_MAGENTA", new Color(255, 85, 255));
        this.colors.put("BRIGHT_CYAN", new Color(85, 255, 255));
        this.colors.put("BRIGHT_WHITE", new Color(255, 255, 255));
    }

    public String printBanner(Integer maxWidth, Double aspectRatio, boolean invert) {
        String headlessProperty = System.getProperty("java.awt.headless");
        String banner = "";
        try {
            System.setProperty("java.awt.headless", "true");
            BufferedImage sourceImage = ImageIO.read(new FileInputStream(this.image));
            BufferedImage resizedImage = resizeImage(sourceImage, maxWidth, aspectRatio);
            banner = imageToBanner(resizedImage, invert);
        }
        catch (Exception ex) {
            System.out.println("WARNING ! Image banner not printable: " + this.image + " (" + ex.getClass()
                    + ": '" + ex.getMessage() + "')");
            ex.printStackTrace();
        }
        finally {
            if(headlessProperty != null) {
                System.setProperty("java.awt.headless", headlessProperty);
            }
        }
        return banner;
    }

    private String imageToBanner(BufferedImage image, boolean dark) {
        StringBuilder banner = new StringBuilder();

        for (int y = 0; y < image.getHeight(); y++) {
            if (dark) {
                banner.append("${AnsiBackground.BLACK}");
            }
            else {
                banner.append("${AnsiBackground.DEFAULT}");
            }
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), false);
                banner.append(getFormatString(color, dark));
            }
            if (dark) {
                banner.append("${AnsiBackground.DEFAULT}");
            }
            banner.append("${AnsiColor.DEFAULT}\n");
        }

        return banner.toString();
    }

    protected String getFormatString(Color color, boolean dark) {
        String matchedColorName = null;
        Double minColorDistance = null;

        for (Entry<String, Color> colorOption : this.colors.entrySet()) {
            double distance = getColorDistance(color, colorOption.getValue());

            if (minColorDistance == null || distance < minColorDistance) {
                minColorDistance = distance;
                matchedColorName = colorOption.getKey();
            }
        }

        return "${AnsiColor." + matchedColorName + "}" + getAsciiCharacter(color, dark);
    }

    private static int getLuminance(Color color, boolean inverse) {
        double red = color.getRed();
        double green = color.getGreen();
        double blue = color.getBlue();

        double luminance;

        if (inverse) {
            luminance = (RED_WEIGHT * (255.0d - red)) + (GREEN_WEIGHT * (255.0d - green))
                    + (BLUE_WEIGHT * (255.0d - blue));
        }
        else {
            luminance = (RED_WEIGHT * red) + (GREEN_WEIGHT * green)
                    + (BLUE_WEIGHT * blue);
        }

        return (int) Math.ceil((luminance / 255.0d) * 100);
    }

    private static char getAsciiCharacter(Color color, boolean dark) {
        double luminance = getLuminance(color, dark);

        if (luminance >= 90) {
            return ' ';
        }
        else if (luminance >= 80) {
            return '.';
        }
        else if (luminance >= 70) {
            return '*';
        }
        else if (luminance >= 60) {
            return ':';
        }
        else if (luminance >= 50) {
            return 'o';
        }
        else if (luminance >= 40) {
            return '&';
        }
        else if (luminance >= 30) {
            return '8';
        }
        else if (luminance >= 20) {
            return '#';
        }
        else {
            return '@';
        }
    }

    private static double getColorDistance(Color color1, Color color2) {
        double redDelta = (color1.getRed() - color2.getRed()) * RED_WEIGHT;
        double greenDelta = (color1.getGreen() - color2.getGreen()) * GREEN_WEIGHT;
        double blueDelta = (color1.getBlue() - color2.getBlue()) * BLUE_WEIGHT;

        return Math.pow(redDelta, 2.0d) + Math.pow(greenDelta, 2.0d)
                + Math.pow(blueDelta, 2.0d);
    }

    private static BufferedImage resizeImage(BufferedImage sourceImage, int maxWidth,
            double aspectRatio) {
        int width;
        double resizeRatio;
        if (sourceImage.getWidth() > maxWidth) {
            resizeRatio = (double) maxWidth / (double) sourceImage.getWidth();
            width = maxWidth;
        }
        else {
            resizeRatio = 1.0d;
            width = sourceImage.getWidth();
        }

        int height = (int) (Math.ceil(resizeRatio * aspectRatio
                * (double) sourceImage.getHeight()));
        Image image = sourceImage.getScaledInstance(width, height, Image.SCALE_DEFAULT);

        BufferedImage resizedImage = new BufferedImage(image.getWidth(null),
                image.getHeight(null), BufferedImage.TYPE_INT_RGB);

        resizedImage.getGraphics().drawImage(image, 0, 0, null);
        return resizedImage;
    }
}
