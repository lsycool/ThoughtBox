package lsycool.com.update;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import lsycool.com.himaterial.R;

/**
 * @author coolszy
 * @date 2012-4-26
 * @blog http://blog.92coding.com
 */

public class UpdateManager {
	/* 下载中 */
	private static final int DOWNLOAD = 1;
	/* 下载结束 */
	private static final int DOWNLOAD_FINISH = 2;

	private static final int UPLOADING = 3;

	private static final int UPLOAD_FINISH = 4;

	/* 保存解析的XML信息 */
	HashMap<String, String> mHashMap;
	/* 下载保存路径 */
	private String mSavePath;
	/* 记录进度条数量 */
	private int progress = 0;
	/* 是否取消更新 */
	private boolean cancelUpdate = false;

	private String upclass = "app";

	private Context mContext;

	private ProgressBar pb;

	private TextView tvProgress;

	private AlertDialog dialogProgress;

	/* 更新进度条 */
	// private ProgressBar mProgress;
	// private Dialog mDownloadDialog;

	private String urlStr = "http://www.lsycool.cn/personal/version.xml";
	private InputStream inputStream;
	private boolean flag = false;

	private String msgInfo = "正在下载。。";

	private void setConfigUrl(String url) {
		urlStr = url;
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// 正在下载
			case DOWNLOAD:
				// 设置进度条位置
				// mProgress.setProgress(progress);
				if (pb != null && tvProgress != null) {
					pb.setProgress(progress);
					tvProgress.setText(mContext.getString(R.string.versionchecklib_progress) + progress);
				}
				break;
			case DOWNLOAD_FINISH:
				// 安装文件
				if (pb != null && tvProgress != null) {
					dialogProgress.setMessage(msgInfo);
					dialogProgress.getButton(AlertDialog.BUTTON_NEGATIVE).setText("确定");
				}
				dialogProgress.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						installApk();
						dialogProgress.dismiss();
					}});
				break;
			case UPLOADING:
				if (pb != null && tvProgress != null) {
					pb.setProgress(progress);
					tvProgress.setText(mContext.getString(R.string.versionchecklib_progress) + progress);
				}
				break;
			case UPLOAD_FINISH:
				if (pb != null && tvProgress != null) {
					dialogProgress.setMessage("上传成功");
					dialogProgress.getButton(AlertDialog.BUTTON_NEGATIVE).setText("确定");
				}
				dialogProgress.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						dialogProgress.dismiss();
					}});
				break;
			default:
				break;
			}
		};
	};

	public UpdateManager(Context context) {
		this.mContext = context;
	}

	/**
	 * 检测软件更新
	 */
	public void checkUpdate() {
		if (isUpdate()) {
			// 显示提示对话框
			showNoticeDialog();
		} else {
			if (!flag) {
				flag = !flag;
				Toast.makeText(mContext, R.string.soft_update_no, Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * 检查软件是否有更新版本
	 * 
	 * @return
	 */
	private boolean isUpdate() {
		// 获取当前软件版本
		int versionCode = getVersionCode(mContext);
		// 把version.xml放到网络上，然后获取文件信息
		// inputStream =
		// ParseXmlService.class.getClassLoader().getResourceAsStream("version.xml");
		getInputStreamFromHttpUrl thread = new getInputStreamFromHttpUrl();
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (null != mHashMap) {
			int serviceCode = Integer.valueOf(mHashMap.get("version"));
			// String appname = mHashMap.get("name");
			// 版本判断
			if (serviceCode > versionCode) {
				return true;
			}
		} else {
			flag = true;
			Toast.makeText(mContext, "无法连接到服务器！", Toast.LENGTH_LONG).show();
		}
		return false;
	}

	private class getInputStreamFromHttpUrl extends Thread {
		@Override
		public void run() {
			try {
				// 把网络访问的代码放在这里
				URL url = new URL(urlStr);
				HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
				urlConn.setConnectTimeout(6000);
				inputStream = urlConn.getInputStream();
				if (inputStream != null) {
					ParseXmlService service = new ParseXmlService();
					mHashMap = service.parseXml(inputStream);
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// Toast.makeText(mContext, "配置文件错误！",
				// Toast.LENGTH_LONG).show();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				// Toast.makeText(mContext, "无法连接到网络！",
				// Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				e.printStackTrace();
				// Toast.makeText(mContext, "配置文件解析错误！",
				// Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * 获取软件版本号
	 * 
	 * @param context
	 * @return
	 */
	private int getVersionCode(Context context) {
		int versionCode = 0;
		try {
			// 获取软件版本号，对应AndroidManifest.xml下android:versionCode
			versionCode = context.getPackageManager().getPackageInfo("lsycool.com.himaterial", 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionCode;
	}

	/**
	 * 展示进度条
	 *
	 * @return
	 */
	private void showLoadingDialog() {

		View loadingView = LayoutInflater.from(mContext).inflate(R.layout.downloading_layout, null);
		dialogProgress = new AlertDialog.Builder(mContext)
				.setMessage(msgInfo)
				.setView(loadingView)
				.setNegativeButton(R.string.soft_update_cancel, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						cancelUpdate = true;
						dialog.dismiss();
					}
				})
				.create();

		pb = (ProgressBar) loadingView.findViewById(R.id.pb);
		tvProgress = (TextView) loadingView.findViewById(R.id.tv_progress);
		tvProgress.setText(mContext.getString(R.string.versionchecklib_progress) + 0);
		pb.setProgress(0);
		dialogProgress.setCanceledOnTouchOutside(false);
		dialogProgress.setCancelable(false);
		dialogProgress.show();
	}

	/**
	 * 显示软件更新对话框
	 */
	private void showNoticeDialog() {
		// 构造对话框
		AlertDialog.Builder builder = new Builder(mContext);
		upclass = mHashMap.get("upclass");
		builder.setTitle(R.string.soft_update_title);
		if (upclass.equals("app")) {
			builder.setMessage(R.string.soft_update_info);
		} else if (upclass.equals("data")) {
			builder.setMessage(R.string.soft_update_info1);
		}else if (upclass.equals("dataapp")) {
			builder.setMessage(R.string.soft_update_info2);			
		}
		// 更新
		builder.setPositiveButton(R.string.soft_update_updatebtn, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				// 显示下载对话框
				msgInfo = "正在下载。。";
				if (upclass.equals("app")) {
					showLoadingDialog();
					downloadApk();
				} else if (upclass.equals("data")) {
					showLoadingDialog();
					downloadData();
				}else if (upclass.equals("dataapp")) {
					showLoadingDialog();
					downloadDataApp();
				}
			}
		});
		// 稍后更新
		builder.setNegativeButton(R.string.soft_update_later, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();

			}
		});
		Dialog noticeDialog = builder.create();
		noticeDialog.show();
	}


	/**
	 * 下载apk文件
	 */
	private void downloadApk() {
		// 启动新线程下载软件
		new downloadApkThread().start();
	}

	/**
	 * 下载文件线程
	 * 
	 * @author coolszy
	 * @date 2012-4-26
	 * @blog http://blog.92coding.com
	 */
	private class downloadApkThread extends Thread {
		@Override
		public void run() {
			try {
				// 判断SD卡是否存在，并且是否具有读写权限
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					// 获得存储卡的路径
					String sdpath = mContext.getExternalFilesDir(null) + "/";
					mSavePath = sdpath + "download";
					URL url = new URL(mHashMap.get("url1"));
					// 创建连接
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(5000);
					conn.setConnectTimeout(6000);
					conn.setRequestProperty("Charset", "UTF-8");
					conn.setRequestMethod("GET");
					conn.connect();
					// 获取文件大小
					int length = conn.getContentLength();
					// 创建输入流
					InputStream is = conn.getInputStream();

					File file = new File(mSavePath);
					// 判断文件目录是否存在
					if (!file.exists()) {
						file.mkdir();
					}
					File apkFile = new File(mSavePath, mHashMap.get("name")+".apk");
					FileOutputStream fos = new FileOutputStream(apkFile);
					int count = 0;
					// 缓存
					byte buf[] = new byte[1024];
					// 写入到文件中
					do {
						int numread = is.read(buf);
						count += numread;
						// 计算进度条位置
						progress = (int) Math.ceil(((float) count / length) * 100);
						// 更新进度
						mHandler.sendEmptyMessage(DOWNLOAD);
						if (numread <= 0) {
							// 下载完成
							mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
							break;
						}
						// 写入文件
						fos.write(buf, 0, numread);
					} while (!cancelUpdate);// 点击取消就停止下载.
					fos.close();
					is.close();
//					dialogProgress.dismiss();
				}
			} catch (MalformedURLException e) {
				dialogProgress.dismiss();
				e.printStackTrace();
			} catch (IOException e) {
				dialogProgress.dismiss();
				e.printStackTrace();
			}
			// 取消下载对话框显示
			// mDownloadDialog.dismiss();
		}
	};

	/*
	 * 下载数据
	 */
	private void downloadData() {
		new downloadDataThread().start();
	}

	/**
	 * 下载数据线程
	 * 
	 * @author coolszy
	 * @date 2012-4-26
	 * @blog http://blog.92coding.com
	 */
	private class downloadDataThread extends Thread {
		@Override
		public void run() {
			try {
				// 判断SD卡是否存在，并且是否具有读写权限
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					// 获得存储卡的路径
					String path = mContext.getFilesDir().getParent();// "/data/data/com.lsy.namespace";
					String mSavePath = path + "/storys.db3";
					URL url = new URL(mHashMap.get("url2"));
					// 创建连接
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setConnectTimeout(6000);
					conn.connect();
					// 获取文件大小
					int length = conn.getContentLength();
					// 创建输入流
					InputStream is = conn.getInputStream();

					File Datafile = new File(mSavePath);
					// 判断文件目录是否存在
					if (Datafile.exists()) {
						Datafile.delete();
					}
					// File apkFile = new File(mSavePath, "mydatabase.db");
					FileOutputStream fos = new FileOutputStream(Datafile);
					int count = 0;
					// 缓存
					byte buf[] = new byte[1024];
					// 写入到文件中
					do {
						int numread = is.read(buf);
						count += numread;
						// 计算进度条位置
						progress = (int) Math.ceil(((float) count / length) * 100);
						// 更新进度
						mHandler.sendEmptyMessage(DOWNLOAD);
						if (numread <= 0) {
							// 下载完成
							break;
						}
						// 写入文件
						fos.write(buf, 0, numread);
					} while (!cancelUpdate);// 点击取消就停止下载.
					fos.close();
					is.close();
//					dialogProgress.dismiss();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				dialogProgress.dismiss();
			} catch (IOException e) {
				e.printStackTrace();
				dialogProgress.dismiss();
			}
			// 取消下载对话框显示
			// mDownloadDialog.dismiss();
		}
	};

	/**
	 * 下载数据线程
	 * 
	 * @author coolszy
	 * @date 2012-4-26
	 * @blog http://blog.92coding.com
	 */
	private class downloadDataAppThread extends Thread {
		@Override
		public void run() {
			try {
				// 判断SD卡是否存在，并且是否具有读写权限
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					// 获得存储卡的路径
					String path = mContext.getFilesDir().getParent();
					String mSavePath2 = path + "/mydatabase.db";
					
					String sdpath = Environment.getExternalStorageDirectory() + "/";
					String mSavePath1 = sdpath + "download";
					mSavePath = mSavePath1;

					URL url1 = new URL(mHashMap.get("url1"));//apk路径
					URL url2 = new URL(mHashMap.get("url2"));//数据路径
					// 创建连接
					HttpURLConnection conn1 = (HttpURLConnection) url1.openConnection();
					conn1.setConnectTimeout(6000);
					conn1.connect();
					
					HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
					conn2.setConnectTimeout(6000);
					conn2.connect();

					// 获取文件大小
					int length = conn1.getContentLength() + conn2.getContentLength();
					// 创建输入流
					InputStream is1 = conn1.getInputStream();//apk
					InputStream is2 = conn2.getInputStream();//data

					File Datafile = new File(mSavePath2);
					// 判断文件目录是否存在
					if (Datafile.exists()) {
						Datafile.delete();
					}
					FileOutputStream fos2 = new FileOutputStream(Datafile);

					File file = new File(mSavePath1);
					// 判断文件目录是否存在
					if (!file.exists()) {
						file.mkdir();
					}
					File apkFile = new File(mSavePath1, mHashMap.get("name"));
					FileOutputStream fos1 = new FileOutputStream(apkFile);
					

					int count = 0;
					// 缓存
					byte buf[] = new byte[1024];
					// 写入到文件中
					do {
						int numread = is2.read(buf);
						count += numread;
						// 计算进度条位置
						progress = (int) Math.ceil(((float) count / length) * 100);
						// 更新进度
						mHandler.sendEmptyMessage(DOWNLOAD);
						if (numread <= 0) {
							break;
						}
						// 写入文件
						fos2.write(buf, 0, numread);
					} while (!cancelUpdate);// 点击取消就停止下载.
					
					fos2.close();
					is2.close();

					// 写入到文件中
					do {
						int numread = is1.read(buf);
						count += numread;
						// 计算进度条位置
						progress = (int) Math.ceil(((float) count / length) * 100);
						// 更新进度
						mHandler.sendEmptyMessage(DOWNLOAD);
						if (numread <= 0) {
							// 下载完成
							mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
							break;
						}
						// 写入文件
						fos1.write(buf, 0, numread);
					} while (!cancelUpdate);// 点击取消就停止下载.
					
					fos1.close();
					is1.close();
//					dialogProgress.dismiss();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				dialogProgress.dismiss();
			} catch (IOException e) {
				e.printStackTrace();
				dialogProgress.dismiss();
			}
		}
	};
	
	/*
	 * 下载数据
	 */
	private void downloadDataApp() {
		new downloadDataAppThread().start();
	}


	/**
	 * 安装APK文件
	 */
	private void installApk() {
		File apkfile = new File(mSavePath, mHashMap.get("name")+".apk");
		if (!apkfile.exists()) {
			return;
		}
		// 通过Intent安装APK文件
		Uri uri;
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.N){
			uri= FileProvider.getUriForFile(mContext, mContext.getPackageName()+".versionProvider",apkfile);
			i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}else{
			uri=Uri.fromFile(apkfile);
		}
		i.setDataAndType(uri, "application/vnd.android.package-archive");
		mContext.startActivity(i);
	}

	public void uploadFiles(String filePath, String uploadUrl)
	{
		AsyncHttpClient client = new AsyncHttpClient();
		RequestParams params = new RequestParams();

		msgInfo = "正在上传。。";

		//添加文件
		try {
			File f = new File(filePath) ;
			if(f.exists()){
				Log.i("AsyncHttp", "Yes") ;
				params.put("file", f);
			}else{
				Log.i("AsyncHttp", "No") ;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
                 /*//////////////
                 * /把文件上传*/
		client.post(uploadUrl, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int i, cz.msebera.android.httpclient.Header[] headers, byte[] bytes) {

				try {
					String resp = new String(bytes, "utf-8");
					mHandler.sendEmptyMessage(UPLOAD_FINISH);
					Log.i("result:",resp);
					//在这里处理返回的内容，例如解析json什么的...
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

			}

			@Override
			public void onStart() {
				// called before request is started
				showLoadingDialog();
			}

			@Override
			public void onFailure(int i, cz.msebera.android.httpclient.Header[] headers, byte[] bytes, Throwable throwable) {

			}

			@Override
			public void onRetry(int retryNo) {
				// called when request is retried
			}

			@Override
			public void onProgress(long bytesWritten, long totalSize) {

				// 计算进度条位置
				progress = (int) Math.ceil(Double.valueOf(totalSize > 0L?(double)bytesWritten * 1.0D / (double)totalSize * 100.0D : -1.0D));
				// 更新进度
				mHandler.sendEmptyMessage(UPLOADING);
				if (bytesWritten >= totalSize) {
					// 下载完成
				}
			}

		});

	}

}
