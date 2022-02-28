博客：https://blog.csdn.net/u011124212/article/details/123109914
# 一、源码分析
with主线：
1、给我们build了一个Glide对象（GlideBuilder里返回）
2、给我们创建了空的Fragment
3、把声明周期方法传导出去，谁实现了lifecycle，谁就能操作生命周期
load主线：传入参数，构建出RequestBuilder对象
into主线：运行队列 等待队列 活动缓存 内存缓存 网络模型
# 二、手写自己的图片加载
## 步骤一、生命周期的实现
1、创建一个空的Fragment
2、绑定声明周期和接口

先创建绑定生命周期接口类和声明周期接口类

```c
public interface LifeCycle {
    void addListener(@NonNull LifeCycleListener listener);

    void removeListener(@NonNull LifeCycleListener listener);
}
```

```c
public interface LifeCycleListener {

    void onStart();

    void onStop();

    void onDestroy();
}
```
创建LifeCycle的实现类ActivityFragmentLifeCycle

```c
public class ActivityFragmentLifeCycle implements LifeCycle {

    // 容器
    private final Set<LifecycleListener> lifecycleListeners =
            Collections.newSetFromMap(new WeakHashMap<LifecycleListener, Boolean>());
    private boolean isStarted;  // 启动的标记
    private boolean isDestroyed; // 销毁的标记

    @Override
    public void addListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.add(listener);

        if (isDestroyed) {
            listener.onDestroy();
        } else if (isStarted) {
            listener.onStart();
        } else {
            listener.onStop();  // 首次启动：会默认 onStop 先停止   然后再onStart
        }
    }

    @Override
    public void removeListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    void onStart() {
        isStarted = true;
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStart();
        }
    }

    void onStop() {
        isStarted = false;
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStop();
        }
    }

    void onDestroy() {
        isDestroyed = true;
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onDestroy();
        }
    }
}
```


创建空的（无UI）Fragment，名称跟Glide保持一致，在Fragment的生命周期方法中调用lifecycle的方法以实现绑定

```c
public class RequestManagerFragment extends Fragment {

    private  ActivityFragmentLifeCycle lifecycle = null;
    @Nullable
    private RequestManager requestManager;

    public RequestManagerFragment() {
        this(new ActivityFragmentLifeCycle());
    }

    public RequestManagerFragment(@NonNull ActivityFragmentLifeCycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void setRequestManager(@Nullable RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    @NonNull
    public ActivityFragmentLifeCycle getGlideLifecycle() {
        return lifecycle;
    }

    @Nullable
    public RequestManager getRequestManager() {
        return requestManager;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // this.lifecycle.addListener(requestManager);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        lifecycle.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        lifecycle.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycle.onDestroy();
    }
}

```

因为我们最终要实现这种方式Glide.with(this).load(url).into(iv_image);我们新建一个Glide类，并且写一个with方法，从源码中我们看到with返回的是RequestManager，还有一个RequestManagerRetriever类，用来专门管理RequestManager，并在其get方法中调用了supportFragmentGet方法，这个方法里做了如下操作：
1、从 FragmentManager 中获取 SupportRequestManagerFragment(空白)，
2、从该 空白Fragment 中获取 RequestManager
3、如果是首次获取，则实例化，设置 Fragment 对应的 RequestManager
我们也新建一个这样的管理类，实现生命周期方法LifecycleListener，并在构造函数里绑定

