package research.pico.librarycategories;

import android.content.Context;
import java.util.*;
import org.json.*;

final class Categories extends CategoryModel {
    private final Context context;
    Categories(Context context){
        this.context=context.getApplicationContext();
        try{
            JSONArray data=new JSONArray(context.getSharedPreferences("pico_library_categories",0).getString("data","[]"));
            HashSet<String> ids=new HashSet<>();
            for(int i=0;i<data.length();i++){
                JSONObject row=data.getJSONObject(i);String id=row.getString("id"),name=row.getString("name");
                if(id.isEmpty()||name.trim().isEmpty()||!ids.add(id))continue;
                Category c=new Category(id,name);c.hidden=row.optBoolean("hidden",false);
                JSONArray apps=row.optJSONArray("members");if(apps!=null)for(int n=0;n<apps.length();n++)c.members.add(apps.getString(n));items.add(c);
            }
            ensureHidden();
        }catch(JSONException failure){throw new IllegalStateException("Saved category data is invalid; left unchanged",failure);}
    }
    void setAppHidden(String pkg,boolean hide){
        Category c=ensureHidden();
        if(hide)c.members.add(pkg);
        else c.members.remove(pkg);
        save();
    }
    void save(){
        try{JSONArray data=new JSONArray();for(Category c:items){JSONObject row=new JSONObject();row.put("id",c.id);row.put("name",c.name);row.put("hidden",c.hidden);row.put("members",new JSONArray(new TreeSet<>(c.members)));data.put(row);}
            if(!context.getSharedPreferences("pico_library_categories",0).edit().putString("data",data.toString()).commit())throw new IllegalStateException("Could not save categories");
        }catch(JSONException failure){throw new IllegalStateException(failure);}
    }
}

