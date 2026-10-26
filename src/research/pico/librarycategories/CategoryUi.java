package research.pico.librarycategories;

import android.app.*;
import android.widget.*;
import java.util.*;
import de.robv.android.xposed.*;

final class CategoryUi {
    private static boolean save(Activity a,Categories data,Runnable changed){
        try{data.save();}
        catch(Throwable failure){XposedBridge.log("PICO Categories: save failed "+failure);Toast.makeText(a,"Could not save categories",Toast.LENGTH_LONG).show();return false;}
        try{changed.run();}catch(Throwable failure){XposedBridge.log("PICO Categories: refresh failed "+failure);Toast.makeText(a,"Saved. Reopen Library to refresh.",Toast.LENGTH_LONG).show();}
        return true;
    }
    private interface NameResult {void accept(String name);}
    private static void name(Activity a,String title,String initial,NameResult result){
        EditText edit=new EditText(a);edit.setSingleLine(true);edit.setText(initial);edit.selectAll();
        AlertDialog d=new AlertDialog.Builder(a).setTitle(title).setView(edit).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            String value=edit.getText().toString().trim();if(value.isEmpty()||value.length()>40){edit.setError("Use 1–40 characters");return;}
            d.dismiss();result.accept(value);
        }));d.show();
    }
    static void manage(Activity a,Categories data,Runnable changed){
        if(a.isFinishing())return;
        LinearLayout box=new LinearLayout(a);box.setOrientation(1);box.setPadding(20,12,20,12);
        TextView help=new TextView(a);help.setText("Hold the ☰ handle and drag to reorder. Move near the list edges to scroll. Release to save. Long-press an app to choose its categories.");box.addView(help);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Library categories").setPositiveButton("Done",null).setNeutralButton("New category",null).create();
        ScrollView scroll=new ScrollView(a);
        DragCategoryList list=new DragCategoryList(a,scroll,(from,to)->{
            Categories.Category c=data.items.get(from);data.moveTo(c,to);
            if(save(a,data,changed))return true;
            data.moveTo(c,from);return false;
        });box.addView(list);
        for(Categories.Category c:new ArrayList<>(data.items)){
            LinearLayout card=new LinearLayout(a);card.setPadding(0,list.dp(8),0,list.dp(8));
            TextView handle=new TextView(a);handle.setText("☰");handle.setTextSize(26);handle.setGravity(android.view.Gravity.CENTER);handle.setClickable(true);
            card.addView(handle,new LinearLayout.LayoutParams(list.dp(52),-1));
            LinearLayout details=new LinearLayout(a);details.setOrientation(1);card.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            TextView title=new TextView(a);title.setText(c.name+(c.hidden?" (hidden)":""));title.setTextSize(18);details.addView(title);
            LinearLayout row=new LinearLayout(a);details.addView(row);list.addRow(card,handle);
            add(row,"Rename",()->{dialog.dismiss();name(a,"Rename category",c.name,value->{c.name=value;save(a,data,changed);manage(a,data,changed);});});
            add(row,c.hidden?"Show":"Hide",()->{c.hidden=!c.hidden;save(a,data,changed);dialog.dismiss();manage(a,data,changed);});
            if(c.custom()){
                add(row,"Apps…",()->{dialog.dismiss();pickApps(a,data,c,changed);});
                if(!c.id.equals(CategoryModel.HIDDEN_ID)){
                    add(row,"Delete",()->new AlertDialog.Builder(a).setTitle("Delete "+c.name+"?").setMessage("Apps stay installed. Only this category is removed.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{data.items.remove(c);save(a,data,changed);dialog.dismiss();manage(a,data,changed);}).show());
                }
            }
        }
        scroll.addView(box);dialog.setView(scroll);
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(button->{dialog.dismiss();name(a,"New category","",value->{data.create(value);save(a,data,changed);manage(a,data,changed);});}));dialog.show();
    }
    private static void add(LinearLayout row,String text,Runnable action){Button b=new Button(row.getContext());b.setText(text);row.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->action.run());}
    static void assign(Activity a,Categories data,String pkg,String label,Runnable changed){
        List<Categories.Category> list=new ArrayList<>();for(Categories.Category c:data.items)if(c.custom()&&!c.id.equals(CategoryModel.HIDDEN_ID))list.add(c);
        String[] names=new String[list.size()];boolean[] selected=new boolean[list.size()];
        for(int i=0;i<list.size();i++){names[i]=list.get(i).name+(list.get(i).hidden?" (hidden)":"");selected[i]=list.get(i).members.contains(pkg);}
        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle(label+" — categories").setNegativeButton("Cancel",null)
            .setPositiveButton("Save",(d,w)->{for(int i=0;i<list.size();i++){if(selected[i])list.get(i).members.add(pkg);else list.get(i).members.remove(pkg);}save(a,data,changed);})
            .setNeutralButton("New category",(d,w)->name(a,"New category","",value->{Categories.Category c=data.create(value);c.members.add(pkg);save(a,data,changed);assign(a,data,pkg,label,changed);}));
        if(list.isEmpty())b.setMessage("Create a category to start organizing this app.");
        else b.setMultiChoiceItems(names,selected,(d,which,checked)->selected[which]=checked);
        b.show();
    }
    private static void pickApps(Activity a,Categories data,Categories.Category category,Runnable changed){
        try{
            List<?> apps=(List<?>)XposedHelpers.callMethod(a,"getMAllApp");
            TreeMap<String,String> names=new TreeMap<>();for(Object app:apps){String pkg=HookEntry.pkg(app);String name=String.valueOf(XposedHelpers.callMethod(app,"getName"));names.put(pkg,name.isEmpty()?pkg:name);}
            List<String> packages=new ArrayList<>(names.keySet());packages.sort(Comparator.comparing(p->names.get(p),String.CASE_INSENSITIVE_ORDER));
            String[] labels=new String[packages.size()];boolean[] checked=new boolean[packages.size()];
            for(int i=0;i<labels.length;i++){String p=packages.get(i);labels[i]=names.get(p)+"\n"+p;checked[i]=category.members.contains(p);}
            new AlertDialog.Builder(a).setTitle(category.name+" — apps").setMultiChoiceItems(labels,checked,(d,n,on)->checked[n]=on)
                .setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
                    // Keep assignments to temporarily unavailable/uninstalled apps.
                    for(int i=0;i<packages.size();i++){if(checked[i])category.members.add(packages.get(i));else category.members.remove(packages.get(i));}save(a,data,changed);
                }).show();
        }catch(Throwable failure){XposedBridge.log("PICO Categories: app picker unavailable "+failure);Toast.makeText(a,"Open Library's app list first",Toast.LENGTH_LONG).show();}
    }
}

