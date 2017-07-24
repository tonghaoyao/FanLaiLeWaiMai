package com.fanlaile.fanlailewaimai.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.fanlaile.fanlailewaimai.R;
import com.fanlaile.fanlailewaimai.app.adapter.DividerDecoration;
import com.fanlaile.fanlailewaimai.app.adapter.GoodsAdapter;
import com.fanlaile.fanlailewaimai.app.adapter.SelectAdapter;
import com.fanlaile.fanlailewaimai.app.adapter.TypeAdapter;
import com.fanlaile.fanlailewaimai.app.bean.GoodsBeanData;
import com.fanlaile.fanlailewaimai.app.bean.GoodsItem;
import com.fanlaile.fanlailewaimai.home.adapter.HomeFragmentAdapter;
import com.fanlaile.fanlailewaimai.home.bean.ShopsBean;
import com.fanlaile.fanlailewaimai.order.bean.OrderBean;
import com.fanlaile.fanlailewaimai.utils.CacheUtils;
import com.fanlaile.fanlailewaimai.utils.Constants;
import com.fanlaile.fanlailewaimai.utils.LogUtil;
import com.fanlaile.fanlailewaimai.utils.StringUtils;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by dell1 on 2017-05-24 .
 * 作者: 童浩瑶 on 8:54
 * QQ号: 1339170870
 * 作用: 每个店铺的商品信息页面
 */

public class GoodsInfoActivity extends Activity implements View.OnClickListener {

    private Context context;
    public static final String ORDER_INFO = "order_info";
    private ShopsBean shopsBean;
    private ImageView imgCart;
    private ViewGroup anim_mask_layout;
    private RecyclerView rvType, rvSelected;
    private TextView tvCount, tvCost, tvSubmit, tvTips;
    private BottomSheetLayout bottomSheetLayout;
    private View bottomSheet;
    private StickyListHeadersListView listView;
    private ImageView iv_back;
    private TextView tv_shopname;
    private ImageView iv_share;
    private TextView tvDistribution;

    private GoodsAdapter myAdapter;
    private SelectAdapter selectAdapter;
    private TypeAdapter typeAdapter;

    //商品列表
    private ArrayList<GoodsItem> goodsList;
    //分类列表
    private ArrayList<GoodsItem> typeList;
    //已选择的商品:SparseArray这个类其实就是 HashMap< Integer,Object >
    //既可以根据key查找Value，也可以根据位置查找value
    private SparseArray<GoodsItem> selectedList;
    //用于记录每个分组选择的数目SparseIntArray 其实是HashMap< Integer,Integer> 的替代者
    private SparseIntArray groupSelect;

    private NumberFormat nf;
    private Handler mHanlder;
    private GoodsBeanData.ResultData resultData;


