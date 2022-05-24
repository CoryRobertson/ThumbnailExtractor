package com.github.coryrobertson.ThumbnailExtractor;

import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.*;
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

        ArgumentParser parser = ArgumentParsers.newFor("./ThumbnailExtractor").build()
                .defaultHelp(true)
                .description("Generate thumbnail for mp4 and mkv file recursively");
        parser.addArgument("-r")
                .action(storeTrue())
                .setDefault(false)
                .help("Force replacing thumbnails when found")
                .required(false);
        parser.addArgument("-rm")
                .action(storeTrue())
                .setDefault(false)
                .help("Remove all thumbnails, instead of creating them, if they exist")
                .required(false);
        parser.addArgument("-d")
                .required(true)
                .help("Directory to search for video files");
        parser.addArgument("-f")
                .required(false)
                .setDefault("60")
                .help("The frame number to extract from the video file");
        parser.addArgument("-p")
                .required(false)
                .setDefault("-1.0")
                .help("Used instead of using -f, allows you to pick a percentage of video length to be used to generate thumbnails");
        parser.addArgument("-n")
                .required(false)
                .setDefault("")
                .help("Filename to output, if not specified, the video file is the file name");

        Namespace ns = null;

        try
        {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e)
        {
            parser.handleError(e);
            System.exit(0);
        }

        // acquire all args from argparse
        boolean forceReplace = !((boolean) ns.getAttrs().get("r"));
        boolean remove = (boolean) ns.getAttrs().get("rm");
        boolean usingForceName = ns.getAttrs().get("n") != "";
        String forceName = (String) ns.getAttrs().get("n");
        String pathToSearch = (String) ns.getAttrs().get("d");
        int frameNumberExtract =  Integer.parseInt((String) ns.getAttrs().get("f"));
        boolean usingPercentage = ns.getAttrs().get("p") != "-1.0";
        double percentage = Double.parseDouble((String) ns.getAttrs().get("p"));


        // find all files recursively
        Collection<File> files = FileUtils.listFiles(new File(pathToSearch), new RegexFileFilter(".+(mkv|mp4)"), DirectoryFileFilter.DIRECTORY);

        File[] files_ = files.toArray(new File[0]); // convert to a nice pretty array :)
        for(int i = 0; i < files_.length; i++)
        {
            String path = files_[i].getPath();

            String fileName = files_[i].getName();
            fileName = fileName.substring(0,fileName.length() - 4); // remove file extension

            String progress_ = (i+1) + "/" + files_.length;

            int index = path.indexOf(".mp4"); // search for .mp4 first
            if(index == -1) // mp4 was not found
            {
                index = path.indexOf(".mkv"); // search for .mkv
                if(index == -1)// mkv was not found? this shouldn't be able to happen, but we continue just incase
                {
                    System.exit(1);
                    continue; // somehow a file was found using the regex and still had no mp4 or mkv inside its filename
                }
            }
            String outputPath;

            if(!usingForceName)
            {
                outputPath = path.substring(0, index) + ".jpg";
            } else
            {
                outputPath = files_[i].getParent() + "/" + forceName;

            }

            File skip = new File(files_[i].getParent() + "/" + fileName + ".jpg");

            if(!skip.getAbsoluteFile().exists())
            {
                skip = new File(files_[i].getParent() + "/" + forceName);
            }

            if((skip.getAbsoluteFile().exists() && forceReplace) || remove)
            {

                if(remove && skip.delete())
                {
                    System.out.println("[" + progress_ + "]" + " Thumbnail found, removing it: " + outputPath);

                }
                else
                {
                    System.out.println("[" + progress_ + "]" + " Thumbnail already existed, skipping: " + path);

                }
                continue;
            }

            System.out.println("[" + progress_ + "] Extracting frame from: " + path +  " --> " + outputPath);

            try
            {
                if(!usingPercentage) {
                    extractFrameFromVideo(path, outputPath, frameNumberExtract);
                }
                else
                {
                    extractFramePercentFromVideo(path,outputPath,percentage);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.err.println("Unable to output file, missing permissions?");
            }
            catch (JCodecException e)
            {
                e.printStackTrace();
                System.err.println("Error bad codec for video file?");
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
                System.err.println("Bad or invalid data when reading file, skipping.");
            }
        }
        System.out.println("Finished extracting thumbnails, thumbnails created: " + (files_.length));
    }

    private static void extractFrameFromVideo(String videoPath, String outputPath, int frameNumber) throws JCodecException, IOException
    {
        Picture frame = FrameGrab.getFrameFromFile(new File(videoPath), frameNumber);
        RenderedImage renderedImage = AWTUtil.toBufferedImage(frame);
        ImageIO.write(renderedImage, "jpg", new File(outputPath));
    }

    private static void extractFramePercentFromVideo(String videoPath, String outputPath, double percentage) throws JCodecException, IOException
    {
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
}
