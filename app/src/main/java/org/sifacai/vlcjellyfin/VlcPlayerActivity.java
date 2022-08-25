package org.sifacai.vlcjellyfin;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.Timer;
import java.util.TimerTask;

public class VlcPlayerActivity extends BaseActivity implements MediaPlayer.EventListener
        , View.OnClickListener {
    private static final String TAG = "VLC播放器";
    public final int Type_SubtitleTrack = 0;
    public final int Type_AudioTrack = 1;
    public final int Type_Playlist = 2;
    public final int Type_Scale = 3;
    public final int Type_Speed = 4;

    private Activity mActivity;
    private MediaPlayer mediaPlayer;
    private LibVLC libVLC;
    private VLCVideoLayout vlcVideoLayout;

    private RelativeLayout Controller; //总控
    private LinearLayout ControllerTop;
    private LinearLayout ControllerBottom;
    private LinearLayout ControllerBottomTop;
    private LinearLayout ControllerBottomBottom;
    private TextView videoTitle;
    private TextView currTime;
    private TextView countTime;
    private TextView speedBtn;
    private TextView scaleBtn;
    private ImageView preBtn;
    private ImageView nextBtn;
    private ImageView playPauseBtn;
    private ImageView stopBtn;
    private ImageView subTracksBtn;
    private ImageView audioTracksBtn;
    private ImageView playListBtn;
    private ImageView pauseFlag;
    private SeekBar currPostion;

    private Timer progressTime = null;  //控制器进度条更新定时
    private Timer reportProcessTime = null; // 报告进度定时器

    private PopMenu playListMenu = null; //播放列表
    private PopMenu subTrackMenu = null; //字幕菜单
    private PopMenu audioTrackMenu = null; //单轨菜单
    private PopMenu scaleTypeMenu = null; //缩放菜单
    private PopMenu speedMenu = null;    //播放速率菜单

    private float speedRate[] = {0.5f, 1.0f, 1.5f, 2.0f}; //倍速播放列表

    private long currPlaybackTimeTrack = 0;  //当前播放进度
    private int ReportTime = 20; // 报告进度间隔次数
    private int ReportVal = 0;    //累积次数


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vlc_player);

        mActivity = this;
        initVlc();
        initController();
        play();
    }

    /**
     * 初始化播放器
     */
    private void initVlc() {
        vlcVideoLayout = findViewById(R.id.VideoView);
        libVLC = new LibVLC(this);
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(vlcVideoLayout, null, true, false);
        mediaPlayer.setEventListener(this);
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Playing: //媒体打开成功
                Hide();
                pauseFlag.setVisibility(View.GONE);
                Log.d(TAG, "onEvent: Playing");
                ReportPlayState(Utils.ReportType.playing, Utils.playList.get(Utils.playIndex).Id);
                initMenu();
                break;
            case MediaPlayer.Event.Paused: //暂停
                pauseFlag.setVisibility(View.VISIBLE);
                break;
            case MediaPlayer.Event.Stopped:
                Log.d(TAG, "onEvent: Stopped");
                ReportPlayState(Utils.ReportType.stop, Utils.playList.get(Utils.playIndex).Id);
                playNext();
                break;
            case MediaPlayer.Event.Opening:  //媒体打开
                Log.d(TAG, "onEvent: Opening");
                break;
            case MediaPlayer.Event.Buffering: //媒体加载public float getBuffering() 获取加载视频流的进度0-100
                int Buffering = (int) event.getBuffering();
                if (null == alertDialogLoading || !alertDialogLoading.isShowing()) {
                    showLoadingDialog();
                }
                setLoadingText("加载进度：%" + Buffering);
                if (Buffering >= 100) {
                    dismissLoadingDialog();
                }
                break;
            case MediaPlayer.Event.EndReached://媒体播放结束
                Log.d(TAG, "onEvent: EndReached");
                break;
            case MediaPlayer.Event.EncounteredError://媒体播放错误
                Log.d(TAG, "onEvent: EncounteredError");
                break;
            case MediaPlayer.Event.TimeChanged://视频时间变化
                currPlaybackTimeTrack = event.getTimeChanged();
                break;
            case MediaPlayer.Event.PositionChanged://视频总时长的百分比
                break;
            case MediaPlayer.Event.SeekableChanged:
                break;
            case MediaPlayer.Event.PausableChanged:
                Log.d(TAG, "onEvent: PausableChanged");
                break;
            case MediaPlayer.Event.LengthChanged:
                Log.d(TAG, "onEvent: LengthChanged");
                break;
            case MediaPlayer.Event.Vout://当图像输出
                Log.d(TAG, "onEvent: Vout");
                break;
            case MediaPlayer.Event.ESAdded:
            case MediaPlayer.Event.ESDeleted:
            case MediaPlayer.Event.ESSelected:
                Log.d(TAG, "onEvent: ES:" + event.type + ":" + event.getEsChangedType() + ":" + event.getEsChangedID());
                break;
            case MediaPlayer.Event.RecordChanged:
                Log.d(TAG, "onEvent: RecordChanged");
                break;
        }
    }

    /**
     * 初始化控制器
     */
    private void initController() {
        Controller = findViewById(R.id.Controller); // 总布局
        ControllerTop = findViewById(R.id.ControllerTop);
        ControllerBottom = findViewById(R.id.ControllerBottom);
        ControllerBottomTop = findViewById(R.id.ControllerBottomTop);
        ControllerBottomBottom = findViewById(R.id.ControllerBottomBottom);
        videoTitle = findViewById(R.id.videoTitle); // 标题
        currTime = findViewById(R.id.currTime);
        countTime = findViewById(R.id.countTime);
        speedBtn = findViewById(R.id.speedBtn);
        scaleBtn = findViewById(R.id.scaleBtn);
        preBtn = findViewById(R.id.preBtn);
        nextBtn = findViewById(R.id.nextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        stopBtn = findViewById(R.id.stopBtn);
        pauseFlag = findViewById(R.id.pauseFlag);
        currPostion = findViewById(R.id.currPostion);
        preBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        playPauseBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        pauseFlag.setOnClickListener(this);
        currPostion.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                boolean rv = false;
                int keycode = keyEvent.getKeyCode();
                if (keycode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    rv = setTimeOnSeekBar(mediaPlayer.getTime() + (long) (mediaPlayer.getLength() * 0.05));
                } else if (keycode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    rv = setTimeOnSeekBar(mediaPlayer.getTime() - (long) (mediaPlayer.getLength() * 0.05));
                }
                return rv;
            }
        });
    }

    /**
     * 初始化字幕和音轨菜单
     */
    private void initMenu() {
        MediaPlayer.TrackDescription[] subTrackList = mediaPlayer.getSpuTracks();
        MediaPlayer.TrackDescription[] audioTrackList = mediaPlayer.getAudioTracks();

        playListBtn = findViewById(R.id.playListBtn);
        subTracksBtn = findViewById(R.id.subTracksBtn);
        audioTracksBtn = findViewById(R.id.audioTracksBtn);
        if (Utils.playList.size() > 1) {
            initPlayListMenu();
        } else {
            playListBtn.setVisibility(View.GONE);
        }

        if (null != subTrackList && subTrackList.length > 1) {
            initSubTrackMenu(subTrackList);
        } else {
            subTracksBtn.setVisibility(View.GONE);
        }

        if (null != audioTrackList && audioTrackList.length > 1) {
            initAudioTrackMenu(audioTrackList);
        } else {
            audioTracksBtn.setVisibility(View.GONE);
        }

        //初始化缩放键
        scaleBtn.setOnClickListener(this);
        scaleBtn.setText(Utils.getVlcScaleTypeName(mediaPlayer.getVideoScale().name()));
        scaleTypeMenu = new PopMenu(this, scaleBtn);
        MediaPlayer.ScaleType[] scaleTypes = MediaPlayer.ScaleType.values();
        for (int i = 0; i < scaleTypes.length; i++) {
            PopMenu.menu menu = scaleTypeMenu.add(Type_Scale, i, i, Utils.getVlcScaleTypeName(scaleTypes[i].name()));
            final int Si = i;
            menu.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    scaleTypeMenu.dismiss();
                    if (mediaPlayer.getVideoScale() != scaleTypes[Si]) {
                        mediaPlayer.setVideoScale(scaleTypes[Si]);
                        scaleBtn.setText(Utils.getVlcScaleTypeName(mediaPlayer.getVideoScale().name()));
                    }
                }
            });
        }

        //初始化速率键
        speedBtn.setOnClickListener(this);
        speedBtn.setText(String.valueOf(mediaPlayer.getRate()));
        speedMenu = new PopMenu(this, speedBtn);
        for (int i = 0; i < speedRate.length; i++) {
            PopMenu.menu menu = speedMenu.add(Type_Speed, i, i, String.valueOf(speedRate[i]));
            final int Fi = i;
            menu.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    speedMenu.dismiss();
                    if (mediaPlayer.getRate() != speedRate[Fi]) {
                        mediaPlayer.setRate(speedRate[Fi]);
                        speedBtn.setText(String.valueOf(mediaPlayer.getRate()));
                    }
                }
            });
        }

    }

    private void initPlayListMenu() {
        playListMenu = new PopMenu(this, playListBtn); //new PopupMenu(this,playListBtn);
        for (int i = 0; i < Utils.playList.size(); i++) {
            PopMenu.menu m = playListMenu.add(Type_Playlist, i, i, Utils.playList.get(i).Name);
            m.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playListMenu.dismiss();
                    if (m.id != Utils.playIndex) {
                        ReportPlayState(Utils.ReportType.stop,Utils.playList.get(Utils.playIndex).Id);
                        Utils.playIndex = m.id;
                        play();
                    }
                }
            });
        }
        playListBtn.setOnClickListener(this);
    }

    /**
     * 初始化字幕菜单
     *
     * @param subTrackList
     */
    private void initSubTrackMenu(MediaPlayer.TrackDescription[] subTrackList) {
        subTrackMenu = new PopMenu(this, subTracksBtn);
        for (int i = 0; i < subTrackList.length; i++) {
            PopMenu.menu m = subTrackMenu.add(Type_SubtitleTrack, subTrackList[i].id, i, subTrackList[i].name);
            m.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    subTrackMenu.dismiss();
                    if (m.id != mediaPlayer.getSpuTrack()) {
                        mediaPlayer.setSpuTrack(m.id);
                    }
                }
            });
        }
        subTracksBtn.setOnClickListener(this);
    }

    /**
     * 初始化音轨菜单
     *
     * @param audioTrackList
     */
    private void initAudioTrackMenu(MediaPlayer.TrackDescription[] audioTrackList) {
        audioTrackMenu = new PopMenu(this, audioTracksBtn);
        for (int i = 0; i < audioTrackList.length; i++) {
            PopMenu.menu m = audioTrackMenu.add(Type_SubtitleTrack, audioTrackList[i].id, i, audioTrackList[i].name);
            m.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    audioTrackMenu.dismiss();
                    if (m.id != mediaPlayer.getAudioTrack()) {
                        mediaPlayer.setAudioTrack(m.id);
                    }
                }
            });
        }
        audioTracksBtn.setOnClickListener(this);
    }

    /**
     * 显示控制器
     */
    public void Show() {
        Show(6);
    }

    /**
     * 显示控制器
     */
    public void Show(int sec) {
        if (ControllerTop.getVisibility() == View.GONE) {
            ControllerTop.setVisibility(View.VISIBLE);
        }
        if (ControllerBottom.getVisibility() == View.GONE) {
            progressTime = new Timer();
            progressTime.schedule(new TimerTask() {
                @Override
                public void run() {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setSeekBar(currPlaybackTimeTrack);
                        }
                    });
                }
            }, 0, 1000);
            ControllerBottom.setVisibility(View.VISIBLE);
            playPauseBtn.requestFocus();
        }
    }

    /**
     * 设置进度条时间
     */
    public void setSeekBar(Long p) {
        if (ControllerBottom.getVisibility() == View.VISIBLE) {
            double i = (double) p / 1000;
            long duration = mediaPlayer.getLength();
            if (duration > 0) {
                long pos = 1000L * p / duration;
                currPostion.setProgress((int) pos);
            }
            currTime.setText(TrickToTime(p));
            countTime.setText(TrickToTime(duration));
        }
    }

    /**
     * 隐藏控制器
     */
    public void Hide() {
        if (ControllerTop.getVisibility() == View.VISIBLE) {
            ControllerTop.setVisibility(View.GONE);
        }
        if (ControllerBottom.getVisibility() == View.VISIBLE) {
            ControllerBottom.setVisibility(View.GONE);
            if (progressTime != null) {
                progressTime.cancel();
                progressTime = null;
            }
        }
    }

    /**
     * 开始播放
     */
    public void play() {
        if (Utils.playList.size() > 0) {
            if (Utils.playIndex < Utils.playList.size()) {
                Video v = Utils.playList.get(Utils.playIndex);
                videoTitle.setText(v.Name);
                mediaPlayer.play(Uri.parse(v.Url));
            }
        } else {
            stop();
        }
    }

    /**
     * 播放下一集
     */
    public void playNext() {
        ReportPlayState(Utils.ReportType.stop, Utils.playList.get(Utils.playIndex).Id);
        Utils.playIndex += 1;
        play();
    }

    /**
     * 上一集
     */
    public void playPre() {
        ReportPlayState(Utils.ReportType.stop, Utils.playList.get(Utils.playIndex).Id);
        if (Utils.playIndex > 0) {
            Utils.playIndex -= 1;
            play();
        }
    }

    /**
     * 停止播放并结束Activity
     */
    public void stop() {
        ReportPlayState(Utils.ReportType.stop, Utils.playList.get(Utils.playIndex).Id);
        if (progressTime != null) {
            progressTime.cancel();
            progressTime = null;
        }
        mediaPlayer.stop();
        mediaPlayer.release();
        libVLC.release();
        finish();
    }

    /**
     * 播放或暂停
     */
    public void playOrpause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            pauseFlag.setVisibility(View.VISIBLE);
        } else {
            mediaPlayer.play();
        }
    }

    /**
     * 设置播放器位置
     */
    public boolean setTimeOnSeekBar(Long p) {
        if (p < mediaPlayer.getLength() && p > 0) {
            mediaPlayer.setTime(p);
            setSeekBar(p);
        }
        return true;
    }

    /**
     * 进度转时间
     *
     * @param trick
     * @return
     */
    public static String TrickToTime(long trick) {
        String time = "";
        long totalSeconds = trick / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;

        time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return time;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (ControllerBottom.getVisibility() == View.GONE) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    Show();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mediaPlayer.setTime(mediaPlayer.getTime() + 30000);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mediaPlayer.setTime(mediaPlayer.getTime() - 10000);
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    playOrpause();
                    return true;
                case KeyEvent.KEYCODE_ESCAPE:
                case KeyEvent.KEYCODE_BACK:
                    stop();
                    return true;
                //退出
            }
        } else {
            switch (keyCode) {
                case KeyEvent.KEYCODE_ESCAPE:
                case KeyEvent.KEYCODE_BACK:
                    Hide();
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.nextBtn) {
            playNext();
        } else if (id == R.id.preBtn) {
            playPre();
        } else if (id == R.id.playPauseBtn) {
            playOrpause();
        } else if (id == R.id.stopBtn) {
            stop();
        } else if (id == R.id.subTracksBtn) {
            subTrackMenu.show(mediaPlayer.getSpuTrack());
        } else if (id == R.id.audioTracksBtn) {
            audioTrackMenu.show(mediaPlayer.getAudioTrack());
        } else if (id == R.id.playListBtn) {
            playListMenu.show(Utils.playIndex);
        } else if (id == R.id.scaleBtn) {
            scaleTypeMenu.show(Utils.getVlcScaleTypeName(mediaPlayer.getVideoScale().name()));
        } else if (id == R.id.speedBtn) {
            speedMenu.show(String.valueOf(mediaPlayer.getRate()));
        }
    }

    private void ReportPlayState(Utils.ReportType type, String Id) {
        if (type == Utils.ReportType.playing) {
            reportProcessTime = new Timer();
            reportProcessTime.schedule(new TimerTask() {
                @Override
                public void run() {
                    ReportPlayState(Utils.ReportType.Progress, Utils.playList.get(Utils.playIndex).Id);
                }
            }, 1000, 10000);
        } else if (type == Utils.ReportType.stop) {
            if (reportProcessTime != null) {
                reportProcessTime.cancel();
                reportProcessTime = null;
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (type == Utils.ReportType.playing) {
                    Utils.ReportPlaying(Id, currPlaybackTimeTrack);
                } else if (type == Utils.ReportType.stop) {
                    Utils.ReportPlaybackStop(Id, currPlaybackTimeTrack);
                } else if (type == Utils.ReportType.Progress) {
                    Log.d(TAG, "run: 报告时空：" + currPlaybackTimeTrack);
                    Utils.ReportPlaybackProgress(Id, !mediaPlayer.isPlaying(), currPlaybackTimeTrack);
                }
            }
        }).start();
    }
}