    /**
     * 我竟然在这里重写了onCreate的两个参数的方法, 我真的是找了好久的错误!!!
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goodsinfo);

        context = this;
        nf = NumberFormat.getCurrencyInstance();
        nf.setMaximumFractionDigits(2);
        mHanlder = new Handler(getMainLooper());

        //初始化视图
        initView();

//        接收intent数据
        shopsBean = (ShopsBean) getIntent().getSerializableExtra(HomeFragmentAdapter.SHOPS_BEAN);
        if (shopsBean != null) {
            LogUtil.e("shop_name==" + shopsBean.getShop_name());
            //得到传递过来的数据
//            Toast.makeText(this, "shop_name==" + shopsBean.getShop_name(), Toast.LENGTH_SHORT).show();
            tv_shopname.setText(shopsBean.getShop_name());

//            //联网之前请求缓存
//            String cacheData = CacheUtils.getString(context, shopsBean.getShop_name());
//            if (!TextUtils.isEmpty(cacheData)) {
//                //解析json数据
//                parseJson(cacheData);
//            }

            getDataFromNet(shopsBean.getGoods_url());
        } else {
            LogUtil.e("没有得到传递过来的数据");
        }

    }

    private void getDataFromNet(String goods_url) {
        OkHttpUtils
                .get()
                .url(Constants.BASE_URL + goods_url)
                .build()
                .execute(new UserCallback() {
                    /**
                     * 联网失败的时候调用
                     */
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        LogUtil.e("首页请求失败==" + e.getMessage());

                    }

                    /**
                     * 联网成功的时候调用
                     * @param response
                     * @param id
                     */
                    @Override
                    public void onResponse(Object response, int id) {
                        LogUtil.e("首页请求成功==" + response);
                        String responseData = (String) response;
                        //在真机上调试时: 使用校园天翼客户端可能 请求得到不正确的数据
                        //在使用FastJson解析数据时会报异常
                        if (StringUtils.startsWithIgnoreCase((String) response, "{")) {
                            //使用FastJson解析json数据
                            //根据商店名字对文本进行缓存
//                            CacheUtils.saveString(context, shopsBean.getShop_name(), responseData);
                            parseJson(responseData);
                        } else {
                            //得到不正确的数据不进行解析
                            LogUtil.e("得到不正确的数据不进行解析");
                        }
                    }

                });
    }

    /**
     * 自定义Callback解决字符串乱码问题
     */
    private abstract class UserCallback extends Callback {
        @Override
        public Object parseNetworkResponse(Response response, int id) throws Exception {
            byte[] b = response.body().bytes(); //获取数据的bytes
            String info = new String(b, "GB2312"); //然后将其转为gb2312
            return info;
        }
    }

    private void parseJson(String response) {
        GoodsBeanData goodsBeanData = JSON.parseObject(response, GoodsBeanData.class);
        resultData = goodsBeanData.getResult();
        if (resultData != null) {
            //有数据
            //添加数据
            GoodsItem goodsItem = null;
            List<GoodsBeanData.ResultData.PageDataBean> pageDataBeen = resultData.getPage_data();
            for (int i = 0; i < pageDataBeen.size(); i++) {
                List<GoodsBeanData.ResultData.PageDataBean.GoodsItemBean> goodsItemBeen = pageDataBeen.get(i).getGoods_item();
                for (int j = 0; j < goodsItemBeen.size(); j++) {
                    goodsItem = new GoodsItem();
                    //商品Id
                    goodsItem.setId(100 * (i + 1) + (j + 1));
                    //typeId从1开始
                    goodsItem.setTypeId(i + 1);
                    goodsItem.setTypeName(pageDataBeen.get(i).getType_name());
                    goodsItem.setPrice(Double.parseDouble(goodsItemBeen.get(j).getPrice()));
                    goodsItem.setStarCount(goodsItemBeen.get(j).getGood_star());
                    goodsItem.setName(goodsItemBeen.get(j).getGood_name());
                    goodsItem.setShopName(pageDataBeen.get(i).getShop_name());
                    goodsList.add(goodsItem);
                    LogUtil.e("Shop_name=="+pageDataBeen.get(i).getShop_name());
                }
                typeList.add(goodsItem);
            }

            //设置适配器
            rvType.setLayoutManager(new LinearLayoutManager(this));

            typeAdapter = new TypeAdapter(this, typeList);
            rvType.setAdapter(typeAdapter);
            rvType.addItemDecoration(new DividerDecoration(this));

            myAdapter = new GoodsAdapter(goodsList, this);
            listView.setAdapter(myAdapter);

            listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    GoodsItem item = goodsList.get(firstVisibleItem);
                    if (typeAdapter.selectTypeId != item.typeId) {
                        typeAdapter.selectTypeId = item.typeId;
                        typeAdapter.notifyDataSetChanged();
                        rvType.smoothScrollToPosition(getSelectedGroupPosition(item.typeId));
                    }
                }
            });


        } else {
            //没有数据

        }
        LogUtil.e("fastjson解析数据成功==" + resultData.getPage_data().get(0).getShop_name());
