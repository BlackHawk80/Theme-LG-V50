package com.auraglass.clockwidget;

import android.content.Context;
import android.graphics.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class GlassArt {
    private GlassArt() {}
    public static String currentTime() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }
    private static int a(int color,int alpha) { return (color & 0x00ffffff) | (Math.max(0,Math.min(255,alpha))<<24); }
    public static Bitmap render(Context ctx,String time,int w,int h,int mode,int intensity) {
        w=Math.max(200,Math.min(900,w));
        h=Math.max(65,Math.min(300,h));
        intensity=Math.max(20,Math.min(100,intensity));
        Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bmp);
        canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
        Paint layout=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        layout.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        layout.setTextSize(h*0.75f);
        float textWidth=layout.measureText(time);
        if(textWidth>w*.91f) layout.setTextSize(layout.getTextSize()*((w*.91f)/textWidth));
        float x=(w-layout.measureText(time))/2f;
        Paint.FontMetrics fm=layout.getFontMetrics();
        float baseline=(h-(fm.ascent+fm.descent))/2f;
        Path glyph=new Path();
        layout.getTextPath(time,0,time.length(),x,baseline,glyph);
        RectF bounds=new RectF();glyph.computeBounds(bounds,true);
        float unit=Math.max(1f,Math.min(h,w/3.7f));
        float luminous=.35f+intensity/135f;

        // No background and no frame. All drawing is restricted to the digit shapes.
        Paint diffuse=new Paint(Paint.ANTI_ALIAS_FLAG);
        diffuse.setStyle(Paint.Style.FILL);
        diffuse.setColor(0x380A1637);
        diffuse.setShadowLayer(unit*.045f,0,unit*.026f,0xAA05152B);
        canvas.drawPath(glyph,diffuse);
        diffuse.clearShadowLayer();

        int[] outer=mode==1?new int[]{0x001CEAFF,0xFF93FAFF,0xFF47A4FF,0x00D0AAFF}:
                     mode==2?new int[]{0x009EE2FF,0xFFFFBFF1,0xFF34F4FB,0x00FFE0AD}:
                             new int[]{0x009DDBFF,0xFFFFFFFF,0xFF9FB8EE,0x00DAF4FF};
        Paint bloom=new Paint(Paint.ANTI_ALIAS_FLAG);
        bloom.setStyle(Paint.Style.STROKE);
        bloom.setStrokeWidth(unit*.042f);
        bloom.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,outer,null,Shader.TileMode.CLAMP));
        bloom.setMaskFilter(new BlurMaskFilter(unit*.04f,BlurMaskFilter.Blur.NORMAL));
        bloom.setAlpha(Math.round(165*luminous));
        canvas.drawPath(glyph,bloom);
        bloom.setMaskFilter(null);

        // Translucent optical volume: layered icy glass colors with a luminous center.
        Paint body=new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] palette=mode==1?
            new int[]{0xF1ECFFFF,0xB7AAECFF,0xD2ECFFFF,0xCF75C8ED,0xF4DBFAFF}:
            mode==2?
            new int[]{0xF7F7F5FF,0xB8A2D7FF,0xDAF4FFFF,0xCD91B6E7,0xF2DDFDFF}:
            new int[]{0xE8F5FCFF,0xB8AFC9E1,0xD2EDF7FF,0xCF9BBDD9,0xF7FCFFFF};
        body.setShader(new LinearGradient(0,bounds.top,0,bounds.bottom,palette,
                new float[]{0,.28f,.52f,.8f,1},Shader.TileMode.CLAMP));
        canvas.drawPath(glyph,body);

        // Holographic caustics and light streaks are clipped entirely inside the glyphs.
        canvas.save();
        canvas.clipPath(glyph);
        Paint caustic=new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] flashes=mode==0?
            new int[]{0x30FFFFFF,0x20E1F3FF,0x00FFFFFF}:
            new int[]{0x7D9CF8FF,0x5CF4ACEE,0x0077BFFF};
        caustic.setShader(new RadialGradient(bounds.left+bounds.width()*.28f,bounds.top+bounds.height()*.27f,
                bounds.width()*.59f,flashes,null,Shader.TileMode.CLAMP));
        canvas.drawRect(bounds,caustic);
        caustic.setShader(new RadialGradient(bounds.left+bounds.width()*.85f,bounds.bottom-bounds.height()*.15f,
            bounds.width()*.48f,mode==2?
                new int[]{0x8DFFBCE7,0x68A8FFF8,0x00FFEAEA}:
                new int[]{0x83E6EEFF,0x5486E8FF,0x00FFFFFF},null,Shader.TileMode.CLAMP));
        canvas.drawRect(bounds,caustic);
        Paint sheen=new Paint(Paint.ANTI_ALIAS_FLAG);
        sheen.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,
            new int[]{0x73FFFFFF,0x04FFFFFF,0x51BAEEFF,0x00FFFFFF},
            new float[]{0,.42f,.62f,1},Shader.TileMode.CLAMP));
        canvas.drawRect(bounds,sheen);
        canvas.restore();

        // Multi-chromatic beveled edges and polished top lip.
        Paint rim=new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(unit*.020f);
        rim.setStrokeJoin(Paint.Join.ROUND);
        rim.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,
            new int[]{0xFFD9FBFF,0xFE7BF9FF,0xF7FFDFFB,0xFFCBF8FF,0xFFD5B3FC},null,Shader.TileMode.CLAMP));
        canvas.drawPath(glyph,rim);

        Paint fine=new Paint(Paint.ANTI_ALIAS_FLAG);
        fine.setStyle(Paint.Style.STROKE);
        fine.setStrokeJoin(Paint.Join.ROUND);
        fine.setStrokeWidth(Math.max(1f,unit*.007f));
        fine.setColor(a(Color.WHITE,Math.round(200*luminous)));
        canvas.save();canvas.translate(0,-unit*.008f);
        canvas.drawPath(glyph,fine);
        canvas.restore();

        Paint bottom=new Paint(Paint.ANTI_ALIAS_FLAG);
        bottom.setStyle(Paint.Style.STROKE);
        bottom.setStrokeWidth(unit*.014f);
        bottom.setColor(mode==2?0xBFFFF0FC:0xAF65E3FF);
        canvas.save();canvas.translate(0,unit*.009f);canvas.drawPath(glyph,bottom);canvas.restore();
        return bmp;
    }
}
