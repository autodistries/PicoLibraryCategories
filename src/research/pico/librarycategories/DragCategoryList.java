package research.pico.librarycategories;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Preview with translations; reparent only after UP so the grabbed handle keeps touch ownership. */
final class DragCategoryList extends LinearLayout {
    interface Moved {boolean move(int from,int to);}
    private final ScrollView scroll;
    private final Moved moved;
    private final List<View> rows=new ArrayList<>();
    private final int[] location=new int[2];
    private View dragging;private int pointer=-1,source,target;
    private float rawY,grabOffset,oldElevation,oldAlpha;
    private final Runnable autoScroll=new Runnable(){public void run(){
        if(dragging==null)return;
        scroll.getLocationOnScreen(location);int edge=dp(48),step=0;
        if(rawY<location[1]+edge)step=-dp(10);
        else if(rawY>location[1]+scroll.getHeight()-edge)step=dp(10);
        if(step!=0){scroll.scrollBy(0,step);preview();}
        postDelayed(this,16);
    }};
    DragCategoryList(Context c,ScrollView scroll,Moved moved){super(c);this.scroll=scroll;this.moved=moved;setOrientation(VERTICAL);}
    int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    void addRow(View row,View handle){
        rows.add(row);addView(row);
        handle.setContentDescription("Drag to reorder category");
        handle.setOnTouchListener((v,event)->{
            int action=event.getActionMasked();
            if(action==android.view.MotionEvent.ACTION_DOWN){
                cancel();dragging=row;source=rows.indexOf(row);target=source;pointer=event.getPointerId(0);rawY=event.getRawY();
                row.getLocationOnScreen(location);grabOffset=rawY-location[1];oldElevation=row.getElevation();oldAlpha=row.getAlpha();
                row.setElevation(dp(12));row.setAlpha(.85f);scroll.requestDisallowInterceptTouchEvent(true);
                post(autoScroll);return true;
            }
            if(dragging!=row)return false;
            if(action==android.view.MotionEvent.ACTION_CANCEL){cancel();return true;}
            int p=event.findPointerIndex(pointer);if(p<0){cancel();return true;}
            // PICO injects one touch pointer; also account for a secondary Android pointer's local offset.
            rawY=event.getRawY()+event.getY(p)-event.getY(0);
            if(action==android.view.MotionEvent.ACTION_MOVE){preview();return true;}
            if(action==android.view.MotionEvent.ACTION_UP){
                preview();int from=source,to=target;View item=dragging;cancel();
                if(from!=to&&moved.move(from,to)){
                    rows.remove(from);rows.add(to,item);removeView(item);addView(item,to);
                    item.announceForAccessibility("Category moved to position "+(to+1));
                }
                v.performClick();return true;
            }
            if(action==android.view.MotionEvent.ACTION_POINTER_UP&&event.getPointerId(event.getActionIndex())==pointer)cancel();
            return true;
        });
    }
    private void preview(){
        if(dragging==null)return;
        getLocationOnScreen(location);
        float top=Math.max(0,Math.min(getHeight()-dragging.getHeight(),rawY-location[1]-grabOffset));
        float[] midpoints=new float[rows.size()];for(int i=0;i<rows.size();i++)midpoints[i]=rows.get(i).getTop()+rows.get(i).getHeight()/2f;
        target=ReorderMath.target(top+dragging.getHeight()/2f,midpoints,source);
        int gap=dragging.getHeight();
        for(int i=0;i<rows.size();i++){
            View row=rows.get(i);float shift=0;
            if(row==dragging)shift=top-row.getTop();
            else if(target>source&&i>source&&i<=target)shift=-gap;
            else if(target<source&&i>=target&&i<source)shift=gap;
            row.setTranslationY(shift);
        }
    }
    private void cancel(){
        removeCallbacks(autoScroll);
        for(View row:rows)row.setTranslationY(0);
        if(dragging!=null){dragging.setElevation(oldElevation);dragging.setAlpha(oldAlpha);}
        dragging=null;pointer=-1;scroll.requestDisallowInterceptTouchEvent(false);
    }
    @Override protected void onDetachedFromWindow(){cancel();super.onDetachedFromWindow();}
}
