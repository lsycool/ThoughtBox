package lsycool.com.himaterial;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Created by lsycool on 27/06/16.
 */
public class ItemAdapt extends BaseAdapter {

    private Context ctx;
    private ArrayList<ItemBean>points;
    private LayoutInflater inflater;

    public ItemAdapt(Context context, ArrayList<ItemBean>points){

        this.ctx = context;
        this.points = points;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return points.size();
    }

    @Override
    public Object getItem(int position) {
        return points.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void remove(int position){
        points.remove(position);
    }

    public void add(ItemBean bean){
        points.add(bean);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.search_list_item, null);
            holder = new ViewHolder();
            holder.index = (TextView) convertView.findViewById(R.id.search_item1);
            holder.tag = (TextView) convertView.findViewById(R.id.search_item2);
            holder.content = (TextView) convertView.findViewById(R.id.search_item3);
            holder.index.setAlpha(0.5f);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


//        if(position == 0){

//            holder.index.setText("编号");
//            holder.tag.setText("标签");
//            holder.content.setText("内容");//系统对联系人的简要方法
//        }else {

        ItemBean lbh = this.points.get(position);
        String l = lbh.getIndex() + "";
        String b = lbh.getTag() + "";
        String h = lbh.getContent() + "";
        holder.index.setText(l);
        holder.tag.setText(b);
        holder.content.setText(h);//系统对联系人的简要方法
//        }

        return convertView;
    }

    private static class ViewHolder {
        TextView index;
        TextView tag;
        TextView content;
    }

    public static class ItemBean {
        private String index;
        private String tag;
        private String content;

        ItemBean(){

            index =  "";
            tag = "";
            content = "";
        }

        ItemBean(String la, String lo, String at){

            index = la;
            tag = lo;
            content = at;
        }

        public void setValue(String la, String lo, String at){
            index = la;
            tag = lo;
            content = at;
        }

        public void setValue( String lo, String at){
            tag = lo;
            content = at;
        }

        public String getIndex(){
            return index;
        }

        public String getTag(){
            return tag;
        }

        public String getContent(){
            return content;
        }
    }

}