```c
public class RequestManagerRetriever implements Handler.Callback {
    static final String FRAGMENT_TAG = "com.bumptech.glide.manager";

    @VisibleForTesting
    final Map<FragmentManager, RequestManagerFragment> pendingSupportRequestManagerFragments =
            new HashMap<>();

    private final Handler handler;

    public RequestManagerRetriever() {
        // 主线程
        handler = new Handler(Looper.getMainLooper(), this);
    }

    private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 1; // androidx Fragmetn 空白

    private volatile RequestManager applicationManager;
    @NonNull
    private RequestManager getApplicationManager(@NonNull Context context) {
        if (applicationManager == null) {
            synchronized (this) {
                if (applicationManager == null) {

                    Glide glide = Glide.get(context.getApplicationContext());
                    applicationManager =  RequestManager.getInstance(new ApplicationLifecycle(), context.getApplicationContext());
                }
            }
        }
        return applicationManager;
    }

    // 参数 Context Activity FragmentActivity ... 调用了 此get函数【共用的】
    @NonNull
    public RequestManager get(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("你个货，传入的是 空的 context，要把你吊起来打...");
        } else if (Util.isOnMainThread() && !(context instanceof Application)) {
            if (context instanceof FragmentActivity) {
                return get((FragmentActivity) context); // 进入FragmentActivity的get函数
            } else if (context instanceof Activity) {
                return get((Activity) context); // 进入Activity的get函数
            } else if (context instanceof ContextWrapper && ((ContextWrapper) context).getBaseContext().getApplicationContext() != null) {
                return get(((ContextWrapper) context).getBaseContext()); // 继续递归寻找 匹配合适的
            }
        }

        // 若上面的判断都不满足，就会执行下面这句代码，同学们想知道Application作用域 就需要关心这句代码（红色区域）
        return getApplicationManager(context);
    }

    @NonNull
    public RequestManager get(@NonNull FragmentActivity activity) {
        if (Util.isOnBackgroundThread()) {
            return get(activity.getApplicationContext());
        } else {
            Util.assertNotDestroyed(activity);
            FragmentManager fm = activity.getSupportFragmentManager();
            return supportFragmentGet(activity, fm);
        }
    }

    @NonNull
    public RequestManager get(@NonNull Fragment fragment) { // androidx
        if (Util.isOnBackgroundThread()) {
            return get(fragment.getContext().getApplicationContext());
        } else {
            FragmentManager fm = fragment.getChildFragmentManager();
            return supportFragmentGet(fragment.getContext(), fm);
        }
    }

    @NonNull
    private RequestManager supportFragmentGet(
            @NonNull Context context,
            @NonNull FragmentManager fm) {

        // 1、从 FragmentManager 中获取 SupportRequestManagerFragment(空白)
        RequestManagerFragment current = getSupportRequestManagerFragment(fm);

        // 2、从该 空白Fragment 中获取 RequestManager
        RequestManager requestManager = current.getRequestManager();

        // 3、首次获取，则实例化 RequestManager
        if (requestManager == null) { // 【同学们注意：这样做的目的是为了  一个Activity或Fragment 只能有一个 RequestManager】

            // 3.1 实例化
            requestManager =  RequestManager.getInstance( current.getGlideLifecycle(), context);

            // 3.2 设置 Fragment 对应的 RequestManager    空白的Fragment<--->requestManager
            current.setRequestManager(requestManager);
        }
        return requestManager;
    }

    // 1、从 FragmentManager 中获取 SupportRequestManagerFragment
    @NonNull
    private RequestManagerFragment getSupportRequestManagerFragment(
            @NonNull final FragmentManager fm) {
        RequestManagerFragment current =
                (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);

        if (current == null) {

            //  1.2 尝试从【记录保存】中获取 Fragment
            current = pendingSupportRequestManagerFragments.get(fm);

            // 1.3 实例化 Fragment
            if (current == null) {

                // 1.3.1 创建对象 空白的Fragment
                current = new RequestManagerFragment();  // 重复创建

                // 1.3.2 【记录保存】映射关系 进行保存   第一个保障
                // 一个MainActivity == 一个空白的SupportRequestManagerFragment == 一个RequestManager
                pendingSupportRequestManagerFragments.put(fm, current);

                // 1.3.3 提交 Fragment 事务
                fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();

                // 1.3.4 post 一个消息
                handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
            }
        }
        return current;
    }

    @Override
    public boolean handleMessage(Message message) {

        switch (message.what) {
            case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER: // 移除 【记录保存】  1.3.5 post 一个消息
                FragmentManager supportFm = (FragmentManager) message.obj;
                pendingSupportRequestManagerFragments.remove(supportFm); // 1.3.6 移除临时记录中的映射关系
                break;
            default:
                break;
        }

        return false;
    }
}
```

