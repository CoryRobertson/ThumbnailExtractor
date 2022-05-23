package com.github.coryrobertson.ThumbnailExtractor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class ThumbnailExtractor
{

    //TODO: make a bash script that downloads the git repo, then builds it using ./gradlew installDist, and finally copies the distribution to the top level folder

    public static void main(String []args)
    {

        String pathToSearch;

        Collection<File> files = null;

        int frameNumberExtract = 0;

        if(args.length >= 2)
        {
            pathToSearch = args[0];
            frameNumberExtract = Integer.parseInt(args[1]);
            files = FileUtils.listFiles(new File(pathToSearch), new RegexFileFilter(".+(mkv|mp4)"), DirectoryFileFilter.DIRECTORY);
        }
        else
        {
            System.out.println("\nRun this program with arguments: <directory to search> <frame number to extract>");
            System.out.println("<directory to search> is the directory the program will recursively search for files");
            System.out.println("<frame number to extract> is the frame in each video that will be used as a thumbnail");
            System.out.println("At the moment, the program only searches for mp4 files and mkv files");

            System.exit(0);
        }



        File[] files_ = files.toArray(new File[0]); // convert to a nice pretty array :)
        for(int i = 0; i < files_.length; i++)
        {
            String path = files_[i].getPath();

            String fileName = files_[i].getName();
            fileName = fileName.substring(0,fileName.length() - 4); // remove file extension
//            double progress = ((double)(i+1) / (double)files_.length) * 100;
//            String progress_ = String.format("%.2f",progress);
            String progress_ = (i+1) + "/" + files_.length;

            int index = path.indexOf(".mp4"); // search for .mp4 first
            if(index == -1) // mp4 was not found
            {
                index = path.indexOf(".mkv"); // search for .mkv
            }

            String outputPath = path.substring(0,index) + ".png";

            File skip = new File(files_[i].getParent() + "\\" + fileName + ".png");
            if(skip.exists())
            {
                System.out.println("[" + progress_ + "]" + " Thumbnail already existed, skipping: " + path);
                continue;
            }

            System.out.println("[" + progress_ + "] Extracting frame from: " + path +  " --> " + outputPath);

            try
            {
                extractFrameFromVideo(path, outputPath, frameNumberExtract);
            }
            catch (JCodecException | IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Finished extracting thumbnails, thumbnails created: " + (files_.length));
    }

    private static void extractFrameFromVideo(String videoPath, String outputPath, int frameNumber) throws JCodecException, IOException
    {
        Picture frame = FrameGrab.getFrameFromFile(new File(videoPath), frameNumber);
        RenderedImage renderedImage = AWTUtil.toBufferedImage(frame);
        ImageIO.write(renderedImage, "png", new File(outputPath));
    }
}
