package com.fanlaile.fanlailewaimai.app;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fanlaile.fanlailewaimai.R;
import com.fanlaile.fanlailewaimai.app.bean.GoodsItem;
import com.fanlaile.fanlailewaimai.order.adapter.CommitOrderAdapter;
import com.fanlaile.fanlailewaimai.order.bean.OrderBean;
import com.fanlaile.fanlailewaimai.order.bean.RunnerBean;
import com.fanlaile.fanlailewaimai.order.bean.UserBean;
import com.fanlaile.fanlailewaimai.order.utils.OrderStorage;
import com.fanlaile.fanlailewaimai.order.utils.UserStorage;
import com.fanlaile.fanlailewaimai.utils.JudgeUtil;
import com.fanlaile.fanlailewaimai.utils.LogUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

import static com.fanlaile.fanlailewaimai.app.SelectRunnerActivity.RUNNER_BEAN;
import static com.fanlaile.fanlailewaimai.order.bean.OrderBean.ORDER_STATE_COMMITED;
import static com.fanlaile.fanlailewaimai.order.bean.OrderBean.ORDER_STATE_UNCOMMITED;

/**
 * 提交/发送 订单页面
 */

public class CommitOrderActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    @InjectView(R.id.iv_back)
    ImageView ivBack;
    @InjectView(R.id.tv_user_name)
    TextView tvShopname;
    @InjectView(R.id.iv_share)
    ImageView ivShare;
    @InjectView(R.id.ivDingdan1)
    ImageView ivDingdan1;
    @InjectView(R.id.tvDingdan)
    TextView tvDingdan;
    @InjectView(R.id.btnDingdan)
    Button btnDingdan;
    @InjectView(R.id.layout_no_user)
    RelativeLayout rlNoUser;
    @InjectView(R.id.rv_order_commit)
    RecyclerView rvOrderCommit;
    @InjectView(R.id.activity_commit_order)
    RelativeLayout activityCommitOrder;
    @InjectView(R.id.tvCost)
    TextView tvCost;
    @InjectView(R.id.tvSubmit)
    TextView tvSubmit;
    @InjectView(R.id.bottom)
    LinearLayout bottom;
    @InjectView(R.id.tv_distribution)
    TextView tvDistribution;

    private OrderBean orderBean;

    private String destinationAddress;
    private String text;
    private PendingIntent sentPI;
    private PendingIntent deliverPI;
    private Context context;

    private TelephonyManager mTelephonyManager;
    private Class<TelephonyManager> clz;

    /**
     * 定义数字格式 是double类型的可以转化为 x.xx 的形式
     */
    private NumberFormat nf;
    private CommitOrderAdapter commitOrderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commit_order);

        nf = NumberFormat.getCurrencyInstance();
        nf.setMaximumFractionDigits(2);

        ButterKnife.inject(this);
        context = this;
        //接收intent数据
        orderBean = (OrderBean) getIntent().getSerializableExtra(GoodsInfoActivity.ORDER_INFO);
        //使用工具类判对象是否为空
        if (!JudgeUtil.isNullOrEmpty(orderBean)) {
            LogUtil.e("orderBean==" + orderBean.getGoodsItemList().get(0).getName());
            //得到传递过来的数据
//            Toast.makeText(this, "orderBean==" + orderBean.getGoodsItemList().get(0).getName(), Toast.LENGTH_SHORT).show();

            //从本地获得默认的用户信息数据
            getDataFromLocal();

        } else {
            LogUtil.e("没有得到传递过来的数据");
        }
    }

    private void getDataFromLocal() {
        if (UserStorage.getInstance().getUserList().size() == 0) {
            //没有用户信息
            //显示rlNoUser 隐藏rvOrderCommit
            rlNoUser.setVisibility(View.VISIBLE);
            rvOrderCommit.setVisibility(View.GONE);
            bottom.setVisibility(View.GONE);

        } else {
            UserBean defaultUser = UserStorage.getInstance().getUserList().get(0);
            //有数据
            rlNoUser.setVisibility(View.GONE);
            rvOrderCommit.setVisibility(View.VISIBLE);
            bottom.setVisibility(View.VISIBLE);

            tvShopname.setText("发送订单");

            //计算总价 + 配送费每件1元
            double countPrice = 0;
            double distribution = 0;
            for (int i = 0; i < orderBean.getGoodsItemList().size(); i++) {
                distribution += 1.00 * orderBean.getGoodsItemList().get(i).getCount();
                countPrice += orderBean.getGoodsItemList().get(i).getPrice() * orderBean.getGoodsItemList().get(i).getCount()
                        + 1.00 * orderBean.getGoodsItemList().get(i).getCount();
            }
            orderBean.setSumCount(countPrice);
            tvCost.setText(nf.format(countPrice));
            tvDistribution.setText("(含运费" +nf.format(distribution) +")");

            orderBean.setUser(defaultUser);
            //设置适配器
            commitOrderAdapter = new CommitOrderAdapter(this, orderBean);
            rvOrderCommit.setAdapter(commitOrderAdapter);

            GridLayoutManager manager = new GridLayoutManager(this, 1);
            //设置布局管理者
            rvOrderCommit.setLayoutManager(manager);  //一列
        }
    }

    @OnClick({R.id.iv_back, R.id.iv_share, R.id.btnDingdan, R.id.tvSubmit,})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
