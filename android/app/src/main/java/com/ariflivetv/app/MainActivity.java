package com.ariflivetv.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.TypedValue;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.ui.PlayerView;

public class MainActivity extends Activity {
    private FrameLayout root;
    private WebView webView;
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView closePlayer;

    public class PlayerBridge {
        @JavascriptInterface
        public void play(String url, String title, float x, float y, float w, float h) {
            if (url == null || !(url.startsWith("https://") || url.startsWith("http://") || url.startsWith("rtsp://"))) return;
            runOnUiThread(() -> playInline(url,title,x,y,w,h));
        }
    }

    private void playInline(String url,String title,float x,float y,float w,float h) {
        stopInline();
        float d=getResources().getDisplayMetrics().density;
        int[] webPos=new int[2]; int[] rootPos=new int[2];
        webView.getLocationOnScreen(webPos); root.getLocationOnScreen(rootPos);
        int left=webPos[0]-rootPos[0]+Math.round(x*d);
        int top=webPos[1]-rootPos[1]+Math.round(y*d);
        playerView=new PlayerView(this); playerView.setUseController(true);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.round(w*d),Math.round(h*d));
        lp.leftMargin=left; lp.topMargin=top;
        root.addView(playerView,lp);
        closePlayer=new TextView(this); closePlayer.setText("✕"); closePlayer.setTextColor(Color.WHITE);
        closePlayer.setTextSize(20); closePlayer.setGravity(Gravity.CENTER); closePlayer.setBackgroundColor(0xCC111111);
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(42),dp(42),Gravity.TOP|Gravity.RIGHT);
        cp.leftMargin=left+Math.round(w*d)-dp(46); cp.topMargin=top+dp(4);
        root.addView(closePlayer,cp); closePlayer.setOnClickListener(v->stopInline());
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder().setBufferDurationsMs(15000, 50000, 1000, 2000).setPrioritizeTimeOverSizeThresholds(true).build();
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36 ARIF-LIVE-TV")
                .setAllowCrossProtocolRedirects(true);
        player=new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(httpFactory))
                .build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(androidx.media3.common.PlaybackException e) {
                Toast.makeText(MainActivity.this, "ভিডিও চালানো যাচ্ছে না: " + (e.getMessage() == null ? "Stream error" : e.getMessage()), Toast.LENGTH_LONG).show();
                closePlayer.setText("✕"); closePlayer.setOnClickListener(v->stopInline());
            }
        });
        MediaItem.Builder item=new MediaItem.Builder().setUri(url);
        String lower=url.toLowerCase();
        if(lower.contains(".m3u8")) item.setMimeType(MimeTypes.APPLICATION_M3U8);
        else if(lower.contains(".mpd")) item.setMimeType(MimeTypes.APPLICATION_MPD);
        if(lower.startsWith("rtsp://")) player.setMediaSource(new RtspMediaSource.Factory().createMediaSource(item.build()));
        else player.setMediaItem(item.build());
        player.prepare(); player.play();
    }
    private int dp(int n){return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,n,getResources().getDisplayMetrics());}
    private void stopInline(){
        if(player!=null){player.release();player=null;}
        if(playerView!=null){root.removeView(playerView);playerView=null;}
        if(closePlayer!=null){root.removeView(closePlayer);closePlayer=null;}
        if(webView!=null) webView.evaluateJavascript("['livebadge','views','channelbug','liveart','ticker'].forEach(id=>{const e=document.getElementById(id);if(e)e.style.display=''});",null);
    }
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        root=new FrameLayout(this); webView=new WebView(this); root.addView(webView,new FrameLayout.LayoutParams(-1,-1)); setContentView(root);
        WebSettings s=webView.getSettings(); s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setAllowFileAccess(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.addJavascriptInterface(new PlayerBridge(),"AndroidPlayer");webView.setWebViewClient(new WebViewClient());webView.setWebChromeClient(new WebChromeClient());webView.loadUrl("file:///android_asset/index.html");
    }
    @Override public void onBackPressed(){if(player!=null)stopInline();else if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){stopInline();if(webView!=null)webView.destroy();super.onDestroy();}
}
