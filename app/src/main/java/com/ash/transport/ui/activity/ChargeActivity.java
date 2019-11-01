package com.ash.transport.ui.activity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.ash.transport.R;
import com.ash.transport.dao.RecordDao;
import com.ash.transport.factory.DialogFactory;
import com.ash.transport.factory.ToastFactory;
import com.ash.transport.model.RecordInfo;
import com.ash.transport.request.BaseRequest;
import com.ash.transport.request.SetBalanceRequest;
import com.ash.transport.ui.widget.EditDialog;
import com.ash.transport.utils.NetUtil;
import com.ash.transport.utils.RegUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/*----------------------------------------------*
 * @package:   com.ash.transport.ui.activity
 * @fileName:  ChargeActivity.java
 * @describe:  车辆余额充值活动类
 *----------------------------------------------*
 * @author:    ash
 * @email:     Glaxyinfinite@outlook.com
 * @date:      on 2019-05-22 21:56
 * @继承关系:   ChargeActivity ← BaseActivity ← [AppCompatActivity]
 *----------------------------------------------*/
public class ChargeActivity extends BaseActivity implements View.OnClickListener {
    private int carId;              // 欲充值的车辆编号

    private TextView tvCarId;       // 车辆编号 文本框
    private TextView btnMoney20;    // 充值金额20元 按钮
    private TextView btnMoney50;    // 充值金额50元 按钮
    private TextView btnMoney100;   // 充值金额100元 按钮
    private TextView btnCustom;     // 自定义充值金额 按钮
    private RecordDao recordDao;    // 充值记录 数据库访问器


    // 重写父类抽象方法 设置布局ID
    @Override
    protected int setLayoutId() {
        return R.layout.activity_charge;
    }

    // 重写父类抽象方法 初始化视图
    @Override
    protected void initView() {
        // 设置顶部原生标题栏标题 已在清单文件中设置
        // ChargeActivity.this.setTitle("车辆余额充值");

        // 显示顶部原生标题栏返回按钮
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        tvCarId = findViewById(R.id.iv_car_id);
        btnMoney20 = findViewById(R.id.btn_money_20);
        btnMoney50 = findViewById(R.id.btn_money_50);
        btnMoney100 = findViewById(R.id.btn_money_100);
        btnCustom = findViewById(R.id.btn_money_custom);
    }

    // 重写父类抽象方法 初始化数据
    @SuppressLint("SetTextI18n")
    @Override
    protected void initData() {

        // 检查网络状态
        if (!NetUtil.isNetworkOK(ChargeActivity.this)) {
            ToastFactory.show(ChargeActivity.this, "网络不可用！");
        }

        carId = getIntent().getIntExtra("com.ash.transport.ui.fragment.CarFragment.carId", 0);
        tvCarId.setText(carId + "");
        btnMoney20.setOnClickListener(this);
        btnMoney50.setOnClickListener(this);
        btnMoney100.setOnClickListener(this);
        btnCustom.setOnClickListener(this);
        recordDao = new RecordDao(ChargeActivity.this);
    }

    // 重写顶部标题栏按钮监听事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // 返回按钮被单击事件
            case android.R.id.home:
                // 销毁当前Activity
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 重写按钮监听接口事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_money_20:         // 20元
                openDialog(20);
                break;

            case R.id.btn_money_50:         // 50元
                openDialog(50);
                break;

            case R.id.btn_money_100:        // 100元
                openDialog(100);
                break;

            case R.id.btn_money_custom:     // 自定义金额

                EditDialog.show(ChargeActivity.this, "自定义充值金额", new EditDialog.OnListener() {
                    @Override
                    public void onAfter(String input) {

                        // 如果输入金额问3位数以内
                        if (RegUtil.isInteger3(input)) {
                            // 调用充值金额方法
                            charge(Integer.valueOf(input));
                        } else {
                            // 使用Toast工厂类显示消息框
                            ToastFactory.show(ChargeActivity.this, "单次充值金额不能超过999元");
                        }

                    }
                });

                break;
        }
    }

    // 定义 打开对话框 方法
    private void openDialog(final int money) {
        // 使用对话框工厂类生成对话框并显示
        DialogFactory.showDialog(ChargeActivity.this,
                "充值确认",
                "您确定要给" + carId + "号小车充值 " + money + " 元吗？",
                new DialogFactory.OnListener() {

                    @Override
                    public void onAfter() {
                        charge(money);
                    }
                });
    }

    // 定义 充值 方法
    private void charge(final int money) {
        // 使用余额充值请求类
        SetBalanceRequest setBalanceRequest = new SetBalanceRequest(ChargeActivity.this);
        setBalanceRequest.setCarId(carId);
        setBalanceRequest.setMoney(money);
        setBalanceRequest.connec(new BaseRequest.OnGetDataListener() {
            @Override
            public void onReturn(Object data) {
                if ("ok".equals(data.toString())) {

                    // 初始化充值记录信息类
                    RecordInfo record = new RecordInfo();
                    // 记录充值车辆
                    record.setCarId(carId);
                    // 记录充值金额
                    record.setMoney(money);

                    // 使用shared获得充值者并记录
                    SharedPreferences shared = getSharedPreferences("userInfo", MODE_PRIVATE);
                    record.setUserName(shared.getString("name", "name"));

                    // 获取当前时间
                    Date currentTime = new Date();
                    // 设置时间格式
                    SimpleDateFormat formatter = new SimpleDateFormat("MM-dd hh:mm");
                    // 记录充值时间
                    record.setChargeDate(formatter.format(currentTime));

                    // 使用充值记录数据库访问器
                    // 增加一条充值记录数据到数据库表
                    recordDao.insert(record);

                    ToastFactory.show(ChargeActivity.this, "充值成功", true);
                } else {
                    ToastFactory.show(ChargeActivity.this, "充值失败", true);
                }

                // 充值结束后销毁当前页面
                finish();
            }
        });
    }
}
