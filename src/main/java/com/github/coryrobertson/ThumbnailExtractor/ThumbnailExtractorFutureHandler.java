package com.github.coryrobertson.ThumbnailExtractor;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.*;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThumbnailExtractorFutureHandler
{
    private ExecutorService executor = Executors.newFixedThreadPool(12);

    private int thumbCount = 1;

    public void stopExecutor()
    {
        executor.shutdown();
    }

    private void extractFramePercentFromVideo(String videoPath, String outputPath, double percentage) throws IOException, JCodecException {
        File file = new File(videoPath);
        Format f = JCodecUtil.detectFormat(file);
        Demuxer d = JCodecUtil.createDemuxer(f, file); // thank you stack overflow <3 https://github.com/jcodec/jcodec/issues/168
        DemuxerTrack vt = d.getVideoTracks().get(0);
        DemuxerTrackMeta dtm = vt.getMeta();

        int nFrames = dtm.getTotalFrames();
        //int fps = (int)(nFrames / dtm.getTotalDuration());

        double frameNumber = percentage * nFrames;

        Picture frame = FrameGrab.getFrameFromFile(new File(videoPath), (int) Math.floor(frameNumber));
        RenderedImage renderedImage = AWTUtil.toBufferedImage(frame);
        ImageIO.write(renderedImage, "jpg", new File(outputPath));
    }

    private static void extractFrameFromVideo(String videoPath, String outputPath, int frameNumber) throws JCodecException, IOException
    {
        Picture frame = FrameGrab.getFrameFromFile(new File(videoPath), frameNumber);
        RenderedImage renderedImage = AWTUtil.toBufferedImage(frame);
        ImageIO.write(renderedImage, "jpg", new File(outputPath));
    }

    public Future<Boolean> extractFrameVideo(String videoPath, String outputPath, int frameNumber)
    {
        return executor.submit(() ->
        {
            try {
                extractFrameFromVideo(videoPath, outputPath, frameNumber);
            } catch (JCodecException e) {
                System.err.println("Error bad codec for video file?");
                System.err.println(videoPath + " -> " + outputPath);
                return false;
            } catch (IOException e) {
                System.err.println("Unable to output file, missing permissions?");
                System.err.println(videoPath + " -> " + outputPath);

                return false;
            }
            System.out.println("[" + thumbCount + "/" + ThumbnailExtractor.thumbNailTotal + "] Extracting frame from: " + videoPath +  " --> " + outputPath);
            thumbCount++;

            return true;
        });
    }

    public Future<Boolean> extractFramePercent(String videoPath, String outputPath, double percentage)
    {
        return executor.submit(() ->
        {
            try {
                extractFramePercentFromVideo(videoPath,outputPath,percentage);
            } catch (JCodecException e) {
                System.err.println("Error bad codec for video file?");
                System.err.println(videoPath + " -> " + outputPath);
                return false;
            } catch (IOException e) {
                System.err.println("Unable to output file, missing permissions?");
                System.err.println(videoPath + " -> " + outputPath);

                return false;
            }
            System.out.println("[" + thumbCount + "/" + ThumbnailExtractor.thumbNailTotal + "] Extracting frame from: " + videoPath +  " --> " + outputPath);
            thumbCount++;
            return true;
        });
    }
}
