/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.demo;

import android.app.Application;
import com.google.android.exoplayer2.offline.DownloadAction.Deserializer;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.offline.ProgressiveDownloadAction;
import com.google.android.exoplayer2.source.dash.offline.DashDownloadAction;
import com.google.android.exoplayer2.source.hls.offline.HlsDownloadAction;
import com.google.android.exoplayer2.source.smoothstreaming.offline.SsDownloadAction;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import java.io.File;

/**
 * Placeholder application to facilitate overriding Application methods for debugging and testing.
 */
public class DemoApplication extends Application {

  private static final String DOWNLOAD_ACTION_FILE = "actions";
  private static final String DOWNLOAD_TRACKER_ACTION_FILE = "tracked_actions";
  private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
  private static final int MAX_SIMULTANEOUS_DOWNLOADS = 2;
  private static final Deserializer[] DOWNLOAD_DESERIALIZERS =
      new Deserializer[] {
        DashDownloadAction.DESERIALIZER,
        HlsDownloadAction.DESERIALIZER,
        SsDownloadAction.DESERIALIZER,
        ProgressiveDownloadAction.DESERIALIZER
      };

  protected String userAgent;

  private File downloadDirectory;
  private Cache downloadCache;
  private DownloadManager downloadManager;
  private DownloadTracker downloadTracker;

  @Override
  public void onCreate() {
    super.onCreate();
    userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
  }

  /** Returns a {@link DataSource.Factory}. */
  public DataSource.Factory buildDataSourceFactory(TransferListener<? super DataSource> listener) {
    DefaultDataSourceFactory upstreamFactory =
        new DefaultDataSourceFactory(this, listener, buildHttpDataSourceFactory(listener));
    return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache());
  }

  /** Returns a {@link HttpDataSource.Factory}. */
  public HttpDataSource.Factory buildHttpDataSourceFactory(
      TransferListener<? super DataSource> listener) {
    return new DefaultHttpDataSourceFactory(userAgent, listener);
  }

  /** Returns whether extension renderers should be used. */
  public boolean useExtensionRenderers() {
    return false;
  }

  public DownloadManager getDownloadManager() {
    initDownloadManager();
    return downloadManager;
  }

  public DownloadTracker getDownloadTracker() {
    initDownloadManager();
    return downloadTracker;
  }

  private synchronized void initDownloadManager() {
    if (downloadManager == null) {
      DownloaderConstructorHelper downloaderConstructorHelper =
          new DownloaderConstructorHelper(
              getDownloadCache(), buildHttpDataSourceFactory(/* listener= */ null));
      downloadManager =
          new DownloadManager(
              downloaderConstructorHelper,
              MAX_SIMULTANEOUS_DOWNLOADS,
              DownloadManager.DEFAULT_MIN_RETRY_COUNT,
              new File(getDownloadDirectory(), DOWNLOAD_ACTION_FILE),
              DOWNLOAD_DESERIALIZERS);
      downloadTracker =
          new DownloadTracker(
              /* context= */ this,
              buildDataSourceFactory(/* listener= */ null),
              new File(getDownloadDirectory(), DOWNLOAD_TRACKER_ACTION_FILE),
              DOWNLOAD_DESERIALIZERS);
      downloadManager.addListener(downloadTracker);
    }
  }

  private synchronized Cache getDownloadCache() {
    if (downloadCache == null) {
      File downloadContentDirectory = new File(getDownloadDirectory(), DOWNLOAD_CONTENT_DIRECTORY);
      downloadCache = new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor());
    }
    return downloadCache;
  }

  private File getDownloadDirectory() {
    if (downloadDirectory == null) {
      downloadDirectory = getExternalFilesDir(null);
      if (downloadDirectory == null) {
        downloadDirectory = getFilesDir();
      }
    }
    return downloadDirectory;
  }

  private static CacheDataSourceFactory buildReadOnlyCacheDataSource(
      DefaultDataSourceFactory upstreamFactory, Cache cache) {
    return new CacheDataSourceFactory(
        cache,
        upstreamFactory,
        new FileDataSourceFactory(),
        /* cacheWriteDataSinkFactory= */ null,
        CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
        /* eventListener= */ null);
  }
}
