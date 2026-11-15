package com.auraglass.clockwidget;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    private SharedPreferences pref;
    private ImageView preview;
    private TextView intensityLabel;
    private int mode=2;
    private int intensity=82;
    private Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){
        public void run(){ renderPreview();handler.postDelayed(this,12000); }
    };
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int a,int b,int c,float r){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b,c});
        d.setCornerRadius(dp(r));return d;
    }
    private TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(android.graphics.Typeface.create("sans-serif",bold?1:0));return t;
    }
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void add(LinearLayout parent,View child,int height){
        parent.addView(child,new LinearLayout.LayoutParams(-1,height<0?height:dp(height)));
    }
    private View space(int v){View x=new View(this);x.setLayoutParams(new LinearLayout.LayoutParams(1,dp(v)));return x;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        pref=getSharedPreferences(ClockProvider.PREFS,MODE_PRIVATE);
        mode=pref.getInt("mode",2);intensity=pref.getInt("strength",82);
        buildUI();
    }
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(refresh);handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private void renderPreview(){
        if(preview!=null)
            preview.setImageBitmap(GlassArt.render(this,GlassArt.currentTime(),850,235,mode,intensity));
    }
    private void save(){
        pref.edit().putInt("mode",mode).putInt("strength",intensity).apply();
        renderPreview();
        ClockProvider.updateAll(this);
        ClockProvider.scheduleNext(this);
    }
    private Button button(String s){
        Button b=new Button(this);
        b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(15);
        b.setBackground(bg(0xff204875,0xff347ab2,0xff238c9f,20));return b;
    }
    private void buildUI(){
        getWindow().setStatusBarColor(0xff071c33);
        getWindow().setNavigationBarColor(0xff071c33);
        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setBackground(bg(0xff071827,0xff142d48,0xff0a213e,0));
        LinearLayout page=vertical();page.setPadding(dp(20),dp(24),dp(20),dp(22));
        scroller.addView(page);
        setContentView(scroller);
        add(page,label("AURA GLASS",15,0xff8ee5fa,true),-2);
        page.addView(space(6));
        add(page,label("Đồng hồ kính lỏng",28,Color.WHITE,true),-2);
        page.addView(space(6));
        add(page,label("iOS 26 style  •  LG V50  •  HH:mm",13,0xffbdd0e9,false),-2);
        page.addView(space(22));

        FrameLayout demo=new FrameLayout(this);
        demo.setBackground(bg(0xff182e55,0xff365a80,0xff162b4f,25));
        add(page,demo,182);
        preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-1);
        lp.setMargins(dp(4),dp(4),dp(4),dp(4));
        demo.addView(preview,lp);
        renderPreview();

        page.addView(space(14));
        add(page,label("Bản widget thực tế không có nền, không khung và không ngày tháng. Phần nền xanh bên trên chỉ để xem thử.",13,0xffbdd0e9,false),-2);
        page.addView(space(20));
        add(page,label("Màu phản chiếu",17,Color.WHITE,true),-2);
        page.addView(space(7));
        RadioGroup group=new RadioGroup(this);
        group.setOrientation(1);
        String[] modes={"Ngọc trai • bạc ánh xanh","Ice Blue • xanh băng","Holographic • xanh tím cầu vồng"};
        for(int i=0;i<modes.length;i++){
            final int n=i;RadioButton r=new RadioButton(this);
            r.setText(modes[i]);r.setTextColor(0xffecf6ff);r.setTextSize(15);
            r.setId(100+i);
            r.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff8ae9f5));
            group.addView(r,new RadioGroup.LayoutParams(-1,dp(46)));
        }
        group.check(100+mode);
        group.setOnCheckedChangeListener((g,checked)->{mode=checked-100;save();});
        add(page,group,-2);
        page.addView(space(10));
        intensityLabel=label("Cường độ ánh sáng: "+intensity+"%",16,Color.WHITE,true);
        add(page,intensityLabel,-2);
        SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(intensity);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff8beeff));
        bar.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean fromUser){
                intensity=Math.max(20,p);intensityLabel.setText("Cường độ ánh sáng: "+intensity+"%");
                if(fromUser)renderPreview();
            }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){save();}
        });
        add(page,bar,47);
        page.addView(space(18));
        Button pin=button("＋  Thêm widget vào màn hình Home");
        add(page,pin,55);pin.setOnClickListener(v->pinWidget());
        page.addView(space(10));
        Button update=button("↻  Cập nhật đồng hồ");
        add(page,update,50);
        update.setOnClickListener(v->{save();Toast.makeText(this,"Đã cập nhật",Toast.LENGTH_SHORT).show();});
        page.addView(space(14));
        Button exact=button("⏱  Quyền cập nhật phút chính xác");
        exact.setOnClickListener(v->exactAlarmSettings());
        add(page,exact,50);
        page.addView(space(14));
        add(page,label("Nếu không xuất hiện cửa sổ thêm widget: nhấn giữ khoảng trống trên Home → Widget → AURA Glass Clock → kéo ra màn hình.",13,0xffb6cee4,false),-2);
        page.addView(space(10));
        add(page,label("Không cần root, không thay Launcher, không thu thập dữ liệu. Khi máy ngủ sâu, Android có thể trì hoãn cập nhật để tiết kiệm pin.",12,0xff9fb7d0,false),-2);
    }
    private void pinWidget(){
        AppWidgetManager mgr=AppWidgetManager.getInstance(this);
        if(android.os.Build.VERSION.SDK_INT>=26 && mgr.isRequestPinAppWidgetSupported()){
            ComponentName component=new ComponentName(this,ClockProvider.class);
            mgr.requestPinAppWidget(component,null,null);
        }else{
            new AlertDialog.Builder(this).setTitle("Thêm widget")
                .setMessage("Nhấn giữ khoảng trống trên Home → Widget → AURA Glass Clock → kéo widget vào Home.")
                .setPositiveButton("Đã hiểu",null).show();
        }
    }
    private void exactAlarmSettings(){
        if(android.os.Build.VERSION.SDK_INT<31){
            Toast.makeText(this,"Phiên bản Android này không yêu cầu quyền đó",Toast.LENGTH_SHORT).show();
            return;
        }
        android.app.AlarmManager am=(android.app.AlarmManager)getSystemService(ALARM_SERVICE);
        if(am!=null && am.canScheduleExactAlarms()){
            Toast.makeText(this,"Đã có quyền đồng hồ chính xác",Toast.LENGTH_SHORT).show();return;
        }
        try{
            Intent intent=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:"+getPackageName()));
            startActivity(intent);
        }catch(Exception err){
            Toast.makeText(this,"Hãy bật quyền báo thức và nhắc nhở trong cài đặt ứng dụng",Toast.LENGTH_LONG).show();
        }
    }
}