//                Toast.makeText(this, "返回", Toast.LENGTH_SHORT).show();
                finish();
                break;
            case R.id.iv_share:
                Toast.makeText(this, "分享", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btnDingdan:
//                Toast.makeText(this, "添加用户信息", Toast.LENGTH_SHORT).show();
                startLogInActivity();
                break;
            case R.id.tvSubmit:
//                Toast.makeText(this, "发送订单", Toast.LENGTH_SHORT).show();
                //判断是否有校园跑者
                if (!JudgeUtil.isNullOrEmpty(orderBean.getRunner())) {
                    //有校园跑者发送短信(订单)

                    //弹出对话框请求发送短信权限
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("提示");
                    builder.setMessage("接下来需要得到发送短信权限, 通过短信发送订单给校园跑者");
                    builder.setNegativeButton("禁止", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //不做处理
                        }
                    });
                    builder.setPositiveButton("允许", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //设置订单日期 和 订单默认状态
                            orderBean.setOrderState(ORDER_STATE_UNCOMMITED);
                            //获取当前时间
                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日   HH:mm:ss");
                            Date curDate = new Date(System.currentTimeMillis());
                            String date = formatter.format(curDate);
                            orderBean.setOrderDate(date);

                            destinationAddress = orderBean.getRunner().getRunnerPhoneNum();
                            //设置商品文本信息, 格式: 商品名(类型)X数量,
                            String goodsText = "";
                            List<GoodsItem> goodsItem = orderBean.getGoodsItemList();
                            for (int j = 0; j < goodsItem.size(); j++) {
                                if (j < goodsItem.size() - 1) {
                                    goodsText += goodsItem.get(j).getName() + "(" + goodsItem.get(j).getTypeName() + ") X"
                                            + goodsItem.get(j).getCount() + ", ";
                                } else {
                                    goodsText += goodsItem.get(j).getName() + "(" + goodsItem.get(j).getTypeName() + ") X"
                                            + goodsItem.get(j).getCount();
                                }
                            }
                            text = "【饭来了】" + "订单日期:" + orderBean.getOrderDate() + "  订单:{ 店铺名:" + goodsItem.get(0).getShopName() + "; " + goodsText + "}  总价:"
                                    + orderBean.getSumCount() + "  用户信息:" + "[姓名:" + orderBean.getUser().getUserName() + ", 地址:" + orderBean.getUser().getAddress()
                                    + ", 联系电话: " + orderBean.getUser().getUserPhoneNum() + " ]";

                            LogUtil.e("text==" + text);
                            //注册 和 处理短信发送状态
                            handleSendState();
                            handleReceiveState();

                            //动态地请求发送短信权限
                            if (getAndroidSDKVersion() >= 23) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    //申请SEND_SMS权限
                                    ActivityCompat.requestPermissions(CommitOrderActivity.this, new String[]{Manifest.permission.SEND_SMS},
                                            SEND_SMS_REQUEST_CODE);
                                } else {
                                    sendSMS(destinationAddress, text);
                                }
                            } else {
                                sendSMS(destinationAddress, text);
                            }
                        }
                    });
                    builder.create().show();


                } else {
                    //没有校园跑者 弹出对话框
                    popDialog("您还没有选择你的校园跑者!!!");
                }
                break;
        }
    }

    /**
     * 弹出对话框
     *
     * @param dialog
     */
    private void popDialog(String dialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage(dialog);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //不做处理
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //不做处理
            }
        });
        builder.create().show();
    }

    /**
     * 打开登录界面
     */
    private void startLogInActivity() {
        Intent intent = new Intent(this, LogInActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //从本地获得默认的用户信息数据
        getDataFromLocal();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == 2) {
            //得到校园跑者添加进OrderStorage
            RunnerBean runnerBean = (RunnerBean) data.getSerializableExtra(RUNNER_BEAN);
            orderBean.setRunner(runnerBean);
            //刷新适配器
            commitOrderAdapter.notifyDataSetChanged();
        }

    }

    private void handleReceiveState() {
        //处理返回的接收状态
        String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
        //创建接收返回的接收状态的Intent
        Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
        deliverPI = PendingIntent.getBroadcast(context, 0, deliverIntent, 0);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context _context, Intent _intent) {
                Toast.makeText(context, "收信人已经成功接收", Toast.LENGTH_SHORT).show();
            }
        }, new IntentFilter(DELIVERED_SMS_ACTION));
    }

    private void handleSendState() {
        //处理返回的发送状态
        String SENT_SMS_ACTION = "SENT_SMS_ACTION";
        Intent sentIntent = new Intent(SENT_SMS_ACTION);
        sentPI = PendingIntent.getBroadcast(context, 0, sentIntent, 0);
        //注册发送信息的广播接收者
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context _context, Intent _intent) {
                LogUtil.e("getResultCode()==" + getResultCode());
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "短信发送成功", Toast.LENGTH_SHORT).show();
                        if (orderBean != null) {
                            orderBean.setOrderState(ORDER_STATE_COMMITED);
                            OrderStorage.getInstance().addData(orderBean);
                            orderBean = null;
                        }
                        finish();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:    //普通错误
                        Toast.makeText(context, "普通错误", Toast.LENGTH_SHORT).show();
                        if (orderBean != null) {
                            orderBean.setOrderState(ORDER_STATE_UNCOMMITED);
                            OrderStorage.getInstance().addData(orderBean);
                            orderBean = null;
                        }
                        finish();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:         //无线广播被明确地关闭
                        Toast.makeText(context, "无线广播被明确地关闭", Toast.LENGTH_SHORT).show();
                        if (orderBean != null) {
                            orderBean.setOrderState(ORDER_STATE_UNCOMMITED);
                            OrderStorage.getInstance().addData(orderBean);
                            orderBean = null;
                        }
                        finish();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:          //没有提供pdu
                        Toast.makeText(context, "没有提供pdu", Toast.LENGTH_SHORT).show();
                        if (orderBean != null) {
                            orderBean.setOrderState(ORDER_STATE_UNCOMMITED);
                            OrderStorage.getInstance().addData(orderBean);
                            orderBean = null;
                        }
                        finish();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:         //服务当前不可用
                        Toast.makeText(context, "服务当前不可用", Toast.LENGTH_SHORT).show();
                        if (orderBean != null) {
                            orderBean.setOrderState(ORDER_STATE_UNCOMMITED);
                            OrderStorage.getInstance().addData(orderBean);
                            orderBean = null;
                        }
                        finish();
                        break;
                }
            }
        }, new IntentFilter(SENT_SMS_ACTION));

    }


    public void sendSMS(String phoneNumber, String message) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            //通过反射得到TelephonyManager对象
            clz = (Class<TelephonyManager>) mTelephonyManager.getClass();

            //判断 sim1 卡的状态 是否为准备状态
            //默认使用SIM1 卡发送
            if (getSimState(0) == TelephonyManager.SIM_STATE_READY) {
                selectSimAndSendSms(0, phoneNumber, message);

            } else if (getSimState(1) == TelephonyManager.SIM_STATE_READY) {
                selectSimAndSendSms(1, phoneNumber, message);
            }
        }
    }


    private final static int SEND_SMS_REQUEST_CODE = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Toast.makeText(CommitOrderActivity.this, "onRequestPermissionsResult被调用！", Toast.LENGTH_SHORT).show();

        doNext(requestCode, grantResults);

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    private void doNext(int requestCode, int[] grantResults) {
        if (requestCode == SEND_SMS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted准许
                Toast.makeText(this, "已获得授权！", Toast.LENGTH_SHORT).show();
                sendSMS(destinationAddress, text);

            } else {
                // Permission Denied拒绝
                Toast.makeText(this, "未获得授权！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static int getAndroidSDKVersion() {
        int version = 0;
        try {
            version = Build.VERSION.SDK_INT;
        } catch (NumberFormatException e) {
//            Log.E("errTag", e.toString());
            Log.e("getAndroidSDKVersion()", "不能得到版本AndroidSDKVersion", null);
        }
        return version;
    }


//    @Override
//    protected void onPause() {
//        SMSMethod.getInstance(this).unregisterReceiver();
//        super.onPause();
//    }

    /**
     * 获取sim的状态，参数对应sim卡的序号，分别为0（卡1）和1（卡2）
     *
     * @param slotID 参数对应sim卡的序号，分别为0（卡1）和1（卡2）
     * @return
     */
    public int getSimState(int slotID) {
        int status = 0;
        try {
            Method mtd = clz.getMethod("getSimState", int.class);
            mtd.setAccessible(true);
            status = (Integer) mtd.invoke(mTelephonyManager, slotID);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return status;
    }

    /**
     * 选择SIM卡 并发送短信
     *
     * @param which
     */
    private void selectSimAndSendSms(final int which, String phoneNumber, String message) {
        SubscriptionInfo sInfo = null;
        //获取短信管理器
        SmsManager manager = null;
        SubscriptionManager sManager = (SubscriptionManager) context
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        List<SubscriptionInfo> list = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            list = sManager.getActiveSubscriptionInfoList();
            if (list != null) {
                if (list.size() == 2) {// double card
                    sInfo = list.get(which);
                } else {//single card
                    sInfo = list.get(0);
                }
            }
            if (sInfo != null) {

                int subId = 0;
                subId = sInfo.getSubscriptionId();

                manager = SmsManager.getSmsManagerForSubscriptionId(subId);
                //发送短信
                smsManager2sendSms(phoneNumber, message, manager);

            }

        } else {
            manager = SmsManager.getDefault();
            //发送短信
            smsManager2sendSms(phoneNumber, message, manager);
        }
    }

    private void smsManager2sendSms(String phoneNumber, String message, SmsManager manager) {
        //拆分短信内容（手机短信长度限制）,貌似长度限制为140个字符,就是
        //只能发送70个汉字,多了要拆分成多条短信发送
        //第四五个参数,如果没有需要监听发送状态与接收状态的话可以写null
        ArrayList<String> divideContents = manager.divideMessage(message);
        if (divideContents.size() == 0) {
            Toast.makeText(context, "短信不能为空", Toast.LENGTH_SHORT).show();
        }
        if (message.length() > 70) {
            ArrayList<String> msgs = manager.divideMessage(message);
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            for (int i = 0; i < msgs.size(); i++) {
                sentIntents.add(sentPI);
            }
            manager.sendMultipartTextMessage(phoneNumber, null, msgs, sentIntents, null);
        } else {
            manager.sendTextMessage(phoneNumber, null, message, sentPI, deliverPI);
        }
    }
}