//        Toast.makeText(context, resultBean.getShop_info().get(0).getShop_name(), Toast.LENGTH_LONG).show();
    }

    private void initView() {
        tvDistribution = (TextView) findViewById(R.id.tv_distribution);
        tvCount = (TextView) findViewById(R.id.tvCount);
        tvCost = (TextView) findViewById(R.id.tvCost);
        tvTips = (TextView) findViewById(R.id.tvTips);
        tvSubmit = (TextView) findViewById(R.id.tvSubmit);
        rvType = (RecyclerView) findViewById(R.id.typeRecyclerView);

        imgCart = (ImageView) findViewById(R.id.imgCart);
        anim_mask_layout = (RelativeLayout) findViewById(R.id.layout_goodsinfo);
        bottomSheetLayout = (BottomSheetLayout) findViewById(R.id.bottomSheetLayout);

        listView = (StickyListHeadersListView) findViewById(R.id.itemListView);

        iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_share = (ImageView) findViewById(R.id.iv_share);
        tv_shopname = (TextView) findViewById(R.id.tv_user_name);

        selectedList = new SparseArray<>();
        groupSelect = new SparseIntArray();

        typeList = new ArrayList<>();
        goodsList = new ArrayList<>();

    }

    public void playAnimation(int[] start_location){
        ImageView img = new ImageView(this);
        img.setImageResource(R.drawable.button_add);
        setAnim(img,start_location);
    }

    private Animation createAnim(int startX, int startY){
        int[] des = new int[2];
        imgCart.getLocationInWindow(des);

        AnimationSet set = new AnimationSet(false);

        Animation translationX = new TranslateAnimation(0, des[0]-startX, 0, 0);
        translationX.setInterpolator(new LinearInterpolator());
        Animation translationY = new TranslateAnimation(0, 0, 0, des[1]-startY);
        translationY.setInterpolator(new AccelerateInterpolator());
        Animation alpha = new AlphaAnimation(1,0.5f);
        set.addAnimation(translationX);
        set.addAnimation(translationY);
        set.addAnimation(alpha);
        set.setDuration(500);

        return set;
    }

    private void addViewToAnimLayout(final ViewGroup vg, final View view,
                                     int[] location) {

        int x = location[0];
        int y = location[1];
        int[] loc = new int[2];
        vg.getLocationInWindow(loc);
        view.setX(x);
        view.setY(y-loc[1]);
        vg.addView(view);
    }
    private void setAnim(final View v, int[] start_location) {

        addViewToAnimLayout(anim_mask_layout, v, start_location);
        Animation set = createAnim(start_location[0],start_location[1]);
        set.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                mHanlder.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        anim_mask_layout.removeView(v);
                    }
                },100);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        v.startAnimation(set);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.bottom:
                showBottomSheet();
                break;
            case R.id.clear:
                clearCart();
                break;
            case R.id.tvSubmit:
//                Toast.makeText(GoodsInfoActivity.this, "结算", Toast.LENGTH_SHORT).show();
                //打开提交订单Activity
                startCommitActivity();

                break;
            case R.id.iv_back:
