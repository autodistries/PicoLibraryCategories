package research.pico.librarycategories;
public final class ModelTests {
    private static int checks;
    private static void check(boolean b){if(!b)throw new AssertionError("check "+(checks+1));checks++;}
    public static void main(String[] args){
        CategoryModel m=new CategoryModel();
        CategoryModel.Category all=m.ensure("all","All"),games=m.create("Games"),tools=m.create("Tools");
        check(m.ensure("all","All again")==all&&m.items.size()==3);
        check(!all.custom()&&games.custom()&&tools.custom());
        check(!games.id.equals(tools.id));
        games.members.add("io.github.wivrn");tools.members.add("io.github.wivrn");
        m.moveTo(tools,1);check(m.items.get(1)==tools&&m.items.get(2)==games);
        check(games.members.contains("io.github.wivrn")&&tools.members.contains("io.github.wivrn"));
        m.moveTo(all,-1);check(m.items.get(0)==all);
        m.moveTo(games,3);check(m.items.get(2)==games);
        m.moveTo(games,0);check(m.items.get(0)==games&&m.items.get(1)==all&&m.items.get(2)==tools);
        m.moveTo(games,2);check(m.items.get(0)==all&&m.items.get(1)==tools&&m.items.get(2)==games);
        check(ReorderMath.target(150,new float[]{50,150,250},1)==1);
        check(ReorderMath.target(40,new float[]{50,150,250},1)==0);
        check(ReorderMath.target(260,new float[]{50,150,250},1)==2);
        check(ReorderMath.target(260,new float[]{25,100,250},0)==2);
        check(ReorderMath.target(Float.NaN,new float[]{50,150,250},1)==1);
        String id=games.id;games.name="VR";check(m.find(id)==games&&games.members.contains("io.github.wivrn"));
        games.hidden=true;check(games.members.contains("io.github.wivrn"));
        m.items.remove(games);check(m.find(id)==null&&tools.members.contains("io.github.wivrn"));
        CategoryModel.Category same=m.create("Tools");check(!same.id.equals(tools.id));
        System.out.println(checks+" category identity/order/membership assertions passed");
    }
}
