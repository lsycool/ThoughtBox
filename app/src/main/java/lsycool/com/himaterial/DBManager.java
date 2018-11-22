package lsycool.com.himaterial;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class DBManager {
     
    private final int BUFFER_SIZE = 10*1024*1024;

     
    private SQLiteDatabase database;
    private Context context;
 
    public DBManager(Context context) {
        this.context = context;
    }
 
    public SQLiteDatabase getDatabase() {
        return database;
    }
 
    public void setDatabase(SQLiteDatabase database) {
        this.database = database;
    }
 
    public void openDatabase() {

    	String path = context.getFilesDir().getParent() + "/storys.db3";
    	this.database = this.openDatabase(path);
    }
 
    private SQLiteDatabase openDatabase(String dbfile) {

        SQLiteDatabase db = null;
        try {
            if (!(new File(dbfile).exists())) {

                InputStream is = this.context.getResources().openRawResource(R.raw.storys);
                FileOutputStream fos = new FileOutputStream(dbfile);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();

                db = SQLiteDatabase.openOrCreateDatabase(dbfile, null);
//                String create_table = "create table LSY(_id integer primary key autoincrement,tag text,content text)";
//                db.execSQL(create_table);
            } else {
                db = SQLiteDatabase.openOrCreateDatabase(dbfile, null);
            }
        } catch (FileNotFoundException e) {
            Log.e("Database", "File not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("Database", "IO exception");
            e.printStackTrace();
        }

        return db;
    }

	
	/**
	 * query all persons, return cursor
	 * @return	Cursor
	 */
	public Cursor queryTheCursor() {
        Cursor c = database.rawQuery("SELECT * FROM db", null);
        return c;
	}
     
    public void closeDatabase() {
        this.database.close();
 
    }
}
