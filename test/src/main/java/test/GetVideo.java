package test;

import javafx.scene.media.MediaPlayer;
import org.bytedeco.javacv.FFmpegFrameGrabber;

import javax.print.attribute.standard.Media;
import java.time.Duration;

public class GetVideo {
    public static void main(String[] args) {
        String videoUrl = "https://cdn.metaxsire.com/xfan/Elsa/video/2_videosync_8_Elsa_1.mp4";
        try {
            long duration = getVideoDuration(videoUrl);
            System.out.println("视频时长: " + duration + " 毫秒");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getVideoDuration(String videoUrl) throws Exception {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoUrl);
        try {
            grabber.start();
            return grabber.getLengthInTime() / 1000; // 转换为毫秒
        } finally {
            grabber.stop();
            grabber.release();
        }
    }
}
