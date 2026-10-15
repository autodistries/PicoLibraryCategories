package research.pico.librarycategories;

final class ReorderMath {
    static int target(float center,float[] midpoints,int source){
        if(source<0||source>=midpoints.length||!Float.isFinite(center))return source;
        int target=0;
        for(int i=0;i<midpoints.length;i++)if(i!=source&&center>midpoints[i])target++;
        return target;
    }
}
