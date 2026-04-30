package com.example.app_xhinh_anh.processing.ai;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundRemoverAi implements AiProcessor {

    private final SubjectSegmenter segmenter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public BackgroundRemoverAi() {
        SubjectSegmenterOptions options = new SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build();
        segmenter = SubjectSegmentation.getClient(options);
    }

    @Override
    public void process(Bitmap input, AiCallback callback) {
        InputImage image = InputImage.fromBitmap(input, 0);

        segmenter.process(image)
                .addOnSuccessListener(new OnSuccessListener<SubjectSegmentationResult>() {
                    @Override
                    public void onSuccess(SubjectSegmentationResult result) {
                        executor.execute(() -> {
                            try {
                                Bitmap foreground = result.getForegroundBitmap();
                                if (foreground != null) {
                                    mainHandler.post(() -> callback.onSuccess(foreground));
                                } else {
                                    mainHandler.post(() -> callback.onError(new Exception("Không tìm thấy chủ thể để tách nền")));
                                }
                            } catch (Exception e) {
                                mainHandler.post(() -> callback.onError(e));
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        callback.onError(e);
                    }
                });
    }
}
