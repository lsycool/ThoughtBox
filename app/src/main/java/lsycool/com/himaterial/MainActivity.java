package lsycool.com.himaterial;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.jfeinstein.jazzyviewpager.JazzyViewPager;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lsycool.com.mailsender.SimpleMailSender;
import lsycool.com.update.UpdateManager;


public class MainActivity extends AppCompatActivity implements ObservableScrollViewCallbacks {

    private DBManager dbHelper;

    private List<String> tablesDB;

    private String currentTableName = "";

    private ItemAdapt adapter = null;

    final Context context = this;

    private boolean[] mSelectedSetting = {true, false, false};

    private final ArrayList<ItemAdapt.ItemBean> currentPoints = new ArrayList<>();

    private SQLiteDatabase db;

    private Drawer result = null;

    // 语音合成对象
    private SpeechSynthesizer mTts;

    // 默认云端发音人
    public static String voicerCloud="xiaoyan";
    // 默认本地发音人
    public static String voicerLocal="xiaoyan";

    private SharedPreferences mSharedPreferences;

    // 云端/本地选择按钮
    private RadioGroup mRadioGroup;

    // 男声/女声选择按钮
    private RadioGroup mRadioGroupMan;

    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_LOCAL;

    private String speed = "60", pitch = "50", preference = "50";

    private boolean isAutoPlay = false;

    AlertDialog dia;

    private int autoPlayIndex = 0;

    JazzyViewPager convertView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBManager(this);
        dbHelper.openDatabase();
        db = dbHelper.getDatabase();

        tablesDB = getAllTables();
        currentTableName = tablesDB.get(0);
        if (currentTableName.equals("sqlite_sequence")) {
            currentTableName = tablesDB.get(1);
        }

        requestPermission(this); //获取权限

        // 将“12345678”替换成您申请的APPID，申请地址：http://www.xfyun.cn
        // 请勿在“=”与appid之间添加任何空字符或者转义符
        SpeechUtility.createUtility(context, SpeechConstant.APPID +"=5a60a22f");

        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
        mSharedPreferences = getSharedPreferences("lsycool.com.himaterial", Activity.MODE_PRIVATE);
        setParam(speed, pitch, preference);