//                Toast.makeText(GoodsInfoActivity.this, "返回", Toast.LENGTH_SHORT).show();
                finish();
                break;
            case R.id.iv_share:
                Toast.makeText(GoodsInfoActivity.this, "分享", Toast.LENGTH_SHORT).show();
            default:
                break;
        }
    }

    private void startCommitActivity() {
        //打开提交订单Activity
        Intent intent = new Intent(this, CommitOrderActivity.class);
        //传入订单信息
        OrderBean orderBean = new OrderBean();
        //把selectedList数据转化为ArrayList
        List<GoodsItem> goodsItems = new ArrayList<>();
        for (int i=0 ; i<selectedList.size(); i++){
            goodsItems.add(selectedList.valueAt(i));
        }

        orderBean.setGoodsItemList(goodsItems);

        intent.putExtra(ORDER_INFO, orderBean);
        this.startActivity(intent);
    }

    //添加商品
    public void add(GoodsItem item,boolean refreshGoodList){

        int groupCount = groupSelect.get(item.typeId);
        if(groupCount==0){
            groupSelect.append(item.typeId,1);
        }else{
            groupSelect.append(item.typeId,++groupCount);
        }

        GoodsItem temp = selectedList.get(item.id);
        if(temp==null){
            item.count=1;
            selectedList.append(item.id,item);
        }else{
            temp.count++;
        }
        update(refreshGoodList);
    }
    //移除商品
    public void remove(GoodsItem item,boolean refreshGoodList){

        int groupCount = groupSelect.get(item.typeId);
        if(groupCount==1){
            groupSelect.delete(item.typeId);
        }else if(groupCount>1){
            groupSelect.append(item.typeId,--groupCount);
        }

        GoodsItem temp = selectedList.get(item.id);
        if(temp!=null){
            if(temp.count<2){
                selectedList.remove(item.id);
            }else{
                item.count--;
            }
        }
        update(refreshGoodList);
    }
    //刷新布局 总价、购买数量等
    private void update(boolean refreshGoodList){
        int size = selectedList.size();
        int count =0;
        double cost = 0;
        double distribution = 0;
        for(int i=0;i<size;i++){
            GoodsItem item = selectedList.valueAt(i);
            count += item.count;

            //加入配送费一件商品一元配送费
            distribution += 1*item.count;
            cost += item.count*item.price + 1*item.count;
        }

        if(count<1){
            tvCount.setVisibility(View.GONE);
        }else{
            tvCount.setVisibility(View.VISIBLE);
        }

        tvCount.setText(String.valueOf(count));


        //设置满多少起送
        if(cost > 9.99){
            tvTips.setVisibility(View.GONE);
            tvSubmit.setVisibility(View.VISIBLE);
        }else{
            tvSubmit.setVisibility(View.GONE);
            tvTips.setVisibility(View.VISIBLE);
        }

        tvCost.setText(nf.format(cost));
        tvDistribution.setText("(含运费" +nf.format(distribution) +")");

        if(myAdapter!=null && refreshGoodList){
            myAdapter.notifyDataSetChanged();
        }
        if(selectAdapter!=null){
            selectAdapter.notifyDataSetChanged();
        }
        if(typeAdapter!=null){
            typeAdapter.notifyDataSetChanged();
        }
        if(bottomSheetLayout.isSheetShowing() && selectedList.size()<1){
            bottomSheetLayout.dismissSheet();
        }
    }
    //清空购物车
    public void clearCart(){
        selectedList.clear();
        groupSelect.clear();
        update(true);

    }
    //根据商品id获取当前商品的采购数量
    public int getSelectedItemCountById(int id){
        GoodsItem temp = selectedList.get(id);
        if(temp==null){
            return 0;
        }
        return temp.count;
    }
    //根据类别Id获取属于当前类别的数量
    public int getSelectedGroupCountByTypeId(int typeId){
        return groupSelect.get(typeId);
    }
    //根据类别id获取分类的Position 用于滚动左侧的类别列表
    public int getSelectedGroupPosition(int typeId){
        for(int i=0;i<typeList.size();i++){
            if(typeId==typeList.get(i).typeId){
                return i;
            }
        }
        return 0;
    }

    public void onTypeClicked(int typeId){
        listView.setSelection(getSelectedPosition(typeId));
    }

    private int getSelectedPosition(int typeId){
        int position = 0;
        for(int i=0;i<goodsList.size();i++){
            if(goodsList.get(i).typeId == typeId){
                position = i;
                break;
            }
        }
        return position;
    }

    private View createBottomSheetView(){
        View view = LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet,(ViewGroup) getWindow().getDecorView(),false);
        rvSelected = (RecyclerView) view.findViewById(R.id.selectRecyclerView);
        rvSelected.setLayoutManager(new LinearLayoutManager(this));
        TextView clear = (TextView) view.findViewById(R.id.clear);
        clear.setOnClickListener(this);
        //传入selectedList数据给底部弹出菜单栏
        selectAdapter = new SelectAdapter(this,selectedList);
        rvSelected.setAdapter(selectAdapter);
        return view;
    }

    private void showBottomSheet(){
        if(bottomSheet==null){
            bottomSheet = createBottomSheetView();
        }
        if(bottomSheetLayout.isSheetShowing()){
            bottomSheetLayout.dismissSheet();
        }else {
            if(selectedList.size()!=0){
                bottomSheetLayout.showWithSheetView(bottomSheet);
            }
        }
    }
}
