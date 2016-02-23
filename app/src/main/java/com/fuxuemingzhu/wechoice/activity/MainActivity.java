package com.fuxuemingzhu.wechoice.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cjj.MaterialRefreshLayout;
import com.cjj.MaterialRefreshListener;
import com.fuxuemingzhu.wechoice.R;
import com.fuxuemingzhu.wechoice.adapter.ChoiceAdapter;
import com.fuxuemingzhu.wechoice.app.AppData;
import com.fuxuemingzhu.wechoice.app.BaseActivity;
import com.fuxuemingzhu.wechoice.entity.Choice;
import com.fuxuemingzhu.wechoice.utils.Logcat;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import okhttp3.Call;

public class MainActivity extends BaseActivity {

    private String url = "http://v.juhe.cn/weixin/query";
    private static String APPKEY = "d7bbe8531dc5a69516334aaafd698d98";
    private List<Choice> listChoice = new ArrayList<>();

    private RelativeLayout ll_load_more;

    private ListView lv_choices;
    private ChoiceAdapter listAdapter;

    private MaterialRefreshLayout materialRefreshLayout;
    private int refreshPages = 1;
    private int morePages = 1;
    final int QUEUE_SIZE = 10;//队列大小
    //手写队列用来存储已经加载过的文章页数
    Queue<Integer> refreshQueue = new LinkedList<>();
    Queue<Integer> moreQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initEvents();
        displayDefaultContent();
    }

    @Override
    protected void initViews() {
        lv_choices = (ListView) findViewById(R.id.lv_main_choices);
        materialRefreshLayout = (MaterialRefreshLayout) findViewById(R.id.refresh_main);
        ll_load_more = (RelativeLayout) ((LayoutInflater) this.getSystemService(Context
                .LAYOUT_INFLATER_SERVICE)).inflate(R
                .layout.foot_more, null, false);
        lv_choices.addFooterView(ll_load_more);

    }

    @Override
    protected void initEvents() {
        materialRefreshLayout.setLoadMore(true);
        materialRefreshLayout.finishRefreshLoadMore();
        materialRefreshLayout.setMaterialRefreshListener(new MaterialRefreshListener() {
            @Override
            public void onRefresh(final MaterialRefreshLayout materialRefreshLayout) {
                while (refreshQueue.contains(refreshPages)) {
                    refreshPages = (int) Math.ceil(Math.random() * 25);
                }
                getFreshContent();
            }

            @Override
            public void onRefreshLoadMore(MaterialRefreshLayout materialRefreshLayout) {
                super.onRefreshLoadMore(materialRefreshLayout);

                while (moreQueue.contains(morePages)) {
                    morePages = (int) Math.ceil(Math.random() * 25);
                }
                getMoreContent();
            }

            @Override
            public void onfinish() {
                super.onfinish();
            }
        });
        materialRefreshLayout.autoRefresh();//drop-down refresh automatically
    }

    private void getFreshContent() {
        if (refreshQueue.size() == QUEUE_SIZE) {
            refreshQueue.poll();
        }
        refreshQueue.offer(refreshPages);
        moreQueue.clear();
        moreQueue.offer(refreshPages);
        Logcat.i("refreshQueue", refreshQueue.toString());
        OkHttpUtils
                .get()
                .url(url)
                .addParams("pno", Integer.toString(refreshPages))
                .addParams("key", APPKEY)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        materialRefreshLayout.finishRefresh();
                        MainActivity.this.showCustomToast("网络异常，请稍后重试");
                    }

                    @Override
                    public void onResponse(String response) {
                        materialRefreshLayout.finishRefresh();
                        if (response == null) {
                            MainActivity.this.showCustomToast("网络异常，请稍后重试");
                            return;
                        }
                        AppData.getInstance(MainActivity.this).setDefaultContent(response);
                        displayContent(response);
                    }
                });
    }

    private void displayContent(String response) {
        JSONObject responseJson = JSON.parseObject(response);
        JSONObject result = responseJson.getJSONObject("result");
        JSONArray jsonList = result.getJSONArray("list");
        listChoice.clear();
        for (int i = 0; i < jsonList.size(); i++) {
            JSONObject choiceJson = jsonList.getJSONObject(i);
            Choice choice = JSON.parseObject(choiceJson.toJSONString(), Choice.class);
            listChoice.add(choice);
        }
        String listString = "";
        for (int i = 0; i < listChoice.size(); i++) {
            listString += listChoice.get(i).toString();
        }
        listAdapter = new ChoiceAdapter(MainActivity.this, listChoice);
        lv_choices.setAdapter(listAdapter);
        lv_choices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Bundle data = new Bundle();
                data.putString("url", listChoice.get(i).getUrl());
                // 创建一个Intent
                Intent intent = new Intent(MainActivity.this,
                        ContentActivity.class);
                intent.putExtras(data);
                // 启动intent对应的Activity
                startActivity(intent);
            }
        });
        Logcat.i("response", listString);
    }

    private void displayDefaultContent() {
        String defaultResponse = AppData.getInstance(this).getDefaultContent();
        if (defaultResponse != null && !defaultResponse
                .equals("")) {
            displayContent(defaultResponse);
        }
    }

    private void getMoreContent() {
        if (moreQueue.size() == 25) {
            moreQueue.clear();
            moreQueue.offer(refreshPages);
        }
        moreQueue.offer(morePages);
        Logcat.i("moreQueue", moreQueue.toString());
        OkHttpUtils
                .get()
                .url(url)
                .addParams("pno", Integer.toString(morePages))
                .addParams("key", APPKEY)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        materialRefreshLayout.finishRefreshLoadMore();
                        MainActivity.this.showCustomToast("网络异常，请稍后重试");
                    }

                    @Override
                    public void onResponse(String response) {
                        materialRefreshLayout.finishRefreshLoadMore();
                        if (response == null) {
                            MainActivity.this.showCustomToast("网络异常，请稍后重试");
                            return;
                        }
                        JSONObject responseJson = JSON.parseObject(response);
                        JSONObject result = responseJson.getJSONObject("result");
                        JSONArray jsonList = result.getJSONArray("list");
                        for (int i = 0; i < jsonList.size(); i++) {
                            JSONObject choiceJson = jsonList.getJSONObject(i);
                            Choice choice = JSON.parseObject(choiceJson.toJSONString(), Choice.class);
                            listChoice.add(choice);
                        }
                        String listString = "";
                        for (int i = 0; i < listChoice.size(); i++) {
                            listString += listChoice.get(i).toString();
                        }
                        listAdapter.notifyDataSetChanged();
                        Logcat.i("response", listString);
                    }
                });
    }

}
