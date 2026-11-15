
package com.auraglass.launcher;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private final Locale vn=new Locale("vi","VN");
    private final Handler handler=new Handler();
    private SharedPreferences prefs;
    private FrameLayout root;
    private TextView hourText, dateText, batteryText;
    private View allAppsPanel;
    private boolean light=false;
    private int opacity=55;
    private final ArrayList<Item> apps=new ArrayList<>();
    private final Item[] dock=new Item[4];
    private final Runnable tick=new Runnable() {
        public void run() { updateTime();handler.postDelayed(this,30000); }
    };
    static class Item {
        String title,pkg,cls;
        Drawable icon;
        Item(String a,String b,String c,Drawable d){title=a;pkg=b;cls=c;icon=d;}
        String key(){return pkg+"/"+cls;}
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("aura",MODE_PRIVATE);
        light=prefs.getBoolean("light",false);
        opacity=prefs.getInt("opacity",light?100:55);
        loadApps();loadDock();build();
    }
    @Override protected void onResume(){
        super.onResume();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }
    @Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}
    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    int fg(){return light?0xff1b3850:0xfff8fbff;}
    int sub(){return light?0xff49718c:0xffbcd4e8;}
    LinearLayout col(){LinearLayout a=new LinearLayout(this);a.setOrientation(1);return a;}
    LinearLayout row(){LinearLayout a=new LinearLayout(this);a.setGravity(Gravity.CENTER_VERTICAL);return a;}
    TextView text(String s,int size,boolean bold,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif-light",Typeface.NORMAL));
        return t;
    }
    GradientDrawable glass(int radius){
        int a=Math.max(25,Math.min(175,opacity));
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
             new int[]{Color.argb(a,255,255,255),Color.argb(a/3,160,210,255)});
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1),Color.argb(light?140:95,255,255,255));
        return g;
    }
    View gap(int height){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(height)));return v;}
    void loadApps(){
        apps.clear();PackageManager pm=getPackageManager();
        Intent q=new Intent(Intent.ACTION_MAIN);q.addCategory(Intent.CATEGORY_LAUNCHER);
        HashSet<String> keys=new HashSet<>();
        for(ResolveInfo ri:pm.queryIntentActivities(q,0)){
            if(ri.activityInfo==null||getPackageName().equals(ri.activityInfo.packageName))continue;
            String pkg=ri.activityInfo.packageName,cls=ri.activityInfo.name;
            if(keys.add(pkg+"/"+cls)){
                try {apps.add(new Item(ri.loadLabel(pm).toString(),pkg,cls,ri.loadIcon(pm)));}
                catch(Exception ignored){}
            }
        }
        Collator compare=Collator.getInstance(vn);
        Collections.sort(apps,(a,b)->compare.compare(a.title,b.title));
    }
    Item byKey(String k){for(Item a:apps)if(a.key().equals(k))return a;return null;}
    Item byPackage(String... names){
        for(String n:names)for(Item a:apps)if(a.pkg.equals(n))return a;
        for(String n:names)for(Item a:apps)if(a.pkg.contains(n))return a;
        return null;
    }
    Item defaultDock(int i){
        if(i==0)return byPackage("com.google.android.dialer","com.android.dialer","com.lge.phone");
        if(i==1)return byPackage("com.google.android.apps.messaging","com.android.mms","com.lge.message");
        if(i==2)return byPackage("com.android.chrome","org.mozilla.firefox","com.sec.android.app.sbrowser");
        return byPackage("com.lge.camera","com.android.camera2","com.google.android.GoogleCamera");
    }
    void loadDock(){
        for(int i=0;i<4;i++){
            Item saved=byKey(prefs.getString("dock"+i,""));
            dock[i]=saved==null?defaultDock(i):saved;
        }
    }
    void launch(Item item){
        if(item==null){showApps();return;}
        Intent i=new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setClassName(item.pkg,item.cls);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try{startActivity(i);}catch(Exception e){Toast.makeText(this,"Không mở được "+item.title,Toast.LENGTH_SHORT).show();}
    }
    void build(){
        allAppsPanel=null;
        int status=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if(light)status|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        getWindow().getDecorView().setSystemUiVisibility(status);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(light?0xffaed9ec:0xff0c2542);
        root=new FrameLayout(this);
        root.addView(new Background(this),new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        LinearLayout page=col();page.setPadding(dp(20),dp(42),dp(20),dp(16));
        root.addView(page,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout heading=row();
        TextView logo=text("AURA  GLASS",19,true,fg());
        logo.setLetterSpacing(.11f);
        heading.addView(logo,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView setting=text("⚙",23,true,fg());setting.setGravity(Gravity.CENTER);setting.setBackground(glass(18));
        heading.addView(setting,new LinearLayout.LayoutParams(dp(46),dp(46)));
        setting.setOnClickListener(v->settings());
        heading.addView(new View(this),new LinearLayout.LayoutParams(dp(8),1));
        TextView appsButton=text("▦",25,true,fg());appsButton.setGravity(Gravity.CENTER);appsButton.setBackground(glass(18));
        heading.addView(appsButton,new LinearLayout.LayoutParams(dp(46),dp(46)));appsButton.setOnClickListener(v->showApps());
        page.addView(heading);
        page.addView(gap(22));
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout mid=col();scroll.addView(mid);
        Calendar cal=Calendar.getInstance();
        int h=cal.get(Calendar.HOUR_OF_DAY);
        mid.addView(text(h<11?"Chào buổi sáng":h<18?"Chào buổi chiều":"Chào buổi tối",16,false,sub()));
        mid.addView(gap(8));
        mid.addView(text("Không gian của bạn.",29,true,fg()));
        mid.addView(gap(22));
        LinearLayout timeCard=col();timeCard.setPadding(dp(20),dp(17),dp(15),dp(15));
        timeCard.setBackground(glass(29));
        mid.addView(timeCard,new LinearLayout.LayoutParams(-1,dp(164)));
        timeCard.addView(text("HÔM NAY",12,true,sub()));
        hourText=text("--:--",62,false,fg());timeCard.addView(hourText);
        dateText=text("",15,false,sub());timeCard.addView(dateText);
        timeCard.setOnClickListener(v->safeStart(new Intent(AlarmClock.ACTION_SHOW_ALARMS)));
        mid.addView(gap(13));
        LinearLayout widgets=row();mid.addView(widgets);
        LinearLayout c1=col();c1.setBackground(glass(22));c1.setPadding(dp(16),dp(14),dp(8),0);
        c1.addView(text("LỊCH",12,true,sub()));
        c1.addView(gap(8));c1.addView(text("✦  Mở lịch",15,false,fg()));
        widgets.addView(c1,new LinearLayout.LayoutParams(0,dp(100),1));
        c1.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_APP_CALENDAR);safeStart(i);});
        widgets.addView(new View(this),new LinearLayout.LayoutParams(dp(12),1));
        LinearLayout c2=col();c2.setBackground(glass(22));c2.setPadding(dp(16),dp(14),dp(8),0);
        c2.addView(text("PIN",12,true,sub()));c2.addView(gap(7));
        batteryText=text("--%",24,true,fg());c2.addView(batteryText);
        widgets.addView(c2,new LinearLayout.LayoutParams(0,dp(100),1));
        c2.setOnClickListener(v->safeStart(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)));
        mid.addView(gap(28));
        LinearLayout line=row();mid.addView(line);
        TextView tag=text("ỨNG DỤNG",12,true,sub());tag.setLetterSpacing(.15f);
        line.addView(tag,new LinearLayout.LayoutParams(0,dp(36),1));
        TextView all=text("Xem tất cả  ›",13,false,fg());line.addView(all);all.setOnClickListener(v->showApps());
        LinearLayout fav=col();mid.addView(fav);
        ArrayList<Item> first=new ArrayList<>();
        String[] common={"com.android.chrome","com.google.android.youtube","com.google.android.apps.photos","com.google.android.gm","com.spotify.music","com.google.android.apps.maps"};
        for(String n:common){Item a=byPackage(n);if(a!=null&&!first.contains(a))first.add(a);}
        for(Item a:apps){if(first.size()>=8)break;if(!first.contains(a))first.add(a);}
        showGrid(fav,first,8);
        mid.addView(gap(12));
        TextView instruction=text("Nhấn giữ ứng dụng để thêm vào Dock",12,false,sub());instruction.setGravity(Gravity.CENTER);
        mid.addView(instruction);
        page.addView(gap(10));
        LinearLayout dockView=row();dockView.setBackground(glass(33));dockView.setPadding(dp(7),dp(8),dp(7),dp(8));
        page.addView(dockView,new LinearLayout.LayoutParams(-1,dp(90)));
        for(int i=0;i<4;i++){
            final int pos=i;
            Item a=dock[i];
            LinearLayout slot=col();slot.setGravity(Gravity.CENTER);
            dockView.addView(slot,new LinearLayout.LayoutParams(0,-1,1));
            slot.addView(icon(a),new LinearLayout.LayoutParams(dp(58),dp(58)));
            slot.setOnClickListener(v->launch(dock[pos]));
            slot.setOnLongClickListener(v->{showApps();return true;});
        }
        TextView footer=text("⌃  Nhấn ▦ để mở tất cả ứng dụng",11,false,sub());
        footer.setGravity(Gravity.CENTER);footer.setPadding(0,dp(8),0,0);
        footer.setOnClickListener(v->showApps());page.addView(footer);
        updateTime();
    }
    View icon(Item a){
        FrameLayout frame=new FrameLayout(this);frame.setBackground(glass(17));
        if(a!=null&&a.icon!=null){
            ImageView image=new ImageView(this);image.setImageDrawable(a.icon);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setPadding(dp(11),dp(11),dp(11),dp(11));
            frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
        }else{
            TextView sym=text("+",27,true,fg());sym.setGravity(Gravity.CENTER);
            frame.addView(sym,new FrameLayout.LayoutParams(-1,-1));
        }
        return frame;
    }
    void showGrid(LinearLayout wrap,List<Item> source,int count){
        wrap.removeAllViews();
        if(source.isEmpty()){wrap.addView(text("Không tìm thấy ứng dụng",14,false,sub()));return;}
        int n=count==0?source.size():Math.min(count,source.size());
        for(int start=0;start<n;start+=4){
            LinearLayout r=row();r.setGravity(Gravity.TOP);wrap.addView(r);
            for(int j=0;j<4;j++){
                if(start+j>=n){r.addView(new View(this),new LinearLayout.LayoutParams(0,dp(104),1));continue;}
                Item a=source.get(start+j);
                LinearLayout tile=col();tile.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
                tile.setPadding(dp(3),dp(6),dp(3),dp(5));
                r.addView(tile,new LinearLayout.LayoutParams(0,dp(108),1));
                tile.addView(icon(a),new LinearLayout.LayoutParams(dp(60),dp(60)));
                TextView name=text(a.title,11,false,fg());name.setMaxLines(2);name.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
                name.setEllipsize(TextUtils.TruncateAt.END);name.setPadding(0,dp(6),0,0);
                tile.addView(name,new LinearLayout.LayoutParams(-1,dp(36)));
                tile.setOnClickListener(v->launch(a));
                tile.setOnLongClickListener(v->{pin(a);return true;});
            }
        }
    }
    void showApps(){
        if(allAppsPanel!=null)return;
        LinearLayout overlay=col();overlay.setPadding(dp(16),dp(37),dp(16),dp(18));
        overlay.setBackgroundColor(light?0xf2d5e8f5:0xf00c2442);
        root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));allAppsPanel=overlay;
        LinearLayout top=row();overlay.addView(top);
        top.addView(text("Tất cả ứng dụng",23,true,fg()),new LinearLayout.LayoutParams(0,dp(51),1));
        TextView close=text("×",26,true,fg());close.setGravity(Gravity.CENTER);close.setBackground(glass(18));
        top.addView(close,new LinearLayout.LayoutParams(dp(45),dp(45)));close.setOnClickListener(v->hideApps());
        EditText search=new EditText(this);search.setTextColor(fg());search.setHintTextColor(sub());
        search.setSingleLine();search.setTextSize(15);search.setHint("Tìm ứng dụng…");
        search.setPadding(dp(15),dp(9),dp(15),dp(9));search.setBackground(glass(16));
        overlay.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));
        overlay.addView(gap(15));
        ScrollView listScroller=new ScrollView(this);listScroller.setVerticalScrollBarEnabled(false);
        overlay.addView(listScroller,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout grid=col();listScroller.addView(grid);showGrid(grid,apps,0);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            public void onTextChanged(CharSequence s,int a,int b,int c){
                String q=s.toString().toLowerCase(vn).trim();
                ArrayList<Item> filtered=new ArrayList<>();
                for(Item app:apps)if(app.title.toLowerCase(vn).contains(q))filtered.add(app);
                showGrid(grid,filtered,0);listScroller.scrollTo(0,0);
            }
            public void afterTextChanged(Editable e){}
        });
        overlay.setAlpha(0);overlay.animate().alpha(1).setDuration(180).start();
    }
    void hideApps(){
        View current=allAppsPanel;
        if(current==null)return;
        allAppsPanel=null;
        try{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(current.getWindowToken(),0);}catch(Exception ignored){}
        root.removeView(current);
    }
    @Override public void onBackPressed(){if(allAppsPanel!=null)hideApps();else super.onBackPressed();}
    void pin(Item a){
        new AlertDialog.Builder(this).setTitle("Ghim "+a.title+" vào Dock")
            .setItems(new String[]{"Vị trí 1","Vị trí 2","Vị trí 3","Vị trí 4"},(dlg,pos)->{
                prefs.edit().putString("dock"+pos,a.key()).apply();loadDock();build();
                Toast.makeText(this,"Đã ghim vào Dock",Toast.LENGTH_SHORT).show();
            }).show();
    }
    void updateTime(){
        if(hourText!=null)hourText.setText(new SimpleDateFormat("HH:mm",vn).format(new Date()));
        if(dateText!=null)dateText.setText(new SimpleDateFormat("EEEE, dd 'tháng' MM yyyy",vn).format(new Date()));
        if(batteryText!=null){
            Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int value=i==null?-1:i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            int scale=i==null?100:i.getIntExtra(BatteryManager.EXTRA_SCALE,100);
            batteryText.setText(value<0?"--%":Math.round(value*100f/Math.max(1,scale))+"%");
        }
    }
    void safeStart(Intent intent){
        try{startActivity(intent);}catch(Exception e){Toast.makeText(this,"Không hỗ trợ trên thiết bị này",Toast.LENGTH_SHORT).show();}
    }
    void settings(){
        new AlertDialog.Builder(this).setTitle("AURA GLASS")
        .setItems(new String[]{"☼  Đổi Sáng / Tối","◌  Điều chỉnh độ trong suốt","▧  Đặt hình nền AURA","⌂  Chọn màn hình Home","⚙  Cài đặt hệ thống","↻  Đặt lại Dock"},
            (dialog,choice)->{
                switch(choice){
                case 0:light=!light;prefs.edit().putBoolean("light",light).apply();build();break;
                case 1:
                    SeekBar sb=new SeekBar(this);sb.setMax(170);sb.setProgress(opacity);
                    new AlertDialog.Builder(this).setTitle("Độ trong của kính").setView(sb)
                        .setPositiveButton("Áp dụng",(a,b)->{opacity=Math.max(25,sb.getProgress());prefs.edit().putInt("opacity",opacity).apply();build();})
                        .setNegativeButton("Hủy",null).show();
                    break;
                case 2:
                    new AlertDialog.Builder(this).setTitle("Hình nền AURA")
                        .setMessage("Thay hình nền hiện tại bằng nền AURA?")
                        .setPositiveButton("Áp dụng",(a,b)->wallpaper())
                        .setNegativeButton("Hủy",null).show();
                    break;
                case 3:safeStart(new Intent(Settings.ACTION_HOME_SETTINGS));break;
                case 4:safeStart(new Intent(Settings.ACTION_SETTINGS));break;
                case 5:
                    SharedPreferences.Editor edit=prefs.edit();
                    for(int i=0;i<4;i++)edit.remove("dock"+i);
                    edit.apply();loadDock();build();break;
                }
            }).setNegativeButton("Đóng",null).show();
    }
    void wallpaper(){
        try{
            View v=new Background(this);
            int w=720,h=1600;
            v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
            v.layout(0,0,w,h);
            Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            v.draw(new Canvas(bmp));
            WallpaperManager.getInstance(this).setBitmap(bmp);
            bmp.recycle();
            Toast.makeText(this,"Đã đặt hình nền AURA",Toast.LENGTH_SHORT).show();
        }catch(Exception ex){Toast.makeText(this,"Không đặt được hình nền",Toast.LENGTH_LONG).show();}
    }
    class Background extends View {
        final Paint p=new Paint(3);
        Background(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){
            int w=getWidth(),h=getHeight();if(w==0||h==0)return;
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0,0,w,h,light?
                new int[]{0xffb6e4f3,0xff98c9e7,0xffc3b8ee,0xff8cdcd2}:
                new int[]{0xff071b31,0xff17496d,0xff344b79,0xff092e45},
                null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);
            p.setShader(new RadialGradient(w*.24f,h*.37f,w*.88f,
                light?new int[]{0x77ffffff,0x00ffffff}:new int[]{0x6626c5ec,0x0026c5ec},null,Shader.TileMode.CLAMP));
            c.drawCircle(w*.24f,h*.37f,w*.88f,p);
            p.setShader(new RadialGradient(w*.88f,h*.83f,w*.92f,
                light?new int[]{0x448abef7,0x008abef7}:new int[]{0x449f8cef,0x009f8cef},null,Shader.TileMode.CLAMP));
            c.drawCircle(w*.88f,h*.83f,w*.92f,p);
            Path flow=new Path();
            flow.moveTo(-w*.4f,h*.81f);
            flow.cubicTo(w*.17f,h*.45f,w*.79f,h*.53f,w*1.2f,h*.09f);
            flow.lineTo(w*1.44f,h*.41f);
            flow.cubicTo(w*.63f,h*.93f,w*.32f,h*.91f,-w*.4f,h*1.09f);
            flow.close();
            p.setShader(new LinearGradient(0,h*.33f,w,h*.82f,
                new int[]{0x22ffffff,0x559bd2ff,0x11ffffff},null,Shader.TileMode.CLAMP));
            c.drawPath(flow,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1f,w*.002f));
            p.setColor(0x60ffffff);c.drawPath(flow,p);
            p.setStyle(Paint.Style.FILL);
        }
    }
}