```c
public class RequestManager implements LifecycleListener {
    private static final String TAG = "RequestManager.class";
    private static RequestManager requestManager;

    private final TargetTracker targetTracker = new TargetTracker();

    private DefaultConnectivity defaultConnectivity;

    private LifeCycle lifecycle;

    private Context mContext;
    private RequestManager(LifeCycle lifecycle, Context context) {
        this.mContext = context;
        this.lifecycle = lifecycle;
        this.lifecycle.addListener(this); // 构造函数 已经给自己注册了【自己给自己绑定】
        defaultConnectivity = new DefaultConnectivity(context);
        this.lifecycle.addListener(defaultConnectivity); // 网络广播注册

    }
    public static RequestManager getInstance(LifeCycle lifecycle, Context context) {

        if (requestManager == null) {
            synchronized (RequestManager.class) {
                if (requestManager == null) {
                    requestManager = new RequestManager(lifecycle,context);
                }
            }
        }

        return requestManager;
    }

    // Activity/Fragment 可见时恢复请求 （onStart() ） 掉用函数
    @Override
    public void onStart() {
        Log.d(TAG, "开始执行生命周期业务 onStart: 运行队列 全部执行，等待队列 全部清空 ....");

        targetTracker.onStart();
        // defaultConnectivity.onStart(); 不需要
    }

    // Activity/Fragment 不可见时暂停请求 （onStop() ） 掉用函数
    @Override
    public void onStop() {
        Log.d(TAG, "开始执行生命周期业务 onStop: 运行队列 全部停止，把任务都加入到等待队列 ....");

        targetTracker.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "开始执行生命周期业务 onDestroy: 自己负责移除自己绑定的生命周期监听，释放操作 ....");
        this.lifecycle.removeListener(this); // 已经给自己销毁了 【自己给自己移除】

        targetTracker.onDestroy();

        this.lifecycle.removeListener(defaultConnectivity); // 网络广播注销
    }


    // 链式调度
    // 加载url
    public RequestBuilder load(String url) {

        return new RequestBuilder(mContext).load(url);
    }

    // 链式调度
    // 加载url
    public RequestBuilder load(Uri uri) {

        return new RequestBuilder(mContext).load(uri);
    }


    // 链式调度
    // 加载url
    public RequestBuilder load(File file) {

        return new RequestBuilder(mContext).load(file);
    }
}
```
至此，我们就完成了with方法的调用
## 步骤二 构建出RequestBuilder对象
从源码中看，load方法返回了一个RequestBuilder，这个对象主要对参数进行一些操作，真正load交给engine引擎，引擎我们待会再说，先贴出简易版的RequestBuilder的代码

```c
public class RequestBuilder {
    //路径
    // 请求路径
    private String url;

    // 上下文
    private Context context;

    private RequestOptions requestOptions;

    // 回调对象
    private RequestListener requestListener;

    public RequestBuilder(Context context) {
        this.context = context;
    }

    public void into(ImageView imageView){
        //glide 99%在into方法实现的
        //不需要传imageView，而是传target
        final ImageViewTarget imageViewTarget = new ImageViewTarget(imageView);
        imageView.setImageResource(requestOptions.getResId());
        Engine.getInstance().into(requestOptions,imageViewTarget);

    }

    public RequestBuilder apply(RequestOptions requestOptions){
        this.requestOptions=requestOptions;
        return this;
    }

    public RequestBuilder load(String url){
        //真正load交给engine引擎
        Engine.getInstance().load(url,context);
        return this;
    }

    public RequestBuilder load(Uri uri){
        //真正load交给engine引擎
        Engine.getInstance().load(uri,context);
        return this;
    }

    public RequestBuilder load(File file){
        //真正load交给engine引擎
        Engine.getInstance().load(file,context);
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public RequestListener getRequestListener() {
        return requestListener;
    }

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }
}
```
## 步骤三 缓存处理
三个缓存，活动缓存、内存缓存和磁盘缓存
其中，活动缓存简单来说就是一个map，在当前页有效，当关闭了Activity，则释放活动缓存，并put到内存缓存。
内存缓存继承了LRU算法，当缓存达到上线，则最早放进来的就会被最先释放
磁盘缓存，非运行时缓存，当APP进程被杀掉，依然存在 。而另外两个是运行时缓存，进程杀死就不存在了。源码的磁盘缓存使用了DiskLruCache，我也是如此
活动缓存：