        initDrawBuilder(savedInstanceState);

        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                result.getDrawerLayout(), /* DrawerLayout object */
                R.string.action_add, /* "open drawer" description for accessibility */
                R.string.action_delete); /* "close drawer" description for accessibility */

        result.setActionBarDrawerToggle(mDrawerToggle);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.menu);

        showDBContents(db);
    }

    private void initDrawBuilder(Bundle savedInstanceState) {
        result = new DrawerBuilder()
                .withActivity(this)
                .withSavedInstance(savedInstanceState)
                .withDisplayBelowStatusBar(false)
                .withTranslucentStatusBar(false)
                .withActionBarDrawerToggle(true)
                .withActionBarDrawerToggleAnimated(true)
                .withDrawerLayout(R.layout.material_drawer_fits_not)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("对话").withIcon(R.mipmap.left_arrow),
                        new SecondaryDrawerItem().withName("活动"),
                        new SecondaryDrawerItem().withName("歇后语"),
                        new SecondaryDrawerItem().withName("搭讪"),
                        new SecondaryDrawerItem().withName("活动")
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem instanceof Nameable) {
                            if(tablesDB.size() > position + 2) {
                                if (tablesDB.get(0).equals("sqlite_sequence")) {
                                    currentTableName = tablesDB.get(position + 1);
                                } else {
                                    currentTableName = tablesDB.get(position);
                                }
                            }
                            showDBContents(db);
                            Toast.makeText(MainActivity.this, ((Nameable) drawerItem).getName().getText(MainActivity.this) + currentTableName, Toast.LENGTH_SHORT).show();
                        }

                        return false;
                    }
                })
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
                    }

                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {

                    }
                })
                .build();
    }

    private void InitViewPage(JazzyViewPager myViewPager) {
        myViewPager.setTransitionEffect(JazzyViewPager.TransitionEffect.Standard);
        myViewPager.setPageMargin(10);//两个页面之间的间距
        myViewPager.setFadeEnabled(true);//有淡入淡出效果
        myViewPager.setOutlineEnabled(false);//无边框
//        myViewPager.setOutlineColor(0xff0000ff);//边框颜色
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.closeDatabase();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        boolean[] isChecked = {mSelectedSetting[0], mSelectedSetting[1], mSelectedSetting[2]};
        UpdateManager manager = new UpdateManager(MainActivity.this);

        switch (id) {
            case android.R.id.home:
                if (result != null) {

                    if (result.isDrawerOpen()) {
                        //设置成汉堡箭头
                        result.closeDrawer();
                        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
                        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

                    } else {
                        //设置成back箭头
                        result.openDrawer();
                        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);

                    }
                }
                return true;

            case R.id.action_settings:

                new AlertDialog.Builder(this)
                        .setTitle("设置")
                        .setMultiChoiceItems(new String[]{"开启编辑", "开启批量添加", "自动播放"}, isChecked, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                mSelectedSetting[which] = isChecked;
                            }
                        })
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setNegativeButton("否", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
                break;
            case R.id.action_add:
                LinearLayout ItemSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.item_modify_popu, null);

                final EditText searchTag = (EditText) ItemSettings.findViewById(R.id.itemTag);
                final EditText searchContent = (EditText) ItemSettings.findViewById(R.id.itemContent);
                final Button buttonAdd = (Button) ItemSettings.findViewById(R.id.batchAdd);

                searchTag.setOnLongClickListener(new View.OnLongClickListener(){
                    public boolean onLongClick(View v)
                    {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        searchTag.setText(cm.getText());
                        return true;
                    }
                });

                searchContent.setOnLongClickListener(new View.OnLongClickListener(){
                    public boolean onLongClick(View v)
                    {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        searchContent.setText(cm.getText());
                        return true;
                    }
                });

                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("")
                        .setView(ItemSettings);

                if (!mSelectedSetting[1]) {

                    builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {

                            String t = searchTag.getText().toString();
                            String s = searchContent.getText().toString();
                            ItemAdapt.ItemBean beanTemp = new ItemAdapt.ItemBean(getMaxID(currentTableName, db) + 2 + "", t, s);
                            addDBContent(t, s, currentTableName, db);

                            currentPoints.add(beanTemp);
                            adapter.notifyDataSetChanged();

                            //跳转到该item
                            ObservableListView pointList = (ObservableListView) findViewById(R.id.point_list);
                            pointList.setSelection(currentPoints.size());
//                        new AlertDialog.Builder(context)
//                                .setTitle("确认")
//                                .setMessage("确定添加？")
//                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//
//                                    }
//                                })
//                                .setNegativeButton("否", new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//
//                                    }
//                                })
//                                .show();
                        }
                    });
                }
                else {
                    buttonAdd.setVisibility(View.VISIBLE);
                    buttonAdd.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String t = searchTag.getText().toString();
                            String s = searchContent.getText().toString();
                            ItemAdapt.ItemBean beanTemp = new ItemAdapt.ItemBean(getMaxID(currentTableName, db) + 2 + "", t, s);
                            addDBContent(t, s, currentTableName, db);

                            currentPoints.add(beanTemp);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(MainActivity.this, "添加成功", Toast.LENGTH_LONG).show();

                            searchContent.setText("");
                            searchTag.setText("");
                        }
                    });
                }
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //跳转到该item
                        ObservableListView pointList = (ObservableListView) findViewById(R.id.point_list);
                        pointList.setSelection(currentPoints.size());
                        dialog.cancel();
                    }

                });

                final AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();

                WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
                layoutParams.width = (int) (WindowManager.LayoutParams.WRAP_CONTENT);
                layoutParams.height = (int) (1450);
                dialog.getWindow().setAttributes(layoutParams);

                buttonAdd.setY(dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getY());
                buttonAdd.setX(dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getX());

                break;
            case R.id.action_practice:
                if (mSelectedSetting[2]) { //是否开启自动播放
                    isAutoPlay = true;
                    dia = showSettingDialog(autoPlayIndex, "View");
                    AutoPlayHandler.post(AutoPlayrunnable);
                } else {
                    isAutoPlay = false;
                    dia = showSettingDialog(autoPlayIndex, "Practice");
                    AutoPlayHandler.removeCallbacks(AutoPlayrunnable);
                }
                break;
            case R.id.action_upgrade:
                // 检查软件更新
                manager.checkUpdate();
                break;
            case R.id.action_upload:

                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    // 获得存储卡的路径
                    String path = this.getFilesDir().getParent();// "/data/data/com.lsy.namespace";
                    String mSavePath = path + "/storys.db3";
                    manager.uploadFiles(mSavePath, "http://www.lsycool.cn/personal/upload.php");
                }
                break;
            case R.id.action_sendmail:

                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    // 获得存储卡的路径
                    String path = this.getFilesDir().getParent();// "/data/data/com.lsy.namespace";
                    String mSavePath = path + "/storys.db3";
                    MailSender sender = new MailSender(mSavePath);
                    sender.start();
                }
                break;
            case R.id.action_speeking:
                LinearLayout SpeekingSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.speekiing_setting, null);

                SeekBar seekBarSpeed = (SeekBar) SpeekingSettings.findViewById(R.id.seekBar_speed);
                SeekBar seekBarPitch = (SeekBar) SpeekingSettings.findViewById(R.id.seekBar_pitch);
                SeekBar seekBarPreference = (SeekBar) SpeekingSettings.findViewById(R.id.seekBar_preference);

                setSeekBarParam(seekBarSpeed, speed);
                setSeekBarParam(seekBarPitch, pitch);
                setSeekBarParam(seekBarPreference, preference);

                mRadioGroup=((RadioGroup) SpeekingSettings.findViewById(R.id.tts_rediogroup));
                mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (checkedId) {
                            case R.id.tts_radioCloud:
                                mEngineType = SpeechConstant.TYPE_CLOUD;
                                break;
                            case R.id.tts_radioLocal:
                                mEngineType =  SpeechConstant.TYPE_LOCAL;
                                break;
                            default:
                                break;
                        }
                    }
                } );

                mRadioGroupMan = ((RadioGroup) SpeekingSettings.findViewById(R.id.tts_rediogroup2));
                mRadioGroupMan.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (checkedId) {
                            case R.id.tts_man:
                                voicerLocal = "xiaofeng";
                                voicerCloud = "xiaofeng";
                                break;
                            case R.id.tts_women:
                                voicerLocal = "xiaoyan";
                                voicerCloud = "xiaoyan";
                                break;
                            default:
                                break;
                        }
                    }
                } );

                AlertDialog.Builder builder1 = new AlertDialog.Builder(this)
                        .setTitle("")
                        .setView(SpeekingSettings);

                builder1.setPositiveButton("确认", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        setParam(speed, pitch, preference);
                    }
                });

                final AlertDialog dialog1 = builder1.create();
                dialog1.setCanceledOnTouchOutside(false);
                dialog1.show();

                break;
        }

        return super.onOptionsItemSelected(item);
    }

    Set<Integer> hasPlayed = new HashSet<>();
    final Random random = new Random(new Date().getTime());

    Handler AutoPlayHandler=new Handler();
    Runnable AutoPlayrunnable = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //要做的事情
            autoPlayIndex = random.nextInt(currentPoints.size());
            while (hasPlayed.contains(autoPlayIndex)) {
                autoPlayIndex = random.nextInt(currentPoints.size());
            }
            hasPlayed.add(autoPlayIndex);
            mTts.startSpeaking(currentPoints.get(autoPlayIndex).getContent(), mTtsListener);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        return super.onKeyDown(keyCode, event);
    }

    private void setSeekBarParam(SeekBar seekBarSpeed, final String result ) {
        seekBarSpeed.setMax(100);
        seekBarSpeed.setProgress(Integer.valueOf(result));
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
                if (result.equals(speed)) {
                    speed = String.valueOf(progress);
                } else if (result.equals(pitch)) {
                    pitch = String.valueOf(progress);
                } else {
                    preference = String.valueOf(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
            }
        });
    }

    private void showDBContents(final SQLiteDatabase db) {

        final EditText searchContent = (EditText) findViewById(R.id.searchContent);
        Button buttonDelete = (Button) findViewById(R.id.buttonDelete);

        final ObservableListView pointList = (ObservableListView) findViewById(R.id.point_list);
        pointList.setScrollViewCallbacks(this);

        adapter = new ItemAdapt(this, currentPoints);
        pointList.setAdapter(adapter);

        getAllData(currentPoints, currentTableName, db);
        adapter.notifyDataSetChanged();

        pointList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showSettingDialog(position, "View");
            }
        });

        pointList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int flag[] = showPopupMenu(position, db, currentTableName);
                if (2 == flag[0]) {
                    currentPoints.remove(position);
                    adapter.notifyDataSetChanged();
                }
                return true;
            }

        });

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchContent.setText("");
                searchContent.requestFocus();
                currentPoints.clear();
                getAllData(currentPoints, currentTableName, db);
                adapter.notifyDataSetChanged();
            }
        });

        searchContent.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
                searchDBContent(s, currentPoints, adapter, db, currentTableName);
            }


        });

    }

    @NonNull
    private void getAllData(ArrayList<ItemAdapt.ItemBean> points, String tableName, SQLiteDatabase db) {

        Cursor c = db.rawQuery("SELECT * FROM " + tableName, null);

        if (!points.isEmpty()) {
            points.clear();
        }

        //获取当天所有数据的Tag
        while (c.moveToNext()) {

            String index = c.getInt(c.getColumnIndex("ID"))+"";
//            if (index.length() > 26) {
//                index = index.substring(0, 26);
//            }
            String tag = c.getString(c.getColumnIndex("TAG")).trim();
//            if (tag.length() > 26) {
//                tag = tag.substring(0, 26).trim() + "...";
//            }
            String content = c.getString(c.getColumnIndex("CONTENT")).trim();
//            if (content.length() > 26) {
//                content = content.substring(0, 26).trim() + "...";
//            }
            points.add(new ItemAdapt.ItemBean(index, tag, content));
        }
        c.close();
    }

    private void searchDBContent(CharSequence s, ArrayList<ItemAdapt.ItemBean> points, ItemAdapt adapter, SQLiteDatabase db, String tableName) {

        if (!points.isEmpty()) {
            points.clear();
        }

        Cursor cs;
        if (isNumeric(s.toString())) {
            cs = db.rawQuery("SELECT * FROM " + tableName + " WHERE ID like ?", new String[]{"%" + s.toString().trim() + "%"});
        } else {
            cs = db.rawQuery("SELECT * FROM " + tableName + " WHERE TAG like ? or CONTENT like ?", new String[]{"%" + s.toString().trim() + "%", "%" + s.toString().trim() + "%"});
        }

//        if (cs.getCount() <= 0) {
//            cs.close();
//            cs = db.rawQuery("SELECT * FROM " + tableName + " WHERE CONTENT like ?", new String[]{"%" + s.toString() + "%"});
//        }

        while (cs.moveToNext()) {

            String index = cs.getString(cs.getColumnIndex("ID"));
//            if (index.length() > 26) {
//                index = index.substring(0, 26);
//            }
            String tag = cs.getString(cs.getColumnIndex("TAG")).trim();
//            if (tag.length() > 26) {
//                tag = tag.substring(0, 26).trim() + "...";
//            }
            String content = cs.getString(cs.getColumnIndex("CONTENT")).trim();
//            if (content.length() > 26) {
//                content = content.substring(0, 26).trim() + "...";
//            }
            points.add(new ItemAdapt.ItemBean(index, tag, content));
        }
        cs.close();
        adapter.notifyDataSetChanged();
    }

    private AlertDialog showSettingDialog(final int positionId, String viewMode) {

        convertView = (JazzyViewPager) getLayoutInflater().inflate(R.layout.item_show_viewpage, null);
        InitViewPage(convertView);
        convertView.setAdapter(new MyPagerAdapter(context, convertView, currentPoints, viewMode, mTts));
        convertView.setCurrentItem(positionId);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("")
                .setView(convertView);

        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode,
                                 KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK
                        && event.getRepeatCount() == 0) {
                    isAutoPlay = false;
                    mTts.stopSpeaking();
                    AutoPlayHandler.removeCallbacks(AutoPlayrunnable);
                }
                return false;
            }
        });

        builder.setOnCancelListener (new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                isAutoPlay = false;
                mTts.stopSpeaking();
                AutoPlayHandler.removeCallbacks(AutoPlayrunnable);
            }
        });


        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
        layoutParams.width = (int) (WindowManager.LayoutParams.WRAP_CONTENT);
        layoutParams.height = (int) (1450);
        dialog.getWindow().setAttributes(layoutParams);

        return dialog;
    }

    public static String getDialogList(String dialog) {
        String result = "";
        String[] results = dialog.split(";");
        for (String s : results) {
            result = result + s.trim() + "\n";
        }
        return result;
    }

    private int updateDBContent(String index, String tag, String content, String tableName, SQLiteDatabase db) {

        ContentValues cv = new ContentValues();
        cv.put("TAG", tag.trim());
        cv.put("CONTENT", content.trim());
        return db.update(tableName, cv, "ID = ?", new String[]{index});
    }

    private long addDBContent(String tag, String content, String tableName, SQLiteDatabase db) {

        ContentValues cv = new ContentValues();
        cv.put("TAG", tag.trim());
        cv.put("CONTENT", content.trim());
        return db.insert(tableName, null, cv);
    }

    private int deleteDBContent(String index, String tableName, SQLiteDatabase db) {

        return db.delete(tableName, "ID = ?", new String[]{index});
    }

    private List<String> getAllTables() {

        List<String> tableList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getDatabase();
        Cursor cs = db.rawQuery("select name from sqlite_master where type='table'", null);
        while (cs.moveToNext()) {
            String name = cs.getString(cs.getColumnIndex("name"));
            tableList.add(name);
        }
        return tableList;
    }

    private int getMaxID(String tableName, SQLiteDatabase db) {
        int maxId = 0;
        Cursor cs = db.rawQuery("SELECT ID FROM " + tableName, null);
        cs.moveToLast();
        cs.moveToPrevious();
        maxId = Integer.parseInt(cs.getString(cs.getColumnIndex("ID")).replace("\ufeff", "").trim());
        cs.close();
        return maxId;
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
//        EditText searchContent = (EditText) findViewById(R.id.searchContent);
//        searchContent.clearFocus();
        if (dragging) {
            View view = getWindow().peekDecorView();
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
        ActionBar ab = getSupportActionBar();
        if (scrollState == ScrollState.UP) {
            if (ab.isShowing()) {
                ab.hide();
            }
        } else if (scrollState == ScrollState.DOWN) {
            if (!ab.isShowing()) {
                ab.show();
            }
        }
    }

    public boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if (!isNum.matches()) {
            return false;
        }
        return true;
    }

    private int[] showPopupMenu(final int position, final SQLiteDatabase db, final String tableName) {
        // View当前PopupMenu显示的相对View的位置
//        PopupMenu popupMenu = new PopupMenu(this, view);
//
//        // menu布局
//        popupMenu.getMenuInflater().inflate(R.menu.menu_popu, popupMenu.getMenu());
//
//        // menu的item点击事件
//        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//
//                return false;
//            }
//        });
//
//        // PopupMenu关闭事件
//        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
//            @Override
//            public void onDismiss(PopupMenu menu) {
//
//            }
//        });
//
//        popupMenu.show();plugin
        final int[] updateOrEdit = new int[1];
        updateOrEdit[0] = -1;
        final Context currentContext = this;
        new AlertDialog.Builder(this)
                .setItems(new String[]{"更新", "删除"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        final String fieldId = currentPoints.get(position).getIndex();

                        switch (which) {
                            case 0:
                                LinearLayout ItemSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.item_modify_popu, null);

                                final EditText searchTag = (EditText) ItemSettings.findViewById(R.id.itemTag);
                                final EditText searchContent = (EditText) ItemSettings.findViewById(R.id.itemContent);

                                searchTag.setText(currentPoints.get(position).getTag());
                                searchContent.setText(currentPoints.get(position).getContent());

                                setContentToClipBoard(searchTag, searchContent);

                                AlertDialog.Builder builder = new AlertDialog.Builder(currentContext)
                                        .setTitle("")
                                        .setView(ItemSettings);

                                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int id) {

                                        String t = searchTag.getText().toString();
                                        String s = searchContent.getText().toString();
                                        currentPoints.get(position).setValue(t, s);
                                        updateDBContent(fieldId, t, s, tableName, db);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }

                                });

                                final AlertDialog dialog2 = builder.create();
                                dialog2.setCanceledOnTouchOutside(true);
                                dialog2.show();

                                WindowManager.LayoutParams layoutParams = dialog2.getWindow().getAttributes();
                                layoutParams.width = (int) (WindowManager.LayoutParams.WRAP_CONTENT);
                                layoutParams.height = (int) (1450);
                                dialog2.getWindow().setAttributes(layoutParams);
                                updateOrEdit[0] = 1;
                                break;
                            case 1:
                                new AlertDialog.Builder(currentContext)
                                        .setTitle("确认")
                                        .setMessage("确定吗？")
                                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                deleteDBContent(fieldId, tableName, db);
                                                currentPoints.remove(position);
                                                adapter.notifyDataSetChanged();
                                            }
                                        })
                                        .setNegativeButton("否", null)
                                        .show();
                                updateOrEdit[0] = 2;
                                break;
                            default:
                                break;
                        }

                    }
                }).show();
        return updateOrEdit;

    }

    private void setContentToClipBoard(final EditText searchTag, final EditText searchContent) {

        searchTag.setOnLongClickListener(new View.OnLongClickListener(){
            public boolean onLongClick(View v)
            {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setText(searchTag.getText().toString());
                Toast.makeText(MainActivity.this, "复制成功", Toast.LENGTH_LONG).show();
                return true;
            }
        });

        searchContent.setOnLongClickListener(new View.OnLongClickListener(){
            public boolean onLongClick(View v)
            {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setText(searchContent.getText().toString());
                Toast.makeText(MainActivity.this, "复制成功", Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }

    private void showTip(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
            }
        });
    }
                /**
                 * 初始化监听。
                 */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(MainActivity.class.getSimpleName(), "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码："+code);
            } else {
            }
        }
    };

    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            convertView.setCurrentItem(autoPlayIndex);
        }

        @Override
        public void onSpeakPaused() {
//            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
//            showTip("继续播放");
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
            if (error == null) {
                synchronized (this) {
                    AutoPlayHandler.postDelayed(AutoPlayrunnable, 500);
                }
//                showTip("播放完成");
            } else if (error != null) {
//                showTip(error.getPlainDescription(true));
            }
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

    //获取发音人资源路径
    private String getResourcePath(){
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/"+voicerLocal+".jet"));
        return tempBuffer.toString();
    }


    /**
     * 参数设置
     * @param
     * @return
     */
    private void setParam(String speed, String pitch, String preference){
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置合成
        if(mEngineType.equals(SpeechConstant.TYPE_CLOUD))
        {
            //设置使用云端引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME,voicerCloud);
        }else {
            //设置使用本地引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            //设置发音人资源路径
            mTts.setParameter(ResourceUtil.TTS_RES_PATH,getResourcePath());
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME,voicerLocal);
        }
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", speed));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", pitch));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", preference));
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
    }

    //设备API大于6.0时，主动申请权限
    private void requestPermission(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, 0);

            }
        }
    }

    private class MailSender extends Thread {
        private String attachment;

        public MailSender(String attachment) {
            this.attachment = attachment;
        }
        @Override
        public void run() {
            try {
                // 这个类主要来发送邮件
                showTip("preparing data..");
                SimpleMailSender sms = new SimpleMailSender("dats", "datc");
//				if(sms.sendTextMail(mailInfo,getBaseContext().getFilesDir().getParent() + "/mydatabase.db")){//有附件
                if(sms.sendTextMail(attachment)){//无附件
                    showTip("send mail successfully.");
                }else{
                    showTip("send mail failed.");
                }
            } catch (Exception e) {
                Log.e("SendMail", e.getMessage(), e);
            }
        }
    }
}
