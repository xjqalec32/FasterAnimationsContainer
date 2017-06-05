package com.tigerlee.libs;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.widget.ImageView;

import java.lang.ref.SoftReference;
import java.util.ArrayList;


public class FasterAnimationsContainer {

    private class AnimationFrame{

        private int mResourceId;
        private int mDuration;

        AnimationFrame(int resourceId, int duration){
            mResourceId = resourceId;
            mDuration = duration;
        }

        int getResourceId() {
            return mResourceId;
        }
        int getDuration() {
            return mDuration;
        }
    }

    private ArrayList<AnimationFrame> mAnimationFrames = new ArrayList<>();
    private int mIndex;

    private boolean mIsRunning;

    private SoftReference<ImageView> mSoftReferenceImageView;
    private Handler mHandler;

    private Bitmap mRecycleBitmap;

    public FasterAnimationsContainer(ImageView imageView) {
        mSoftReferenceImageView = new SoftReference<>(imageView);
        mHandler = new Handler();
    }

    public FasterAnimationsContainer init(int[] resIds, int interval){
        mAnimationFrames.clear();

        for(int resId : resIds){
            mAnimationFrames.add(new AnimationFrame(resId, interval));
        }

        mIndex = -1;

//        mRecycleBitmap = null;

        return this;
    }

    public synchronized void start() {
        if (mIsRunning) return;
        mIsRunning = true;
        mHandler.post(new FramesSequenceAnimation());
    }

    private AnimationFrame getNext() {
        mIndex++;
        if (mIndex >= mAnimationFrames.size()) {
            mIndex = -1;
            return null;
        }
        return mAnimationFrames.get(mIndex);
    }

    private class FramesSequenceAnimation implements Runnable {

        @Override
        public void run() {
            ImageView imageView = mSoftReferenceImageView.get();
            if (null == imageView) {
                mIsRunning = false;
                return;
            }

            AnimationFrame frame = getNext();
            if (null == frame) {
                mIsRunning = false;
                return;
            }

            GetImageDrawableTask task = new GetImageDrawableTask(imageView, imageView.getResources());
            task.execute(frame.getResourceId());

            mHandler.postDelayed(this, frame.getDuration());
        }
    }

    private class GetImageDrawableTask extends AsyncTask<Integer, Void, Drawable> {

        private Resources resources;
        private ImageView mImageView;

        public GetImageDrawableTask(ImageView imageView, Resources resources) {
            mImageView = imageView;
            this.resources = resources;
        }

        @SuppressLint("NewApi")
        @Override
        protected Drawable doInBackground(Integer... params) {
            BitmapDrawable drawable = null;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                if (mRecycleBitmap != null){
                    options.inBitmap = mRecycleBitmap;
                }
                options.inSampleSize = 1;
                mRecycleBitmap = BitmapFactory.decodeResource(resources, params[0], options);
                drawable = new BitmapDrawable(resources, mRecycleBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(Drawable result) {
            if(null != result) mImageView.setImageDrawable(result);
        }
    }
}
