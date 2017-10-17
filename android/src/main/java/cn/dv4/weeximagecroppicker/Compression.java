package cn.dv4.weeximagecroppicker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import com.alibaba.fastjson.JSONObject;
import id.zelory.compressor.Compressor;

import java.io.File;
import java.io.IOException;

public class Compression {

    public File compressImage(final Activity activity, final JSONObject options, final String originalImagePath) throws IOException {
        Integer maxWidth = options.containsKey("compressImageMaxWidth") ? options.getInteger("compressImageMaxWidth") : null;
        Integer maxHeight = options.containsKey("compressImageMaxHeight") ? options.getInteger("compressImageMaxHeight") : null;
        Double quality = options.containsKey("compressImageQuality") ? options.getDouble("compressImageQuality") : null;

        if (maxWidth == null && maxHeight == null && quality == null) {
            Log.d("image-crop-picker", "Skipping image compression");
            return new File(originalImagePath);
        }

        Log.d("image-crop-picker", "Image compression activated");
        Compressor compressor = new Compressor(activity)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).getAbsolutePath());

        if (quality == null) {
            Log.d("image-crop-picker", "Compressing image with quality 100");
            compressor.setQuality(100);
        } else {
            Log.d("image-crop-picker", "Compressing image with quality " + (quality * 100));
            compressor.setQuality((int) (quality * 100));
        }

        if (maxWidth != null) {
            Log.d("image-crop-picker", "Compressing image with max width " + maxWidth);
            compressor.setMaxWidth(maxWidth);
        }

        if (maxHeight != null) {
            Log.d("image-crop-picker", "Compressing image with max height " + maxHeight);
            compressor.setMaxHeight(maxHeight);
        }

        File image = new File(originalImagePath);

        String[] paths = image.getName().split("\\.(?=[^\\.]+$)");
        String compressedFileName = paths[0] + "-compressed";

        if (paths.length > 1)
            compressedFileName += "." + paths[1];

        return compressor.compressToFile(image, compressedFileName);
    }

    public String compressVideo(final Activity activity, final JSONObject options, final String originalVideo, final String compressedVideo) {
        // TODO: 官方并未实现视频压缩，因为ffmpeg太慢并且需要licence
        return originalVideo;
    }
}