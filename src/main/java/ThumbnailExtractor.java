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

    public static void main(String []args)
    {
        String pathToSearch;

        Collection<File> files;

        if(args.length >= 1)
        {
            pathToSearch = args[0];
            files = FileUtils.listFiles(new File(pathToSearch), new RegexFileFilter(".+(mkv|mp4)"), DirectoryFileFilter.DIRECTORY);
        }
        else
        {
            files = FileUtils.listFiles(new File("./"), new RegexFileFilter(".+(mkv|mp4)"), DirectoryFileFilter.DIRECTORY);
        }

        File[] files_ = files.toArray(new File[0]); // convert to a nice pretty array :)
        for(int i = 0; i < files_.length; i++)
        {
            String path = files_[i].getPath();
            int index = path.indexOf(".mp4"); // search for .mp4 first
            if(index == -1) // mp4 was not found
            {
                index = path.indexOf(".mkv"); // search for mkv
            }

            String outputPath = path.substring(0,index) + ".png";

            try
            {
                extractFrameFromVideo(path, outputPath, 0);
            }
            catch (JCodecException | IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private static void extractFrameFromVideo(String videoPath, String outputPath, int frameNumber) throws JCodecException, IOException
    {
        Picture frame = FrameGrab.getFrameFromFile(new File(videoPath), frameNumber);
        RenderedImage renderedImage = AWTUtil.toBufferedImage(frame);
        ImageIO.write(renderedImage, "png", new File(outputPath));
    }
}
