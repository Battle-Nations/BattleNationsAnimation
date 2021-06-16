package util;

import bn.GameFiles;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Z2RawExporter {

    public Z2RawExporter() {
        GameFiles.init();
    }

    public void exportAssets(String outPath) throws IOException {
        for (File file : GameFiles.glob("*_0.z2raw")) {
            exportFile(file.getName(), outPath);
        }
    }

    public void exportFile(String name, String outPath) throws IOException {
        LittleEndianInputStream in = new LittleEndianInputStream(GameFiles.open(name));
        try {
            int ver = in.readInt();
            if (ver < 0 || ver > 1)
                throw new FileFormatException("Unrecognized version");
            int width = in.readInt();
            int height = in.readInt();
            int bits = in.readInt();
            BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            if (ver == 0)
                readRaw(im, in, width, height, bits);
            else
                readRLE(im, in, width, height, bits);

            //Export to file
            File outputfile = new File(outPath + "\\" + name + ".png");
            ImageIO.write(im, "png", outputfile);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new FileFormatException("Invalid array index", e);
        }
        finally {
            in.close();
        }
    }

    private void readRaw(BufferedImage im, LittleEndianInputStream in, int width, int height, int bits)
            throws IOException {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                im.setRGB(x, y, readPix(in, bits));
    }

    private void readRLE(BufferedImage im, LittleEndianInputStream in, int width, int height, int bits)
            throws IOException {
        in.readInt(); // length
        int palSize = in.readInt();
        if (palSize < 1 || palSize > 256)
            throw new FileFormatException("Invalid palette size");
        int[] pal = new int[palSize];
        for (int i = 0; i < palSize; i++)
            pal[i] = readPix(in, bits);

        int x = 0;
        int y = 0;
        while (y < height) {
            int c = in.readByte();
            int num = (c >> 1) + 1;
            if ((c & 1) == 0) {
                for (int i = 0; i < num; i++) {
                    im.setRGB(x, y, pal[in.readByte()]);
                    if (++x >= width) { x = 0; y++; }
                }
            }
            else {
                int pix = pal[in.readByte()];
                for (int i = 0; i < num; i++) {
                    im.setRGB(x, y, pix);
                    if (++x >= width) { x = 0; y++; }
                }
            }
        }
    }

    private int readPix(LittleEndianInputStream in, int bits) throws IOException {
        int r, g, b, a;
        if (bits == 4) {
            int p = in.readByte();
            a = (p & 0xf) * 0x11;
            b = ((p >> 4) & 0xf) * 0x11;
            p = in.readByte();
            g = (p & 0xf) * 0x11;
            r = ((p >> 4) & 0xf) * 0x11;
        }
        else {
            r = in.readByte();
            g = in.readByte();
            b = in.readByte();
            a = in.readByte();
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
