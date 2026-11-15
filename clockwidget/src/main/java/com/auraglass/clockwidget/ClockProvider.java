package com.auraglass.clockwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

public class ClockProvider extends AppWidgetProvider {
    public static final String ACTION_TICK="com.auraglass.clockwidget.UPDATE_TIME";
    public static final String ACTION_REFRESH="com.auraglass.clockwidget.REFRESH_WIDGET";
    public static final String PREFS="glass_clock_settings";

    @Override public void onEnabled(Context context) {
        super.onEnabled(context);updateAll(context);scheduleNext(context);
    }
    @Override public void onUpdate(Context ctx,AppWidgetManager manager,int[] ids) {
        super.onUpdate(ctx,manager,ids);
        for(int id:ids)updateOne(ctx,manager,id);
        scheduleNext(ctx);
    }
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle opts){
        super.onAppWidgetOptionsChanged(c,m,id,opts);
        updateOne(c,m,id);
    }
    @Override public void onReceive(Context c,Intent intent){
        super.onReceive(c,intent);
        String action=intent==null?null:intent.getAction();
        if(ACTION_TICK.equals(action)||ACTION_REFRESH.equals(action)||
           Intent.ACTION_TIME_CHANGED.equals(action)||
           Intent.ACTION_TIMEZONE_CHANGED.equals(action)||
           Intent.ACTION_DATE_CHANGED.equals(action)||
           Intent.ACTION_BOOT_COMPLETED.equals(action)||
           Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)){
            updateAll(c);scheduleNext(c);
        }
    }
    @Override public void onDeleted(Context c,int[] ids){
        super.onDeleted(c,ids);
        if(widgetIds(c).length==0)cancelNext(c);
    }
    @Override public void onDisabled(Context c){
        cancelNext(c);super.onDisabled(c);
    }
    private static int[] widgetIds(Context c) {
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,ClockProvider.class));
    }
    public static void updateAll(Context ctx){
        AppWidgetManager mgr=AppWidgetManager.getInstance(ctx);
        for(int id:widgetIds(ctx))updateOne(ctx,mgr,id);
    }
    private static void updateOne(Context c,AppWidgetManager m,int id){
        Bundle opts=m.getAppWidgetOptions(id);
        float density=c.getResources().getDisplayMetrics().density;
        int dpw=opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,280);
        int dph=opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,100);
        int width=Math.max(320,Math.min(880,(int)(dpw*density)));
        int height=Math.max(95,Math.min(280,(int)(dph*density)));
        SharedPreferences pref=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        int mode=pref.getInt("mode",2);
        int strength=pref.getInt("strength",82);
        String time=GlassArt.currentTime();
        Bitmap bitmap=GlassArt.render(c,time,width,height,mode,strength);
        RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.glass_clock_widget);
        views.setImageViewBitmap(R.id.glass_time,bitmap);
        views.setContentDescription(R.id.glass_time,"Đồng hồ "+time);
        Intent open=new Intent(c,MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent click=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.glass_time,click);
        m.updateAppWidget(id,views);
    }
    private static PendingIntent alarmIntent(Context c){
        Intent tick=new Intent(c,ClockProvider.class).setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(c,10,tick,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static void scheduleNext(Context c){
        if(widgetIds(c).length==0)return;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        long now=System.currentTimeMillis();
        long next=((now/60000L)+1L)*60000L+120L;
        PendingIntent pi=alarmIntent(c);
        try{
            if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()){
                am.setAndAllowWhileIdle(AlarmManager.RTC,next,pi);
            }else{
                am.setExactAndAllowWhileIdle(AlarmManager.RTC,next,pi);
            }
        }catch(SecurityException ex){
            am.setAndAllowWhileIdle(AlarmManager.RTC,next,pi);
        }
    }
    private static void cancelNext(Context c){
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am!=null)am.cancel(alarmIntent(c));
    }
}
