package research.pico.librarycategories;
import java.util.*;

class CategoryModel {
    static final String PREFIX="pico_cat_";
    static final String HIDDEN_ID=PREFIX+"hidden";
    static final class Category {
        final String id;volatile String name;volatile boolean hidden;
        final Set<String> members=java.util.concurrent.ConcurrentHashMap.newKeySet();
        Category(String id,String name){this.id=id;this.name=name;}
        boolean custom(){return id.startsWith(PREFIX);}
    }
    final List<Category> items=new java.util.concurrent.CopyOnWriteArrayList<>();
    Category find(String id){for(Category c:items)if(c.id.equals(id))return c;return null;}
    Category ensure(String id,String name){Category c=find(id);if(c==null){c=new Category(id,name);items.add(c);}return c;}
    Category ensureHidden(){
        Category c=find(HIDDEN_ID);
        if(c==null){c=new Category(HIDDEN_ID,"Hidden");c.hidden=true;items.add(c);}
        return c;
    }
    Category create(String name){return ensure(PREFIX+UUID.randomUUID().toString().replace("-",""),name.trim());}
    void moveTo(Category c,int target){int from=items.indexOf(c);if(from>=0&&target>=0&&target<items.size()&&from!=target){items.remove(from);items.add(target,c);}}
    boolean isHidden(String pkg){Category c=find(HIDDEN_ID);return c!=null&&c.members.contains(pkg);}
    Set<String> getHiddenMembers(){Category c=find(HIDDEN_ID);return c!=null?c.members:Collections.emptySet();}
}

