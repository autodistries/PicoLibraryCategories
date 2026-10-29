package research.pico.librarycategories;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.util.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    private static final String APP="com.pvr.appmanager.",NAV="com.bytedance.osui.grouplist.OSUISideNavigation";
    static final String MANAGE="pico_manage_categories",MENU="pico_edit_categories",HIDE="pico_hide_app";
    private static volatile WeakReference<Activity> currentActivity;
    private Categories categories;
    private ClassLoader targetLoader;
    private final Map<Object,List<Object>> filters=new WeakHashMap<>();
    private static final class UnknownMenu {Object app;final java.util.concurrent.atomic.AtomicReference<PopupWindow> popup=new java.util.concurrent.atomic.AtomicReference<>();}
    private final ThreadLocal<UnknownMenu> unknown=new ThreadLocal<>();
    private static final class Navigation {
        final ViewGroup view;final Object section;final LinkedHashMap<String,View> stock=new LinkedHashMap<>();
        final List<View> visible=new ArrayList<>();int childIndex;
        Navigation(ViewGroup view,Object section){this.view=view;this.section=section;}
    }
    static Activity activity(Context c){while(c instanceof ContextWrapper){if(c instanceof Activity)return (Activity)c;Context next=((ContextWrapper)c).getBaseContext();if(next==c)break;c=next;}return null;}
    private Categories store(Context c){if(categories==null){categories=new Categories(c);syncFilters();}return categories;}
    static String pkg(Object app){return (String)XposedHelpers.callMethod(XposedHelpers.callMethod(app,"getApp"),"v");}
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load){
        if(load.packageName.equals("research.pico.librarycategories")){
            XposedHelpers.findAndHookMethod(MainActivity.class.getName(),load.classLoader,"isModuleActive",new XC_MethodReplacement(){@Override protected Object replaceHookedMethod(MethodHookParam p){return true;}});return;
        }
        if(!load.packageName.equals("com.pvr.appmanager")||!load.isFirstApplication)return;
        try{install(load.classLoader);}catch(Throwable failure){XposedBridge.log("PICO Categories: incompatible Library "+failure);}
    }
    private void install(ClassLoader cl){
        targetLoader=cl;
        Class<?> navType=XposedHelpers.findClass(NAV,cl),activityType=XposedHelpers.findClass(APP+"AllAppActivity",cl);
        Class<?> menu=XposedHelpers.findClass(APP+"utils.menu.MenuBean",cl);
        XposedHelpers.findClass(APP+"Filter$a",cl);
        Class<?> appData=XposedHelpers.findClass(APP+"entity.AppData",cl);
        XposedHelpers.findMethodExact(navType,"t",View.class);
        XposedHelpers.findMethodExact(APP+"Filter",cl,"getTargetFilter",String.class);
        XposedHelpers.findAndHookMethod(APP+"utils.menu.AppMenuTabHelper",cl,"addMenuTabs",Context.class,navType,XposedHelpers.findClass(APP+"utils.ToolTipsHelper",cl),new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                try{
                    ViewGroup nav=(ViewGroup)p.args[1];SparseArray<?> sections=(SparseArray<?>)XposedHelpers.getObjectField(nav,"d");
                    if(sections.size()!=1)throw new IllegalStateException("Unexpected sidebar section count");
                    Object section=sections.valueAt(0);SparseArray<?> items=(SparseArray<?>)XposedHelpers.getObjectField(section,"d");
                    Navigation n=new Navigation(nav,section);n.childIndex=nav.getChildCount();
                    Categories data=store(nav.getContext());
                    for(int i=0;i<items.size();i++){
                        View item=(View)items.valueAt(i);String tag=(String)item.getTag();
                        n.stock.put(tag,item);n.visible.add(item);n.childIndex=Math.min(n.childIndex,nav.indexOfChild(item));
                        TextView label=(TextView)XposedHelpers.callMethod(item,"getTextView");data.ensure(tag,label.getText().toString());
                    }
                    XposedHelpers.setAdditionalInstanceField(nav,"picoNavigation",n);rebuild(n,"all",false);
                    makeScrollable(nav);
                }catch(Throwable failure){XposedBridge.log("PICO Categories: stock sidebar retained "+failure);}
            }
        });
        XposedHelpers.findAndHookMethod(navType,"t",View.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                View item=(View)p.args[0];if(!MANAGE.equals(item.getTag()))return;
                p.setResult(null);Activity a=activity(item.getContext());if(a!=null)CategoryUi.manage(a,store(a),()->refresh(a));
            }
        });
        XposedHelpers.findAndHookMethod(activityType,"getCategoryIndex",boolean.class,String.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                Navigation n=navigation((Activity)p.thisObject);if(n==null)return;
                int index=0;for(View item:n.visible){if(Objects.equals(item.getTag(),p.args[1])){p.setResult(index);return;}index++;}p.setResult(0);
            }
        });
        XposedHelpers.findAndHookMethod(APP+"utils.Utils",cl,"getAppMenuName",String.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){if(categories!=null){Categories.Category c=categories.find((String)p.args[0]);if(c!=null)p.setResult(c.name);}}
        });
        XposedBridge.hookAllConstructors(XposedHelpers.findClass(APP+"Filter",cl),new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                synchronized(filters){filters.put(p.thisObject,new ArrayList<>());syncFilters();}
            }
        });
        XposedHelpers.findAndHookMethod(APP+"Filter",cl,"getTargetFilter",String.class,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                String tag=(String)p.args[0];
                if(tag!=null&&!tag.startsWith(Categories.PREFIX)&&p.getResult()!=null){
                    p.setResult(wrapStockFilter(p.getResult()));
                }
            }
        });
        XposedHelpers.findAndHookMethod(activityType,"onResume",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                Activity a=(Activity)p.thisObject;
                a.getWindow().getDecorView().post(()->setupHeaderManageButton(a));
                currentActivity=new WeakReference<>(a);
                if(a.getIntent().getBooleanExtra(MANAGE,false)){a.getIntent().removeExtra(MANAGE);a.getWindow().getDecorView().post(()->CategoryUi.manage(a,store(a),()->refresh(a)));}
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class,"onNewIntent",Intent.class,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                if(!activityType.isInstance(p.thisObject))return;
                Intent intent=(Intent)p.args[0];if(!intent.getBooleanExtra(MANAGE,false))return;
                intent.removeExtra(MANAGE);Activity a=(Activity)p.thisObject;a.setIntent(intent);
                a.getWindow().getDecorView().post(()->CategoryUi.manage(a,store(a),()->refresh(a)));
            }
        });
        Class<?> helper=XposedHelpers.findClass(APP+"utils.menu.MoreMenusHelper",cl);
        XposedHelpers.findAndHookMethod(helper,"getAllMenus",Object.class,int.class,XposedHelpers.findClass("d.b0.b.p",cl),new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                if(p.hasThrowable()||!appData.isInstance(p.args[0])||p.getResult()==null)return;
                List<Object> rows=new ArrayList<>((List<?>)p.getResult());
                rows.add(XposedHelpers.newInstance(menu,MENU,android.R.string.untitled,android.graphics.Color.WHITE,p.args[0],p.args[1]));
                rows.add(XposedHelpers.newInstance(menu,HIDE,android.R.string.untitled,android.graphics.Color.WHITE,p.args[0],p.args[1]));
                p.setResult(rows);
            }
        });
        XposedHelpers.findAndHookMethod(helper,"generateView",Context.class,menu,View.OnClickListener.class,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                if(!(p.getResult() instanceof TextView))return;
                Object menuBean=p.args[1];String key=(String)XposedHelpers.callMethod(menuBean,"getKey");
                if(!MENU.equals(key)&&!HIDE.equals(key))return;
                TextView row=(TextView)p.getResult();Object helperObject=p.thisObject;
                Object app=XposedHelpers.callMethod(menuBean,"getAppData");
                Context c=(Context)p.args[0];
                if(MENU.equals(key)){
                    row.setText("Categories…");
                    row.setOnClickListener(v->{
                        Object ref=XposedHelpers.getAdditionalInstanceField(helperObject,"picoPopup");
                        if(ref instanceof WeakReference){Object popup=((WeakReference<?>)ref).get();if(popup instanceof PopupWindow)((PopupWindow)popup).dismiss();}
                        Activity a=activity(v.getContext());if(a!=null)CategoryUi.assign(a,store(a),pkg(app),(String)XposedHelpers.callMethod(app,"getName"),()->refresh(a));
                    });
                }else if(HIDE.equals(key)){
                    Categories data=store(c);String appPkg=pkg(app);boolean isHidden=data.isHidden(appPkg);
                    row.setText(isHidden?"Unhide":"Hide");
                    row.setOnClickListener(v->{
                        Object ref=XposedHelpers.getAdditionalInstanceField(helperObject,"picoPopup");
                        if(ref instanceof WeakReference){Object popup=((WeakReference<?>)ref).get();if(popup instanceof PopupWindow)((PopupWindow)popup).dismiss();}
                        Activity a=activity(v.getContext());
                        if(a!=null){data.setAppHidden(appPkg,!isHidden);refresh(a);}
                    });
                }
            }
        });
        XposedHelpers.findAndHookMethod(helper,"showMoreMenu",View.class,View.class,int.class,List.class,View.OnClickListener.class,int.class,XposedHelpers.findClass("d.b0.b.l",cl),new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){if(p.getResult() instanceof PopupWindow)XposedHelpers.setAdditionalInstanceField(p.thisObject,"picoPopup",new WeakReference<>((PopupWindow)p.getResult()));}
        });
        // Unknown uses a separate popup builder, not MoreMenusHelper.
        XposedHelpers.findAndHookMethod(APP+"z.j0",cl,"s",appData,XposedHelpers.findClass(APP+"z.j0",cl),int.class,boolean.class,XposedHelpers.findClass("d.b0.c.q",cl),View.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){UnknownMenu u=new UnknownMenu();u.app=p.args[0];unknown.set(u);}
            @Override protected void afterHookedMethod(MethodHookParam p){unknown.remove();}
        });
        XposedHelpers.findAndHookMethod(APP+"view.g",cl,"a",View.class,View.class,int.class,int.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam p){
                UnknownMenu u=unknown.get();if(u==null)return;View root=(View)p.args[1];Context c=root.getContext();
                int id=c.getResources().getIdentifier("filter_layout","id","com.pvr.appmanager");LinearLayout list=root.findViewById(id);if(list==null)return;
                Class<?> tvClass=XposedHelpers.findClass("com.bytedance.osui.common.OSUITextView",cl);
                int padding=c.getResources().getDimensionPixelSize(c.getResources().getIdentifier("margin_8","dimen","com.pvr.appmanager"));
                int size=c.getResources().getIdentifier("common_textsize_16","dimen","com.pvr.appmanager");
                int background=c.getResources().getIdentifier("app_sort_all_bg","drawable","com.pvr.appmanager");

                TextView row=(TextView)XposedHelpers.newInstance(tvClass,c);
                row.setText("Categories…");row.setTextColor(android.graphics.Color.WHITE);row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(padding,0,0,0);row.setTextSize(0,c.getResources().getDimension(size));row.setBackgroundResource(background);
                row.setOnClickListener(v->{PopupWindow popup=u.popup.get();if(popup!=null)popup.dismiss();Activity a=activity(c);if(a!=null)CategoryUi.assign(a,store(a),pkg(u.app),(String)XposedHelpers.callMethod(u.app,"getName"),()->refresh(a));});
                View existing=list.getChildCount()>0?list.getChildAt(0):null;
                LinearLayout.LayoutParams params=existing==null?new LinearLayout.LayoutParams(-1,64):new LinearLayout.LayoutParams(existing.getLayoutParams());
                list.addView(row,params);

                TextView hideRow=(TextView)XposedHelpers.newInstance(tvClass,c);
                Categories data=store(c);String appPkg=pkg(u.app);boolean isHidden=data.isHidden(appPkg);
                hideRow.setText(isHidden?"Unhide":"Hide");hideRow.setTextColor(android.graphics.Color.WHITE);hideRow.setGravity(Gravity.CENTER_VERTICAL);
                hideRow.setPadding(padding,0,0,0);hideRow.setTextSize(0,c.getResources().getDimension(size));hideRow.setBackgroundResource(background);
                hideRow.setOnClickListener(v->{
                    PopupWindow popup=u.popup.get();if(popup!=null)popup.dismiss();
                    Activity a=activity(c);
                    if(a!=null){data.setAppHidden(appPkg,!isHidden);refresh(a);}
                });
                list.addView(hideRow,new LinearLayout.LayoutParams(params));
            }
            @Override protected void afterHookedMethod(MethodHookParam p){UnknownMenu u=unknown.get();if(u!=null&&p.getResult() instanceof PopupWindow)u.popup.set((PopupWindow)p.getResult());}
        });
        XposedBridge.log("PICO Categories: Library hooks installed");
    }
    private void setupHeaderManageButton(Activity a){
        if(a==null||a.isFinishing())return;
        View decor=a.getWindow().getDecorView();
        TextView libraryTv=findLibraryTitleView(decor);
        if(libraryTv==null){
            decor.postDelayed(()->setupHeaderManageButton(a),200);
            return;
        }
        if(!(libraryTv.getParent() instanceof ViewGroup))return;
        ViewGroup parent=(ViewGroup)libraryTv.getParent();

        if(decor.findViewWithTag("pico_manage_header_btn")!=null)return;

        ImageView btn=new ImageView(a);
        btn.setTag("pico_manage_header_btn");
        btn.setFocusable(true);
        btn.setClickable(true);
        btn.setImageDrawable(a.getDrawable(android.R.drawable.ic_menu_manage));
        btn.setColorFilter(0xFFDCDCDC);
        btn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        int dp32=(int)(32*a.getResources().getDisplayMetrics().density);
        int dp8=(int)(8*a.getResources().getDisplayMetrics().density);
        int dp12=(int)(12*a.getResources().getDisplayMetrics().density);

        btn.setPadding(dp8/2,dp8/2,dp8/2,dp8/2);

        btn.setOnClickListener(v->{
            Activity act=activity(v.getContext());
            if(act!=null){
                CategoryUi.manage(act,store(act),()->refresh(act));
            }
        });

        if(parent instanceof LinearLayout&&((LinearLayout)parent).getOrientation()==LinearLayout.VERTICAL){
            int index=parent.indexOfChild(libraryTv);
            ViewGroup.LayoutParams origLp=libraryTv.getLayoutParams();
            parent.removeView(libraryTv);

            LinearLayout rowContainer=new LinearLayout(a);
            rowContainer.setOrientation(LinearLayout.HORIZONTAL);
            rowContainer.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout.LayoutParams btnLp=new LinearLayout.LayoutParams(dp32,dp32);
            btnLp.leftMargin=dp12;

            rowContainer.addView(libraryTv,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
            rowContainer.addView(btn,btnLp);

            parent.addView(rowContainer,index,origLp);
        }else{
            int index=parent.indexOfChild(libraryTv);
            LinearLayout.LayoutParams btnLp=new LinearLayout.LayoutParams(dp32,dp32);
            btnLp.leftMargin=dp12;
            btnLp.gravity=Gravity.CENTER_VERTICAL;
            parent.addView(btn,index+1,btnLp);
        }
    }
    private static TextView findLibraryTitleView(View root){
        if(root==null||root.getVisibility()!=View.VISIBLE)return null;
        if(root instanceof TextView){
            TextView tv=(TextView)root;
            if("Library".equalsIgnoreCase(tv.getText().toString().trim())){
                return tv;
            }
        }
        if(root instanceof ViewGroup){
            ViewGroup g=(ViewGroup)root;
            for(int i=0;i<g.getChildCount();i++){
                TextView tv=findLibraryTitleView(g.getChildAt(i));
                if(tv!=null)return tv;
            }
        }
        return null;
    }
    private void makeScrollable(ViewGroup nav){
        if(nav.getParent() instanceof ScrollView||!(nav.getParent() instanceof ViewGroup))return;
        ViewGroup parent=(ViewGroup)nav.getParent();int index=parent.indexOfChild(nav);
        ViewGroup.LayoutParams original=nav.getLayoutParams();
        ScrollView scroll=new ScrollView(nav.getContext());scroll.setFillViewport(true);
        parent.removeView(nav);scroll.addView(nav,new ScrollView.LayoutParams(-1,-2));parent.addView(scroll,index,original);
    }
    private Object customFilter(Categories.Category c){
        Class<?> type=XposedHelpers.findClass(APP+"Filter$a",targetLoader);
        return Proxy.newProxyInstance(targetLoader,new Class<?>[]{type},(proxy,method,args)->{
            switch(method.getName()){
                case "a":return c.id;
                case "b":{
                    @SuppressWarnings("unchecked")
                    List<Object> input=(List<Object>)args[0];
                    Activity a=currentActivity!=null?currentActivity.get():null;
                    if(a!=null){
                        try{
                            @SuppressWarnings("unchecked")
                            List<Object> master=(List<Object>)XposedHelpers.callMethod(a,"getMAllApp");
                            if(master!=null&&!master.isEmpty())input=master;
                        }catch(Throwable ignored){}
                    }
                    List<Object> result=new ArrayList<>();
                    if(input!=null)for(Object app:input)if(c.members.contains(pkg(app)))result.add(app);
                    return result;
                }
                case "c":return c.members.contains(pkg(args[0]));
                case "toString":return "PicoCategory("+c.id+")";
                case "hashCode":return System.identityHashCode(proxy);
                case "equals":return proxy==args[0];
                default:throw new UnsupportedOperationException(method.getName());
            }
        });
    }
    private Object wrapStockFilter(Object original){
        if(original==null||Proxy.isProxyClass(original.getClass()))return original;
        Class<?> type=XposedHelpers.findClass(APP+"Filter$a",targetLoader);
        return Proxy.newProxyInstance(targetLoader,new Class<?>[]{type},(proxy,method,args)->{
            switch(method.getName()){
                case "a":return XposedHelpers.callMethod(original,"a");
                case "b":{
                    @SuppressWarnings("unchecked")
                    List<Object> list=(List<Object>)XposedHelpers.callMethod(original,"b",args[0]);
                    if(categories==null||list==null)return list;
                    Activity a=currentActivity!=null?currentActivity.get():null;
                    if(a!=null){
                        try{
                            String selected=(String)XposedHelpers.getObjectField(a,"mLastTab");
                            if(selected!=null&&selected.startsWith(Categories.PREFIX))return list;
                        }catch(Throwable ignored){}
                    }
                    Set<String> hiddenApps=categories.getHiddenMembers();
                    if(hiddenApps.isEmpty())return list;
                    List<Object> filtered=new ArrayList<>();
                    for(Object app:list)if(!hiddenApps.contains(pkg(app)))filtered.add(app);
                    return filtered;
                }
                case "c":{
                    boolean res=(Boolean)XposedHelpers.callMethod(original,"c",args[0]);
                    if(!res||categories==null)return res;
                    Activity a=currentActivity!=null?currentActivity.get():null;
                    if(a!=null){
                        try{
                            String selected=(String)XposedHelpers.getObjectField(a,"mLastTab");
                            if(selected!=null&&selected.startsWith(Categories.PREFIX))return res;
                        }catch(Throwable ignored){}
                    }
                    return !categories.getHiddenMembers().contains(pkg(args[0]));
                }
                case "toString":return "PicoStockFilterWrapper("+original+")";
                case "hashCode":return System.identityHashCode(proxy);
                case "equals":return proxy==args[0];
                default:return method.invoke(original,args);
            }
        });
    }
    @SuppressWarnings("unchecked") private void syncFilters(){
        if(categories==null)return;
        synchronized(filters){for(Map.Entry<Object,List<Object>> entry:filters.entrySet()){
            List<Object> target=(List<Object>)XposedHelpers.callMethod(entry.getKey(),"getMFilterList");
            for(int i=0;i<target.size();i++){
                Object f=target.get(i);
                String tag=(String)XposedHelpers.callMethod(f,"a");
                if(tag!=null&&!tag.startsWith(Categories.PREFIX)&&!Proxy.isProxyClass(f.getClass())){
                    target.set(i,wrapStockFilter(f));
                }
            }
            target.removeAll(entry.getValue());entry.getValue().clear();
            for(Categories.Category c:categories.items)if(c.custom()){Object f=customFilter(c);target.add(f);entry.getValue().add(f);}
        }}
    }
    private void refreshMembership(Activity a){
        try{
            String selected=(String)XposedHelpers.getObjectField(a,"mLastTab");
            if(selected==null)return;
            Object filter=XposedHelpers.callMethod(a,"getMFilter"),adapter=XposedHelpers.getObjectField(a,"mAdapter");
            Object all=XposedHelpers.callMethod(a,"getMAllApp");
            String keyword=(String)XposedHelpers.getObjectField(a,"mSearchKeyword");
            Object nav=XposedHelpers.callMethod(a,"getMAppMenuTab");
            Button header=(Button)XposedHelpers.callMethod(nav,"getHeaderButton");
            XposedHelpers.callMethod(filter,"filterData",selected,adapter,all,header.getVisibility()==View.VISIBLE,keyword);
            XposedHelpers.callMethod(filter,"sortData",XposedHelpers.callMethod(filter,"getSortType"),adapter);
        }catch(Throwable failure){XposedBridge.log("PICO Categories: membership refresh failed "+failure);}
    }
    private Navigation navigation(Activity a){
        Object nav=XposedHelpers.callMethod(a,"getMAppMenuTab");return (Navigation)XposedHelpers.getAdditionalInstanceField(nav,"picoNavigation");
    }
    private void refresh(Activity a){
        try{
            a.getWindow().getDecorView().post(()->setupHeaderManageButton(a));
            Navigation current=navigation(a);
            if(current!=null){
                List<String> expected=new ArrayList<>();for(Categories.Category c:categories.items)if(!c.hidden&&(c.custom()||current.stock.containsKey(c.id)))expected.add(c.id+"/"+c.name);
                List<String> actual=new ArrayList<>();for(View v:current.visible)if(!MANAGE.equals(v.getTag()))actual.add(v.getTag()+"/"+((TextView)XposedHelpers.callMethod(v,"getTextView")).getText());
                if(actual.equals(expected)){refreshMembership(a);return;}
            }
            Navigation n=navigation(a);if(n==null)return;String selected="all";for(View v:n.visible)if(v.isActivated())selected=String.valueOf(v.getTag());rebuild(n,selected,true);}
        catch(Throwable failure){XposedBridge.log("PICO Categories: refresh failed "+failure);Toast.makeText(a,"Close and reopen Library to refresh categories",Toast.LENGTH_LONG).show();}
    }
    @SuppressWarnings("unchecked") private void rebuild(Navigation n,String selected,boolean select){
        syncFilters();
        List<View> next=new ArrayList<>();Context context=n.view.getContext();
        for(Categories.Category c:store(context).items){
            if(c.hidden)continue;View row=n.stock.get(c.id);
            if(row==null){if(!c.custom())continue;row=(View)XposedHelpers.callMethod(n.view,"n",context.getDrawable(android.R.drawable.ic_menu_agenda),c.name);row.setTag(c.id);}
            ((TextView)XposedHelpers.callMethod(row,"getTextView")).setText(c.name);next.add(row);
        }
        if(next.isEmpty()){View all=n.stock.get("all");if(all!=null)next.add(all);}
        // Keep header/search children, but update BOTH the visual and selection-index model.
        for(View old:n.visible)n.view.removeView(old);
        SparseArray<Object> items=(SparseArray<Object>)XposedHelpers.getObjectField(n.section,"d");items.clear();
        View.OnClickListener click=(View.OnClickListener)XposedHelpers.callMethod(n.view,"getItemClickListener");
        int index=0,selectedIndex=0;
        for(View row:next){row.setOnClickListener(click);items.put(index,row);n.view.addView(row,n.childIndex+index);if(Objects.equals(row.getTag(),selected))selectedIndex=index;index++;}
        n.visible.clear();n.visible.addAll(next);
        XposedHelpers.callMethod(n.view,"m");XposedHelpers.callMethod(n.view,"setDefActivatedIndex",selectedIndex);
        if(select)XposedHelpers.callMethod(n.view,"s");
    }
}