```c
public class ActiveCache {
    private Map<String, Value> mapList = new HashMap<>();
    private com.gaos.glide.glide.resource.ValueCallback valueCallBack;

    public ActiveCache(ValueCallback valueCallBack) {
        this.valueCallBack = valueCallBack;
    }

    /**
     * TODO 添加 活动缓存
     */
    public void put(String key, Value value) {
        Tool.checkNotEmpty(key); // key 不能为空

        // 每次put的时候 put进来的Value 绑定到 valueCallback
        value.setCallBack(this.valueCallBack);

        // 存储 --> 容器
        mapList.put(key, value);
    }

    /**
     * TODO 给外界获取Value
     */
    public Value get(String key) {
        Value value = mapList.get(key);
        if (null != value) {
            return value; // 返回容器里面的 Value
        }
        return null;
    }

    public void deleteActive(String key){
        if(mapList!=null) {
            mapList.remove(key);
        }
    }

}

```
内存缓存：
LRUCache自带put get等方法 所以不用自己写了

```c
public class MemoryCache extends LruCache<String, Value> {

    public MemoryCache(int maxSize) {
        super(maxSize);
    }

    /**
     * bitmap的大小
     * @param key
     * @param value
     * @return
     */
    @Override
    protected int sizeOf(String key, Value value) {
        int sdkInt = Build.VERSION.SDK_INT;

        if(sdkInt>=Build.VERSION_CODES.KITKAT){
            return value.getmBitmap().getAllocationByteCount();
        }
        return value.getmBitmap().getByteCount();
    }
}
```
磁盘缓存：

```c
public class DiskBitmapCache implements BitmapCache{

    private DiskLruCache diskLruCache;
    private static volatile DiskBitmapCache instance;
    private int maxDiskSize = 50 * 1024 *1024;
    private String imageCachePath = "image";
    private File file;
    private Context context;
    //单例
    public static DiskBitmapCache getInstance(Context context) {
        if (instance == null) {
            synchronized (DiskBitmapCache.class) {
                if (instance == null) {
                    instance = new DiskBitmapCache(context);
                }
            }
        }
        return instance;
    }

    //创建构造方法时，对DiskLruCache初始化，open

    private DiskBitmapCache(Context context){
        File file = getImageCacheFile(context,imageCachePath);
        this.file = file;
        if(!file.exists()){
            file.mkdirs();
        }
        this.context = context;
        //第一个传文件夹（私有目录的文件夹），app版本号，对应值数量，单个文件支持的最大大小
        try {

            diskLruCache = DiskLruCache.open(file,getAppVersion(context),1,maxDiskSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //获取版本号
    private int getAppVersion(Context context) {
        //获取包管理器
        PackageManager pm = context.getPackageManager();
        //获取包信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            //返回版本号
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }




    //获取图片文件
    private File getImageCacheFile(Context context, String imageCachePath) {
        String path;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            path = context.getExternalCacheDir().getPath();
        } else {
            path = context.getCacheDir().getPath();
        }
        return new File(path + File.separator + imageCachePath);
    }


    /**
     *
     * @param key
     * @param value
     */
    @Override
    public void put(String key, Value value) {
        DiskLruCache.Editor editor =null;
        OutputStream outputStream = null;
        try {
            if(diskLruCache.isClosed()){
                diskLruCache = DiskLruCache.open(file,getAppVersion(context),1,maxDiskSize);
            }
            editor = diskLruCache.edit(key);
            if(editor!=null) {
                outputStream = editor.newOutputStream(0);
                Bitmap bitmap = value.getmBitmap();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
            }
        } catch (IOException e) {
           e.printStackTrace();
        }finally {
            try {
                if(editor!=null) {
                    editor.commit();
                }
                if(diskLruCache.isClosed()){
                    diskLruCache = DiskLruCache.open(file,getAppVersion(context),1,maxDiskSize);
                }
                diskLruCache.close();
                if(outputStream!=null){
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Value get(String key) {
        Value value = new Value();
        InputStream inputStream = null;
        try {
            if(diskLruCache.isClosed()){
                diskLruCache = DiskLruCache.open(file,getAppVersion(context),1,maxDiskSize);
            }
            DiskLruCache.Snapshot snapshot = diskLruCache.get(key);
            if(snapshot==null){
                return null;
            }
             inputStream = snapshot.getInputStream(0);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            value.setmBitmap(bitmap);
            value.setKey(key);
            return value;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(inputStream!=null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public void remove(String key) {
        try {
            diskLruCache.remove(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

```

