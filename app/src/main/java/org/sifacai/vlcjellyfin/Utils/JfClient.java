package org.sifacai.vlcjellyfin.Utils;

import android.app.Application;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.Response;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.sifacai.vlcjellyfin.Bean.Item;
import org.sifacai.vlcjellyfin.Bean.Items;
import org.sifacai.vlcjellyfin.Player.Video;
import org.sifacai.vlcjellyfin.Utils.Config;
import org.sifacai.vlcjellyfin.Utils.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class JfClient {
    public static final String TAG = "JellyfinClient";
    public static final String DeviceId = "TW96aWxsYS81LjAgKFdpbmRvd3MgTlQgNi4xOyBXa";
    public static final String XEmbyAuthorization = "MediaBrowser Client=\"Vlc_J_TV\", Device=\"Vlc_J_TV\", DeviceId=\"" + DeviceId + "\", Version=\"10.8.1\"";
    public static HttpHeaders headers;
    public static Config config;
    public static String UserId = "";
    public static String AccessToken = "";

    public static int playIndex = 0; //当前播放
    public static ArrayList<Video> playList = new ArrayList<>(); //播放列表

    public enum ReportType {
        playing,
        Progress,
        stop
    }

    /**
     * description 忽略https证书验证
     *
     * @author yanzy
     * @version 1.0
     * @date 2021/9/8 14:42
     */
    private static TrustManager[] getTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
        return trustAllCerts;
    }


    /**
     * description 忽略https证书验证
     * `在这里插入代码片`
     *
     * @author yanzy
     * @version 1.0
     * @date 2021/9/8 14:42
     */
    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, getTrustManager(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * description 忽略https证书验证
     *
     * @author yanzy
     * @version 1.0
     * @date 2021/9/8 14:42
     */
    public static HostnameVerifier getHostnameVerifier() {
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
        return hostnameVerifier;
    }


    /**
     * 初始化配置
     *
     * @param application
     */
    public static void init(Application application) {
        config = new Config(application);
        SetHeaders();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(getSSLSocketFactory());
        builder.hostnameVerifier(getHostnameVerifier());
        builder.connectTimeout(5, TimeUnit.SECONDS);
        builder.readTimeout(3, TimeUnit.SECONDS);
        builder.writeTimeout(5, TimeUnit.SECONDS);
        OkGo.getInstance().init(application)
                .setOkHttpClient(builder.build())
                .setRetryCount(3);
        Picasso.setSingletonInstance(new Picasso.Builder(application.getBaseContext())
                .downloader(new OkHttp3Downloader(builder.build()))
                .build()
        );
    }

    /**
     * 回放报告
     *
     * @param type
     * @param id
     * @param PositionTicks
     */
    public static void ReportPlayBackState(ReportType type, String id, long PositionTicks) {
        String url = config.getJellyfinUrl();
        if (type == ReportType.playing) {
            url += "/Sessions/Playing";
        } else if (type == ReportType.Progress) {
            url += "/Sessions/Playing/Progress";
        } else if (type == ReportType.stop) {
            url += "/Sessions/Playing/Stopped";
        }
        String reqstr = "{\"itemId\":\"" + id + "\",\"PositionTicks\":\"" + PositionTicks * 10000 + "\"}";
        SendPost(url, reqstr, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                //回放报告
            }
        }, null);
    }

    /**
     * 获取封面图url
     *
     * @param itemid
     * @param tagid
     * @return
     */
    public static String GetImgUrl(String itemid, String tagid) {
        String url = config.getJellyfinUrl() + "/Items/" + itemid + "/Images/Primary";
        url += "?fillHeight=286&fillWidth=200&quality=96&tag=" + tagid;
        return url;
    }

    public static String GetBackdropUrl(String itemid, String tagid) {
        String url = config.getJellyfinUrl() + "/Items/" + itemid + "/Images/Backdrop";
        url += "?maxWidth=1280&quality=90&tag=" + tagid;
        return url;
    }

    public static String GetImgUrl(Item item) {
        if (item.getImageTags() == null) return "";
        if (item.getImageTags().getPrimary() == null || item.getImageTags().getPrimary().equals(""))
            return "";
        return GetImgUrl(item.getId(), item.getImageTags().getPrimary());
    }

    /**
     * 获取播放url
     *
     * @param itemid
     * @return
     */
    public static String GetPlayUrl(String itemid) {
        String playbackurl = config.getJellyfinUrl() + "/Items/" + itemid + "/PlaybackInfo?UserId=" + UserId  + "&DeviceId=" + DeviceId + "&api_key=" + AccessToken;
        // 可以修改各种具体参数如比特率，允许的编码方式，字幕烧录方式等
        // 此处为 仅使用 h264 强制烧录字幕
        String deviceProfile = "{\"DeviceProfile\":{\"MaxStreamingBitrate\":120000000,\"MaxStaticBitrate\":100000000,\"MusicStreamingTranscodingBitrate\":384000,\"DirectPlayProfiles\":[{\"Container\":\"mp4,m4v\",\"Type\":\"Video\",\"VideoCodec\":\"h264\",\"AudioCodec\":\"aac,mp3,opus,flac,vorbis\"},{\"Container\":\"mov\",\"Type\":\"Video\",\"VideoCodec\":\"h264\",\"AudioCodec\":\"aac,mp3,opus,flac,vorbis\"},{\"Container\":\"opus\",\"Type\":\"Audio\"},{\"Container\":\"webm\",\"AudioCodec\":\"opus\",\"Type\":\"Audio\"},{\"Container\":\"mp3\",\"Type\":\"Audio\"},{\"Container\":\"aac\",\"Type\":\"Audio\"},{\"Container\":\"m4a\",\"AudioCodec\":\"aac\",\"Type\":\"Audio\"},{\"Container\":\"m4b\",\"AudioCodec\":\"aac\",\"Type\":\"Audio\"},{\"Container\":\"flac\",\"Type\":\"Audio\"},{\"Container\":\"webma\",\"Type\":\"Audio\"},{\"Container\":\"webm\",\"AudioCodec\":\"webma\",\"Type\":\"Audio\"},{\"Container\":\"wav\",\"Type\":\"Audio\"},{\"Container\":\"ogg\",\"Type\":\"Audio\"}],\"TranscodingProfiles\":[{\"Container\":\"ts\",\"Type\":\"Audio\",\"AudioCodec\":\"aac\",\"Context\":\"Streaming\",\"Protocol\":\"hls\",\"MaxAudioChannels\":\"2\",\"MinSegments\":\"1\",\"BreakOnNonKeyFrames\":true},{\"Container\":\"aac\",\"Type\":\"Audio\",\"AudioCodec\":\"aac\",\"Context\":\"Streaming\",\"Protocol\":\"http\",\"MaxAudioChannels\":\"2\"},{\"Container\":\"mp3\",\"Type\":\"Audio\",\"AudioCodec\":\"mp3\",\"Context\":\"Streaming\",\"Protocol\":\"http\",\"MaxAudioChannels\":\"2\"},{\"Container\":\"opus\",\"Type\":\"Audio\",\"AudioCodec\":\"opus\",\"Context\":\"Streaming\",\"Protocol\":\"http\",\"MaxAudioChannels\":\"2\"},{\"Container\":\"wav\",\"Type\":\"Audio\",\"AudioCodec\":\"wav\",\"Context\":\"Streaming\",\"Protocol\":\"http\",\"MaxAudioChannels\":\"2\"},{\"Container\":\"opus\",\"Type\":\"Audio\",\"AudioCodec\":\"opus\",\"Context\":\"Static\",\"Protocol\":\"http\",\"MaxAudioChannels\":\"2\"},{\"Container\":\"mp3\",\"Type\":\"Audio\",\"AudioCodec\":\"mp3\",\"Context\":\"Static\",\"Protocol\":\"http\",\"MaxAudioChannels\":\"2\"},{\"Container\":\"aac\",\"Type\":\"Audio\",\"AudioCodec\":\"aac\",\"Context\":\"Static\",\"Protocol\":\"http\",\"MaxAudioChannels\":\"2\"},{\"Container\":\"wav\",\"Type\":\"Audio\",\"AudioCodec\":\"wav\",\"Context\":\"Static\",\"Protocol\":\"http\",\"MaxAudioChannels\":\"2\"},{\"Container\":\"ts\",\"Type\":\"Video\",\"AudioCodec\":\"aac,mp3\",\"VideoCodec\":\"h264\",\"Context\":\"Streaming\",\"Protocol\":\"hls\",\"MaxAudioChannels\":\"2\",\"MinSegments\":\"1\",\"BreakOnNonKeyFrames\":true}],\"ContainerProfiles\":[],\"CodecProfiles\":[{\"Type\":\"Video\",\"Codec\":\"h264\",\"Conditions\":[{\"Condition\":\"NotEquals\",\"Property\":\"IsAnamorphic\",\"Value\":\"true\",\"IsRequired\":false},{\"Condition\":\"EqualsAny\",\"Property\":\"VideoProfile\",\"Value\":\"high|main|baseline|constrained baseline|high 10\",\"IsRequired\":false},{\"Condition\":\"EqualsAny\",\"Property\":\"VideoRangeType\",\"Value\":\"SDR\",\"IsRequired\":false},{\"Condition\":\"LessThanEqual\",\"Property\":\"VideoLevel\",\"Value\":\"52\",\"IsRequired\":false},{\"Condition\":\"NotEquals\",\"Property\":\"IsInterlaced\",\"Value\":\"true\",\"IsRequired\":false}]}],\"SubtitleProfiles\":[{\"Format\":\"vtt\",\"Method\":\"Embed\"},{\"Format\":\"ass\",\"Method\":\"Embed\"},{\"Format\":\"ssa\",\"Method\":\"Embed\"}],\"ResponseProfiles\":[{\"Type\":\"Video\",\"Container\":\"m4v\",\"MimeType\":\"video/mp4\"}]}}";

        String playpath = "";
        SendPost(playbackurl, deviceProfile, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    // 使用Gson解析JSON数据
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(str, JsonObject.class);
                    JsonArray mediaSources = jsonObject.getAsJsonArray("MediaSources");

                    // 检查MediaSources是否为空
                    if (mediaSources != null && mediaSources.size() > 0) {
                        JsonObject mediaSource = mediaSources.get(0).getAsJsonObject();
                        String transcodingUrl = mediaSource.get("TranscodingUrl").getAsString();
                        final String playpath = transcodingUrl;
                    } 
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, null);

        String playurl = ""; // Declare the playurl variable
        if (playpath != null && !playpath.isEmpty()) {
            playurl = config.getJellyfinUrl() + playpath;
        } else {
            playurl = config.getJellyfinUrl() + "/videos/" + itemid + "/stream.mp4?static=true&DeviceId=" + DeviceId + "&api_key=" + AccessToken;
        }

        return playurl; // Return the playurl variable
}       

    public static void SearchByTerm(String term, int limit, JJCallBack scb, JJCallBack errcb) {
        String BaseUrl = config.getJellyfinUrl() + "/Users/" + UserId + "/Items?";
        BaseUrl += "Fields=PrimaryImageAspectRatio,CanDelete,BasicSyncInfo,MediaSourceCount";
        BaseUrl += "&Recursive=true&EnableTotalRecordCount=false&ImageTypeLimit=1&IncludePeople=false";
        BaseUrl += "&IncludeMedia=true&IncludeGenres=false&IncludeStudios=false&IncludeArtists=false";
        BaseUrl += "&Limit=" + limit;

        String PersonUrl = config.getJellyfinUrl() + "/Persons?Fields=PrimaryImageAspectRatio%2CCanDelete%2CBasicSyncInfo%2CMediaSourceCount&Recursive=true";
        PersonUrl += "&EnableTotalRecordCount=false&ImageTypeLimit=1&IncludePeople=true&IncludeMedia=false";
        PersonUrl += "&IncludeGenres=false&IncludeStudios=false&IncludeArtists=false&userId=" + UserId;
        String personUrl = PersonUrl + "&searchTerm=" + term + "&Limit=" + limit;

        String movieUrl = BaseUrl + "&searchTerm=" + term + "&IncludeItemTypes=Movie";
        String seriesUrl = BaseUrl + "&searchTerm=" + term + "&IncludeItemTypes=Series";
        String episodeUrl = BaseUrl + "&searchTerm=" + term + "&IncludeItemTypes=Episode";

        JsonArray items = new JsonArray();
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Items items = new Items();
                            items.setItems(new ArrayList<>());
                            String jsonstr = SendGet(movieUrl);
                            Items moviejob = Utils.jsonToClass(jsonstr, Items.class);
                            if (moviejob != null) {
                                items.AddItems(moviejob.getItems());
                                items.setTotalRecordCount(items.getTotalRecordCount() + moviejob.getTotalRecordCount());
                            }

                            jsonstr = SendGet(seriesUrl);
                            Items seriesobj = Utils.jsonToClass(jsonstr, Items.class);
                            if (seriesobj != null) {
                                items.AddItems(seriesobj.getItems());
                                items.setTotalRecordCount(items.getTotalRecordCount() + seriesobj.getTotalRecordCount());
                            }

                            jsonstr = SendGet(personUrl);
                            Items personobj = Utils.jsonToClass(jsonstr, Items.class);
                            if (personobj != null) {
                                items.AddItems(personobj.getItems());
                                items.setTotalRecordCount(items.getTotalRecordCount() + personobj.getTotalRecordCount());
                            }
                            scb.onSuccess(items);
                        } catch (Exception e) {
                            errcb.onError(e.toString());
                        }
                    }
                }
        ).start();
    }

    /**
     * 获取项目附加部分
     *
     * @param itemid
     * @param cb
     */
    public static void GetAddPart(String itemid, JJCallBack cb, JJCallBack errcb) {
        String AddPartUrl = config.getJellyfinUrl() + "/Videos/" + itemid + "/AdditionalParts?userId=" + UserId;
        SendGet(AddPartUrl, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    Items items = Utils.jsonToClass(str, Items.class);
                    cb.onSuccess(items);
                } catch (Exception e) {
                    errcb.onError(e.getMessage());
                }
            }
        }, errcb);
    }

    /**
     * 获取剧集
     *
     * @param seriesId 剧ID
     * @param seasonId 季ID
     * @param cb
     */
    public static void GetEpisodes(String seriesId, String seasonId, JJCallBack cb, JJCallBack errcb) {
        String EpisodesUrl = config.getJellyfinUrl() + "/Shows/" + seriesId + "/Episodes?seasonId=" + seasonId;
        EpisodesUrl += "&userId=" + UserId;
        EpisodesUrl += "&Fields=ItemCounts,PrimaryImageAspectRatio,BasicSyncInfo,CanDelete,MediaSourceCount,Overview,Path";

        SendGet(EpisodesUrl, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    Items items = Utils.jsonToClass(str, Items.class);
                    cb.onSuccess(items);
                } catch (Exception e) {
                    errcb.onError(e.getMessage());
                }
            }
        }, errcb);

    }

    /**
     * 获取季(Seasons)数据
     *
     * @param seriesId 剧ID
     * @param cb
     */
    public static void GetSeasons(String seriesId, JJCallBack cb, JJCallBack errcb) {
        String SeasonsUrl = config.getJellyfinUrl() + "/Shows/" + seriesId + "/Seasons?userId=" + UserId;
        SeasonsUrl += "&Fields=ItemCounts,PrimaryImageAspectRatio,BasicSyncInfo,MediaSourceCount,Path";

        SendGet(SeasonsUrl, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    Items items = Utils.jsonToClass(str, Items.class);
                    cb.onSuccess(items);
                } catch (Exception e) {
                    errcb.onError(e.getMessage());
                }

            }
        }, errcb);
    }

    /**
     * 根据条件获取条目列表
     *
     * @param term  可以是 &Limit、&SortBy、&IncludeItemTypes、&PersonIds 等
     * @param scb
     * @param errcb
     */
    public static void GetItemsByTerm(String term, JJCallBack scb, JJCallBack errcb) {
        String BaseUrl = config.getJellyfinUrl() + "/Users/" + UserId + "/Items?Recursive=true&StartIndex=0&CollapseBoxSetItems=false";
        BaseUrl += term;

        SendGet(BaseUrl, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    Items items = Utils.jsonToClass(str, Items.class);
                    scb.onSuccess(items);
                } catch (Exception e) {
                    errcb.onError(e.getMessage());
                }
            }
        }, errcb);
    }

    /**
     * 获取合集条目
     *
     * @param parentId
     * @param type      类型 movie、tvshows……
     * @param sortBy    排序条件  评分、加入时间……
     * @param sortOrder 排序规则 升序、降序
     * @param limit     每页条数
     * @param page      页
     * @param cb
     */
    public static void GetCollection(String parentId, String type, String sortBy, String sortOrder, int limit, int page, JJCallBack cb, JJCallBack errcb) {
        String itemsUrl = config.getJellyfinUrl() + "/Users/" + UserId + "/Items?ParentId=" + parentId + "&Limit=" + limit;
        itemsUrl += "&Recursive=true&Fields=PrimaryImageAspectRatio,BasicSyncInfo,Seasons,Episodes,Path&ImageTypeLimit=1";
        itemsUrl += "&EnableImageTypes=Primary,Backdrop,Banner,Thumb";
        itemsUrl += "&SortBy=" + sortBy + ",SortName,ProductionYear&SortOrder=" + sortOrder;

        if (type.equals("tvshows")) {
            itemsUrl += "&IncludeItemTypes=Series";
        } else if (type.equals("movies")) {
            itemsUrl += "&IncludeItemTypes=Movie";
        } else if (type.equals("CollectionFolder") || type.equals("Folder")) {

        } else {
            itemsUrl += "&IncludeItemTypes=Movie,Series";
        }
        int startIndex = page * limit - limit;
        itemsUrl += "&StartIndex=" + startIndex;

        SendGet(itemsUrl, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    Items items = Utils.jsonToClass(str, Items.class);
                    cb.onSuccess(items);
                } catch (Exception e) {
                    errcb.onError(e.getMessage());
                }

            }
        }, errcb);
    }

    /**
     * 获取最新项目
     *
     * @param parentId
     * @param cb
     */
    public static void GetLatest(String parentId, JJCallBack cb, JJCallBack errcb) {
        String lastestUrl = config.getJellyfinUrl() + "/Users/" + UserId + "/Items/Latest?";
        lastestUrl += "Limit=16&Fields=PrimaryImageAspectRatio%2CBasicSyncInfo%2CPath";
        lastestUrl += "&ImageTypeLimit=1&EnableImageTypes=Primary,Backdrop,Thumb";
        lastestUrl += "&ParentId=" + parentId;

        SendGet(lastestUrl, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                Type type = new TypeToken<List<Item>>() {
                }.getType();
                List<Item> rets = Utils.jsonToClass(str, type);
                if (null != rets) {
                    Items items = new Items();
                    items.setItems(rets);
                    items.setTotalRecordCount(rets.size());
                    cb.onSuccess(items);
                }
            }
        }, errcb);
    }

    /**
     * 获取最近播放
     *
     * @param cb
     */
    public static void GetResume(JJCallBack cb, JJCallBack err) {
        String resumeUrl = config.getJellyfinUrl() + "/Users/" + UserId + "/Items/Resume?";
        resumeUrl += "Limit=12&Recursive=true&Fields=PrimaryImageAspectRatio,BasicSyncInfo";
        resumeUrl += "&ImageTypeLimit=1&EnableImageTypes=Primary,Backdrop,Thumb";
        resumeUrl += "&EnableTotalRecordCount=false&MediaTypes=Video";

        SendGet(resumeUrl, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    Items items = Utils.jsonToClass(str, Items.class);
                    cb.onSuccess(items);
                } catch (Exception e) {
                    err.onError(e.getMessage());
                }


            }
        }, err);
    }

    /**
     * 获取首页
     *
     * @param cb
     */
    public static void GetViews(JJCallBack cb, JJCallBack err) {
        String viewsUrl = config.getJellyfinUrl() + "/Users/" + UserId + "/Views";
        SendGet(viewsUrl, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    Items views = Utils.jsonToClass(str, Items.class);
                    cb.onSuccess(views);
                } catch (Exception e) {
                    err.onError(e.getMessage());
                }
            }
        }, err);
    }

    /**
     * 获取项目详情
     *
     * @param itemid
     * @param cb
     */
    public static void GetItemInfo(String itemid, JJCallBack cb, JJCallBack err) {
        String url = config.getJellyfinUrl() + "/Users/" + UserId + "/Items/" + itemid;
        SendGet(url, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    Item item = Utils.jsonToClass(str, Item.class);
                    cb.onSuccess(item);
                } catch (Exception e) {
                    err.onError(e.getMessage());
                }
            }
        }, err);
    }

    /**
     * 验证用户名密码
     *
     * @param username
     * @param password
     * @param cb
     */
    public static void AuthenticateByName(String username, String password, JJCallBack cb, JJCallBack err, boolean saveUser) {
        if (username.equals("") || password.equals("")) {
            err.onError("用户名和密码验证失败！");
            return;
        }
        String url = config.getJellyfinUrl() + "/Users/authenticatebyname";
        String reqjson = "{\"Username\":\"" + username + "\",\"Pw\":\"" + password + "\"}";
        SendPost(url, reqjson, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    JsonObject userObj = new Gson().fromJson(str, JsonObject.class);
                    if (userObj.has("User") && userObj.has("AccessToken")) {
                        String userId = userObj.get("User").getAsJsonObject().get("Id").getAsString();
                        String Token = userObj.get("AccessToken").getAsString();
                        if (!Token.equals("")) {
                            UserId = userId;
                            AccessToken = Token;
                            if (saveUser) {
                                config.setUserName(username);
                                config.setPassWord(password);
                            }
                            SetHeaders();
                            cb.onSuccess(true);
                        } else {
                            err.onError("验证失败：" + str);
                        }
                    } else {
                        err.onError("验证失败：" + str);
                    }
                } catch (Exception e) {
                    err.onError("验证失败：" + e.getMessage());
                }
            }
        }, err);
    }

    /**
     * 获取用户列表
     *
     * @param cb
     */
    public static void GetUsers(JJCallBack cb, JJCallBack err) {
        String url = config.getJellyfinUrl() + "/users/public";
        SendGet(url, new JJCallBack() {
            @Override
            public void onSuccess(String str) {
                JsonArray users = new Gson().fromJson(str, JsonArray.class);
                cb.onSuccess(users);
            }
        }, err);
    }

    /**
     * 验证服务器地址
     *
     * @param url
     * @param cb
     */
    public static void VerityServerUrl(String url, JJCallBack cb, JJCallBack err) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            SendGet(url + "/system/info/public", new JJCallBack() {
                @Override
                public void onSuccess(String str) {
                    try {
                        JsonObject serverInfo = new Gson().fromJson(str, JsonObject.class);
                        String ServerId = "";
                        ServerId = serverInfo.get("Id").getAsString();
                        if (ServerId == null || ServerId.length() == 0) {
                            err.onError("服务器连接失败！");
                        } else {
                            config.setJellyfinUrl(url);
                            cb.onSuccess(true);
                        }
                    } catch (Exception e) {
                        err.onError(e.getMessage());
                    }
                }
            }, err);
        } else {
            err.onError("服务器地址不正确！");
        }
    }

    /**
     * 设置请求头
     */
    public static void SetHeaders() {
        headers = new HttpHeaders();
        String xea = XEmbyAuthorization;
        if (null != AccessToken && AccessToken.length() > 0) {
            xea += ", Token=\"" + AccessToken + "\"";
        }
        headers.put("Accept", "application/json");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        headers.put("X-Emby-Authorization", xea);
    }

    /**
     * Get
     *
     * @param url
     * @param cb
     */
    public static void SendGet(String url, JJCallBack cb, JJCallBack errcb) {
        OkGo.<String>get(url).headers(headers).execute(new AbsCallback<String>() {
            @Override
            public String convertResponse(okhttp3.Response response) throws IOException {
                String result = "";
                if (null != response.body()) {
                    result = response.body().string();
                }
                return result;
            }

            @Override
            public void onSuccess(Response<String> response) {
                cb.onSuccess(response.body());
            }

            @Override
            public void onError(Response<String> response) {
                if (errcb != null) {
                    errcb.onError(response.getException().getMessage());
                }
            }
        });
    }

    /**
     * Post
     *
     * @param url
     * @param jsonStr
     * @param cb
     */
    public static void SendPost(String url, String jsonStr, JJCallBack cb, JJCallBack errcb) {
        OkGo.<String>post(url).headers(headers).upJson(jsonStr).execute(new AbsCallback<String>() {
            @Override
            public String convertResponse(okhttp3.Response response) throws Throwable {
                String result = "";
                if (null != response.body()) {
                    result = response.body().string();
                }
                return result;
            }

            @Override
            public void onSuccess(Response<String> response) {
                cb.onSuccess(response.body());
            }

            @Override
            public void onError(Response<String> response) {
                if (errcb != null) {
                    errcb.onError(response.getException().getMessage());
                }
            }
        });
    }

    /**
     * 阻塞式Get
     *
     * @param url
     */
    public static String SendGet(String url) throws IOException {
        String response = "";
        response = OkGo.<String>get(url).headers(headers).execute().body().string();
        return response;
    }

    public static String SendPost(String url, String body) throws IOException {
        String response = "";
        response = OkGo.<String>post(url).upBytes(body.getBytes()).execute().body().string();
        return response;
    }

    public static class JJCallBack implements JCallBack {

        @Override
        public void onSuccess(String str) {

        }

        @Override
        public void onSuccess(Boolean bool) {

        }

        @Override
        public void onSuccess(JsonObject jsonObject) {

        }

        @Override
        public void onSuccess(JsonArray jsonArray) {

        }

        @Override
        public void onSuccess(Item item) {

        }

        @Override
        public void onSuccess(Items items) {

        }

        @Override
        public void onError(String str) {

        }
    }

    public interface JCallBack {
        void onSuccess(String str);

        void onSuccess(Boolean bool);

        void onSuccess(JsonObject jsonObject);

        void onSuccess(JsonArray jsonArray);

        void onSuccess(Item item);

        void onSuccess(Items items);

        void onError(String str);
    }
}
