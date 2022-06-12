package com.github.coryrobertson.ThumbnailExtractor;

import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

public class ThumbnailExtractor
{

    public static int thumbNailTotal;

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

        ThumbnailExtractorFutureHandler thumb = new ThumbnailExtractorFutureHandler();

        // find all files recursively
        Collection<File> files = FileUtils.listFiles(new File(pathToSearch), new RegexFileFilter(".+(mkv|mp4)"), DirectoryFileFilter.DIRECTORY);

        ArrayList<Future<Boolean>> futureList = new ArrayList<>();

        File[] files_ = files.toArray(new File[0]); // convert to a nice pretty array :)

        int thumbGenCount = 0;
        thumbNailTotal = files_.length;
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

            // Here we generate our output path but taking off the file extension which is most likely .mp4, and appending .jpg to it.
            String outputPath;
            if(!usingForceName)
            {
                outputPath = path.substring(0, index) + ".jpg";
            } else
            {
                outputPath = files_[i].getParent() + "/" + forceName;

            }

            // This block here is for checking if the thumbnail exists already
            File skip = new File(files_[i].getParent() + "/" + fileName + ".jpg");
            if(!skip.getAbsoluteFile().exists() && !forceName.isEmpty())
            {
                skip = new File(files_[i].getParent() + "/" + forceName);
            }

            // if we need to remove the file we enter this if statement, same for if we need to replace thumbnails
            if((skip.getAbsoluteFile().exists() && forceReplace) || remove)
            {

                // this check implicitly deletes the file, spooky code :)
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

//            System.out.println("[" + progress_ + "] Extracting frame from: " + path +  " --> " + outputPath);

            // This try block adds futures to an arraylist for later use
            try
            {
                if(!usingPercentage)
                {
                    var a = thumb.extractFrameVideo(path, outputPath, frameNumberExtract);
                    futureList.add(a);
                    thumbGenCount++;
                }
                else
                {
                    var a = thumb.extractFramePercent(path,outputPath,percentage);
                    futureList.add(a);
                    thumbGenCount++;
                }
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
                System.err.println("Bad or invalid data when reading file, skipping.");
            }

        }
        boolean complete = false;

        while(!complete)
        {
            int i = 0;
            complete = true;

            for (var future : futureList) {
                if(!future.isDone())
                {
                    complete = false;
                }
                else
                {
                    i++;
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
//            System.out.println("working, " + i + " number of threads complete");

        }

        System.out.println("Finished extracting thumbnails, thumbnails created: " + (thumbGenCount));
        thumb.stopExecutor();
    }




}