## 步骤四 Engine引擎
主要操作就是读取缓存，而从网络加载图片则分发给EngineJob处理
读取逻辑先找活动缓存，没有就找内存缓存，没有就去找磁盘缓存，在没有找服务器
从网络加载的图片首先放到活动缓存，在放到内存缓存
图片加载时，来自活动缓存，则从活动缓存移除放到内存缓存
来自内存缓存则从内存缓存移除放到活动缓存
来自磁盘缓存则放到活动缓存，移除内存缓存
总之，存储只缓存2份

```c
public class Engine implements ValueCallback,ResponseListener{
    private static final String TAG = "Engine";
    /**
     * 1、读缓存（1级，2级，3级）,读取逻辑
     * 2、分发给EngineJob
     */
    private Context glideContext;
    private String path;
    private String key;
    private ActiveCache activeCache; // 活动缓存
    private MemoryCache memoryCache; // 内存缓存
    private DiskBitmapCache diskLruCache; // 磁盘缓存
    private final int MEMORY_MAX_SIZE = 1024*1024*120;
    private static Engine engine;
    RequestOptions requestOptions;
    ImageViewTarget imageViewTarget;

    public static Engine getInstance(){
        if(engine==null){
            engine = new Engine();
        }
        return engine;
    }

    private Engine(){
        if (activeCache == null) {
            activeCache = new ActiveCache(this); // 回调给外界，Value资源不再使用了 设置监听
        }
        if (memoryCache == null) {
            memoryCache = new MemoryCache(MEMORY_MAX_SIZE); // 内存缓存
        }
    }


    public void load(String path,Context context){
        this.path = path;
        this.glideContext = context;
        this.key = new Key(path).getKey();
        diskLruCache = DiskBitmapCache.getInstance(glideContext);
    }

    public void load(Uri uri, Context context){
        this.path = uri.getPath();
        this.glideContext = context;
        this.key = new Key(path).getKey();
        diskLruCache = DiskBitmapCache.getInstance(glideContext);
    }

    public void load(File file, Context context){
        this.path = file.getAbsolutePath();
        this.glideContext = context;
        this.key = new Key(path).getKey();
        diskLruCache = DiskBitmapCache.getInstance(glideContext);
    }

    //读取缓存方法
    public void into(RequestOptions requestOptions, ImageViewTarget imageViewTarget){
        Tool.assertMainThread();
        this.requestOptions = requestOptions;
        //如果本地缓存有，就直接渲染
        Value value = cacheAction(imageViewTarget);//
        this.imageViewTarget = imageViewTarget;
        if(value!=null){
            Bitmap bitmap = value.getmBitmap();
            Matrix matrix = new Matrix();
            if(bitmap!=null) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, requestOptions.getWidth(), requestOptions.getHeight(), matrix, true);

            imageViewTarget.setResource(bitmap);
            }
        }
    }

    private Value cacheAction(ImageViewTarget imageViewTarget){
        //判断读取逻辑,1级缓存-活动缓存有，没有就找内存缓存，没有就去找磁盘缓存，找服务器
        Value value = activeCache.get(key);
        if(value!=null){
            Log.w(TAG,"1111--我是来自于活动缓存的数据");
            //发现活动缓存有，就可以在这里设置进去
            memoryCache.put(key,value);
            //-------------
            //1、发现活动缓存有，就放到内存缓存，然后移除活动缓存
            //2、服务器去获取
            //3、RequestBuilder
            //4、ImageViewTarget
            activeCache.deleteActive(key);
            return value;
        }
         value = memoryCache.get(key);
        if(value!=null){
            // 移动操作 剪切（内存--->活动）
            activeCache.put(key, value); // 把内存缓存中的元素，加入到活动缓存中...
            memoryCache.remove(key); // 移除内存缓存
            Log.w(TAG,"1111--我是来自于内存缓存的数据");
            return value;
        }
        value = diskLruCache.get(key);
        if(value!=null){
            Log.w(TAG,"1111--我是来自于磁盘缓存的数据");
            //磁盘缓存中有，就放到活动缓存，然后移除内存缓存
            activeCache.put(key,value);
            memoryCache.remove(key);
            //真正的glide  key（path,width,height,签名),UUID
            return value;
        }

        //服务器去找
        //--------------------
        new EngineJob().loadResource(path,this,glideContext,imageViewTarget,requestOptions);

        return null;
    }


    //写缓存


    @Override
    public void valueNonUseListener(String key, Value value) {

    }

    @Override
    public void responseSuccess(String key, Value value) {
        Log.d(TAG, "saveCache: >>>>>>>>>>>>>>>>>>>>>>>>>> 加载外置资源成功后 ，保存到缓存中， key:" + key + " value:" + value);
        value.setKey(key);
        if (diskLruCache != null) {
            activeCache.put(key, value);  //这个无所谓 自由控制了
            //这里不需要放内存缓存
            diskLruCache.put(key, value); // 保存到磁盘缓存中....
        }
    }

    @Override
    public void responseException(Exception e) {

    }
}
```

