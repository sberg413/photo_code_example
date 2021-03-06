package com.lightcyclesoftware.photoscodeexample.ui;


import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lightcyclesoftware.photoscodeexample.R;
import com.lightcyclesoftware.photoscodeexample.model.DataModel;
import com.lightcyclesoftware.photoscodeexample.service.PhotosApiManager;
import com.wang.avi.AVLoadingIndicatorView;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PhotosFragment extends Fragment {
    public static final String TAG = "PhotosFragment";
    public static final int RECORDS_PER_QUERY = 48;
    public static final float LOADING_POINT = 0.5f;
    private List<DataModel.Record> recordList = new ArrayList<>();
    private PhotosAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private int page = 0;
    private boolean lastPageLoaded;
    private PhotoInfiniteScrollListener mInfiniteScrollListener;
    private boolean hasScrolled;

    @BindView(R.id.photo_list)
    public RecyclerView mRecyclerView;

    @BindView(R.id.loading_indicator)
    public AVLoadingIndicatorView mAVLoadingIndicatorView;

    @BindView(R.id.scrollng_loading_indicator)
    public AVLoadingIndicatorView mScrollingAVLoadingIndicatorView;

    public PhotosFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        PhotosApiManager.getInstance().getCookie(getActivity()).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::onNext
                        ,this::onFailure
                        ,this::onSuccess);

        mAdapter = new PhotosAdapter(recordList);
        mLayoutManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
        mInfiniteScrollListener = createInfiniteScrollListener();
        mRecyclerView.addOnScrollListener(mInfiniteScrollListener);
    }

    private PhotoInfiniteScrollListener createInfiniteScrollListener() {
        return new PhotoInfiniteScrollListener(RECORDS_PER_QUERY, LOADING_POINT, mLayoutManager) {
            @Override public void onScrolledToEnd(final int firstVisibleItemPosition) {
                if (!hasScrolled && !lastPageLoaded) {
                    try {
                        hasScrolled = true;
                        mScrollingAVLoadingIndicatorView.setVisibility(View.VISIBLE);
                        PhotosApiManager.getInstance().getPhotos(getActivity(), page)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(PhotosFragment.this::onPhotosNext
                                        , PhotosFragment.this::onPhotosFailure
                                        , PhotosFragment.this::onPhotosSuccess);
                    }catch(UnsupportedEncodingException e){
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private void onNext(Object object){

    }

    private void onFailure(@NonNull Throwable thowable) {
        System.out.println(thowable.getMessage());
        mAVLoadingIndicatorView.setVisibility(View.GONE);
    }

    private void onPhotosNext(DataModel dataModel){
        if (dataModel.getData()
                .getAlbum()
                .getPhotos()
                .getRecords().size() == 0) {
            lastPageLoaded = true;
        } else {
            recordList.addAll(dataModel.getData()
                    .getAlbum()
                    .getPhotos()
                    .getRecords());
        }
    }

    private void onPhotosFailure(@NonNull Throwable thowable) {
        System.out.println(thowable.getMessage());
        hasScrolled = false;
        mScrollingAVLoadingIndicatorView.setVisibility(View.GONE);
        mAVLoadingIndicatorView.setVisibility(View.GONE);
    }

    private void onPhotosSuccess(){
        Log.d(TAG,"# of photos = " + Integer.toString(recordList.size()));
        mAdapter.notifyDataSetChanged();
        mRecyclerView.scrollToPosition(page * RECORDS_PER_QUERY + RECORDS_PER_QUERY);
        mScrollingAVLoadingIndicatorView.setVisibility(View.GONE);
        mAVLoadingIndicatorView.setVisibility(View.GONE);
        page++;
        hasScrolled = false;
    }

    private void onSuccess(){
        try {
            PhotosApiManager.getInstance().getPhotos(getActivity(), page)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onPhotosNext
                            ,this::onPhotosFailure
                            ,this::onPhotosSuccess);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
