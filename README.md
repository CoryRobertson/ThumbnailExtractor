# ThumbnailExtractor
A tool that allows you to give a directory of video files, and the program will add thumnails next to each one :)

###### Build and Use instructions
```bash
git clone https://github.com/CoryRobertson/ThumbnailExtractor.git
cd ThumbnailExtractor
chmod +x ./gradlew
./gradlew installDist
mv ./build/install/ThumbnailExtractor/* .
cd bin
./ThumbnailExtractor -h
./ThumbnailExtractor -d <path to directory to search e.g. ./path/to/folder/>

```