```c
public class EngineJob implements Runnable{
    private String path;
    private ResponseListener responseListener;
    private Context context;
    RequestOptions requestOptions;
    private ImageViewTarget imageViewTarget;
    public Value loadResource(String path, ResponseListener responseListener, Context context, ImageViewTarget imageViewTarget, RequestOptions requestOptions){
        //执行线程，线程池
        this.path = path;
        this.responseListener = responseListener;
        this.context = context;
        this.imageViewTarget = imageViewTarget;
        this.requestOptions = requestOptions;
        Uri uri = Uri.parse(path);
        if("HTTP".equalsIgnoreCase(uri.getScheme())||"HTTPS".equalsIgnoreCase(uri.getScheme())){
            ThreadPoolManager.getInstance().execute(this);
        }else{
            //区分本地还是网络
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            Value value = new Value();
            value.setmBitmap(bitmap);
            String key = new Key(path).getKey();
            value.setKey(key);
            imageViewTarget.setResource(bitmap);
            responseListener.responseSuccess(key,value);
        }
        return null;
    }
    @Override
    public void run() {
        //第三方Glide做了很多步，这里就一步做了
        //这里就去做接口请求，由子线程直接转为主线程
        final Bitmap bitmap = getImageBitmap(path);
        Executor executor = new MainThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Value value = new Value();
                Log.w("EngineJob","1111--我是来自于服务器获取的数据");
                value.setmBitmap(bitmap);
                String key = new Key(path).getKey();
                responseListener.responseSuccess(key,value);

                imageViewTarget.setResource(bitmap);
            }
        });

    }


    private Bitmap getImageBitmap(String url){
        Bitmap bitmap=null;
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            URL imageUrl = new URL(url);
            //要用到HttpURLConnection
            conn = (HttpURLConnection) imageUrl.openConnection();
            conn.connect();
             is = conn.getInputStream();
            //Bitmap工厂类，流转化成Bitmap
            bitmap = BitmapFactory.decodeStream(is);
            Matrix matrix = new Matrix();
            bitmap =  Bitmap.createBitmap(bitmap,0,0,requestOptions.getWidth(), requestOptions.getHeight(), matrix,true);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(is!=null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(conn!=null) {
                conn.disconnect();
            }
        }
        return bitmap;
    }
}
```
