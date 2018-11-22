package lsycool.com.himaterial;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.jfeinstein.jazzyviewpager.JazzyViewPager;
import com.jfeinstein.jazzyviewpager.OutlineContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static lsycool.com.himaterial.MainActivity.getDialogList;

/**
 * Created by lsycool on 20/06/17.
 */

public class MyPagerAdapter extends PagerAdapter {

    private LayoutInflater inflater;
    private JazzyViewPager viewPager;
    private String viewMode = "Add";
    private ArrayList<ItemAdapt.ItemBean> currentPoints;
    private Map<Integer, Integer> randPos2Positon = new HashMap<>();
    Context context;
    // 语音合成对象
    private SpeechSynthesizer mTts;

    private boolean isSpeaking = false;

    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            isSpeaking = true;

        }

        @Override
        public void onSpeakPaused() {

        }

        @Override
        public void onSpeakResumed() {

        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
        }

        @Override
        public void onCompleted(SpeechError error) {
            isSpeaking = false;
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    public MyPagerAdapter(Context context, JazzyViewPager myViewPager, ArrayList<ItemAdapt.ItemBean> currentPoints){
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.viewPager = myViewPager;
        this.currentPoints = currentPoints;
    }

    public MyPagerAdapter(Context context, JazzyViewPager myViewPager, ArrayList<ItemAdapt.ItemBean> currentPoints, String viewMode, SpeechSynthesizer mTts){
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.viewPager = myViewPager;
        this.currentPoints = currentPoints;
        this.viewMode = viewMode;
        this.mTts = mTts;
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {

        View convertView = inflater.inflate(R.layout.item_modify_popu, null);
        EditText tag = (EditText) convertView.findViewById(R.id.itemTag);
        final EditText content = (EditText) convertView.findViewById(R.id.itemContent);
        CheckBox isShow = (CheckBox) convertView.findViewById(R.id.isShowResult);
        ImageButton speeker = (ImageButton)convertView.findViewById(R.id.speeker);
        final Random random = new Random();
        final int pos = random.nextInt(currentPoints.size());
        randPos2Positon.put(position, pos);

        if (viewMode.equals("Practice")) {

            tag.setText(currentPoints.get(pos).getTag());
            String[] contentDbs = currentPoints.get(pos).getContent().split(";");
            if (1 == contentDbs.length) {
                content.setText(contentDbs[0].split("，")[0]);
            } else {
                content.setText(contentDbs[0]);
            }
            tag.setInputType(InputType.TYPE_NULL);
            content.setKeyListener(null);
            isShow.setVisibility(View.VISIBLE);
            container.addView(convertView);
            viewPager.setObjectForPosition(convertView, pos);

        } else {
            tag.setText(currentPoints.get(position).getTag());
            content.setText(getDialogList(currentPoints.get(position).getContent()));
            if (viewMode.equals("View")) {
                tag.setInputType(InputType.TYPE_NULL);
                content.setKeyListener(null);
            }
            isShow.setVisibility(View.GONE);
            container.addView(convertView);
            viewPager.setObjectForPosition(convertView, position);
        }

        isShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    if (viewMode.equals("Practice")) {
                        content.setText(currentPoints.get(pos).getContent().split(";")[0]);
                    }
                } else {
                    if (viewMode.equals("Practice")) {
                        content.setText(getDialogList(currentPoints.get(pos).getContent()));
                    }
                }
            }
        });

        speeker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSpeaking) {
                    mTts.startSpeaking(currentPoints.get(position).getContent(), mTtsListener);
                } else {
                    mTts.stopSpeaking();
                }
                isSpeaking = !isSpeaking;
            }
        });

        setContentToClipBoard(tag, content);

        return convertView;
    }
    @Override
    public void destroyItem(ViewGroup container, int position, Object obj) {
        if (!viewMode.equals("Practice")) {
            container.removeView(viewPager.findViewFromObject(position));
        } else {
            container.removeView(viewPager.findViewFromObject(randPos2Positon.get(position)));
        }
    }
    @Override
    public int getCount() {
        return currentPoints.size();
    }
    @Override
    public boolean isViewFromObject(View view, Object obj) {
        if (view instanceof OutlineContainer) {
            return ((OutlineContainer) view).getChildAt(0) == obj;
        } else {
            return view == obj;
        }
    }

    private void setContentToClipBoard(final EditText searchTag, final EditText searchContent) {

        searchTag.setOnLongClickListener(new View.OnLongClickListener(){
            public boolean onLongClick(View v)
            {
                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setText(searchTag.getText().toString());
                Toast.makeText(context, "复制成功", Toast.LENGTH_LONG).show();
                return true;
            }
        });

        searchContent.setOnLongClickListener(new View.OnLongClickListener(){
            public boolean onLongClick(View v)
            {
                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setText(searchContent.getText().toString());
                Toast.makeText(context, "复制成功", Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }
}
