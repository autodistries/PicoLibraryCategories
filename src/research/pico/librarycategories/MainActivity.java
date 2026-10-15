package research.pico.librarycategories;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

public final class MainActivity extends Activity {
    public boolean isModuleActive(){return false;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(28,28,28,28);
        TextView info=new TextView(this);info.setText("PICO Library Categories\n\nEnable this module for Library (com.pvr.appmanager) and this app in LSPosed, then restart Library or reboot.\n\nLibrary gains Manage categories… in its sidebar and Categories… in app menus. Apps can belong to multiple custom categories.\n\nModule active: "+isModuleActive());box.addView(info);
        Button open=new Button(this);open.setText("Open category manager");box.addView(open);
        open.setOnClickListener(v->{Intent i=getPackageManager().getLaunchIntentForPackage("com.pvr.appmanager");
            if(i==null){Toast.makeText(this,"Library is unavailable",Toast.LENGTH_LONG).show();return;}
            i.putExtra("pico_manage_categories",true);startActivity(i);});setContentView(box);
    }
}
