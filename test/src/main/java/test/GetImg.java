package test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class GetImg {
    // 测试
    public static void main(String[] args) {
        try {
            int[] size = getImageSize("https://cdn.metaxsire.com/xfan/Ayumi/video/6_videosync_4_Ayumi_1.jpg");
            System.out.println("宽度: " + size[0] + "px");
            System.out.println("高度: " + size[1] + "px");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int[] getImageSize(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        BufferedImage image = ImageIO.read(url);

        if (image == null) {
            throw new IOException("无法读取图片: " + imageUrl);
        }

        return new int[]{image.getWidth(), image.getHeight()};
    }
}
