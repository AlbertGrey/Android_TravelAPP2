package tw.org.iii.travelapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FavoriteActivity extends AppCompatActivity {

    private ListView favorite_list;
    private FavoriteActivity.MyAdapter myAdapter;
    private float screenWidth, screenHeight, newHeight;
    private ImageView item_img;
    private String stitle, img_url, backgroundColor;
    private ArrayList<DataStation> dataList, dataList2, dataList3;
    private RequestQueue queue;
    private String url = "http://36.235.39.18:8080/fsit04/User_favorite";
    private String userId = "1";
    private String restUrl = "http://36.235.39.18:8080/fsit04/restaruant";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        setTitle("我的最愛");
        queue= Volley.newRequestQueue(this);
        getIntentData(); //取得Intent資料
        getFavorite(HomePageActivity.userID); //取得我的最愛

        dataList = new ArrayList<>();
        dataList2 = new ArrayList<>();
        dataList3 = new ArrayList<>();
//        getRest();
        favorite_list = findViewById(R.id.favorite_list);
//        favorite_list.setBackgroundColor(Color.parseColor("#ffe4e1"));
        getScreenSize();
        MyAsyncTask myAsyncTask = new MyAsyncTask();
        myAsyncTask.execute(
                HomePageActivity.urlIP +
                        "/fsit04/User_favorite?user_id=" + HomePageActivity.userID);
//        init();
    }
    //取得螢幕大小
    private void getScreenSize(){
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        newHeight = screenWidth / 16 * 9;
    }

    private void init(){
        myAdapter = new MyAdapter(FavoriteActivity.this);
        favorite_list.setAdapter(myAdapter);
        favorite_list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        favorite_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            //觸發按鈕事件
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                intentToDetail(position);
           }
        });
    }
    //取得Intent資料
    private void getIntentData(){
        Intent intent = getIntent();
    }
    //intent至detail頁面
    private void intentToDetail(int position){
        String img_url = dataList.get(position).getPhoto_url().get(0);
        String stitle = dataList.get(position).getStitle();
        String xbody = dataList.get(position).getXbody();
        String total_id = dataList.get(position).getTotal_id();
        double lat = dataList.get(position).getLat();
        double lng = dataList.get(position).getLng();
        String address = dataList.get(position).getAddress();
        String meme_time = dataList.get(position).getMEMO_TIME();
        ArrayList<String> photos = dataList.get(position).getPhoto_url();

        Intent intent = new Intent(FavoriteActivity.this, DetailActivity.class);
        intent.putExtra("stitle", total_id + "." + stitle);
        intent.putExtra("xbody", xbody);
        intent.putExtra("img_url", img_url);
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);
        intent.putExtra("address", address);
        intent.putExtra("memo_time", meme_time);
        intent.putStringArrayListExtra("photos", photos);
        startActivity(intent);
    }

    @Override
    public void finish() {
        super.finish();
    }
    //BaseAdapter for ListView
    private class MyAdapter extends BaseAdapter {

        private Context myContext;
        private LayoutInflater inflater;

        public MyAdapter(Context context){
            this.myContext = context;
            inflater = LayoutInflater.from(this.myContext);
        }
        //取得list的數量
        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            //設置ListView的layout,沒有根目錄root值為null
            if(convertView == null) {
                convertView = inflater.inflate(R.layout.layout_favorite_listview, null);
            }
            //取得ListView layout的每個view
            item_img = convertView.findViewById(R.id.favorite_item_img);
            TextView item_title = convertView.findViewById(R.id.favorite_item_title);
            ImageView removeTv = convertView.findViewById(R.id.favorite_remove);
            //從我的最愛移除
            removeTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String total_Id = dataList.get(position).getTotal_id();
                    deleteFavorite(userId, total_Id);
                    dataList.remove(position);
                    myAdapter.notifyDataSetChanged();
                }
            });
            //設定title
            String stitle = dataList.get(position).getStitle();
            String total_id = dataList.get(position).getTotal_id();
            item_title.setText(total_id + "." + stitle);
            //設定photo
            GlideApp
                    .with(FavoriteActivity.this)
                    .load(dataList.get(position).getPhoto_url().get(0))
                    .override((int)screenWidth-100, (int)newHeight-100)
//                        .centerCrop()
//                        .placeholder(R.drawable.loading)
                    .into(item_img);
            //回傳convertView
            return convertView;
        }
    }
    //解析JSON字串
    private void parseJSON(String s){
        try {
            JSONArray array = new JSONArray(s);
            for (int i=0; i<array.length(); i++){
                ArrayList<String> photo_url = new ArrayList<>();
                JSONObject obj = array.getJSONObject(i);
                JSONArray imgs = obj.getJSONArray("Img");
                for(int y = 0; y < imgs.length(); y++){
                    String imgUrl = imgs.getJSONObject(y).getString("url");
                    photo_url.add(imgUrl);
                }
                String total_id = obj.getString("total_id");
                String cat2 = obj.getString("CAT2");
                String xbody = obj.getString("xbody");
                String address = obj.getString("address");
                stitle = obj.getString("name");
                String memo_time = obj.getString("MEMO_TIME");
                double lng = obj.getDouble("lng");
                double lat = obj.getDouble("lat");

                DataStation data = new DataStation(
                        total_id, stitle, photo_url, xbody, lat, lng, address, memo_time);
                dataList.add(data);
            }

        } catch (JSONException e) {
            Log.v("brad", e.toString());
        }
    }

    private class MyAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String data = getData(strings[0]);
            parseJSON(data);
            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            init();
        }
        //從URL獲取JSON字串
        private String getData(String url){
            StringBuffer sb = new StringBuffer();
            try {
                URL newURL = new URL(url);
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(newURL.openStream()));
                String line;
                while( (line = br.readLine()) != null){
                    sb.append(line);
                }
            } catch (IOException e) {
                Log.v("brad", e.toString());
            }
            return sb.toString();
        }
    }
    /** 加入我的最愛
     *
     * @param user_id     用戶id
     * @param total_id   地點的id
     */
    private void addFavorite(String user_id,String total_id){
        final String p1 =user_id;
        final String p2=total_id;

        String url =
                HomePageActivity.urlIP + "/fsit04/User_favorite?user=" + HomePageActivity.userID;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, null){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> m1 =new HashMap<>();
                m1.put("user_id",p1);
                m1.put("total_id", p2);
                return m1;
            }
        };
        queue.add(stringRequest);
    }
    /** 刪除我的最愛
     *
     * @param user_id     用戶id
     * @param total_id   地點的id
     */
    private void deleteFavorite(String user_id,String total_id){
        String url =
                HomePageActivity.urlIP + "/fsit04/User_favorite?user=" + HomePageActivity.userID;
        final String p1 = user_id;
        final String p2 = total_id;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                }, null){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> m1 =new HashMap<>();
                m1.put("_method","DELETE");
                m1.put("user_id",p1);
                m1.put("total_id", p2);

                return m1;
            }
        };
        queue.add(stringRequest);
    }
    /** 取得我的最愛
     *
     * @param user_id 用戶id
     */
    private void getFavorite(String user_id){
        final String p1 = user_id;
        String getFavoriteUrl =
                HomePageActivity.urlIP + "/fsit04/User_favorite?user_id=" + HomePageActivity.userID;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, getFavoriteUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseGetFavorite(response);
                    }
                }, null);
        queue.add(stringRequest);
    }
    /**  用戶我的最愛parseJSON
     *
     * @param response
     * total_id = 地點ID, name = 地點名稱, type = 地點類型, CAT2 = 分類
     * MEMO_TIME = 營業時間, address = 地址, xbody = 簡介
     * lat = 緯度, lng = 經度
     * description = 照片的描述, url = 照片的url
     */
    private void parseGetFavorite(String response){
        try {
            JSONArray array1 = new JSONArray(response);
            for(int i= 0;i<array1.length();i++) {
                ArrayList<String> photo_url = new ArrayList<>();
                JSONObject ob1 =array1.getJSONObject(i);
                String total_id = ob1.getString("total_id");
                String name = ob1.getString("name");
                String type= ob1.getString("type");
                String CAT2 = ob1.getString("CAT2");
                String MEMO_TIME = ob1.getString("MEMO_TIME");
                String address = ob1.getString("address");
                String xbody = ob1.getString("xbody");
                double lat = ob1.getDouble("lat");
                double lng = ob1.getDouble("lng");
                JSONArray imgs =ob1.getJSONArray("Img");
                for(int y = 0; y < imgs.length(); y++){
                    String imgUrl = imgs.getJSONObject(y).getString("url");
                    photo_url.add(imgUrl);
                }
                DataStation data2 = new DataStation(total_id, name, type, CAT2, MEMO_TIME,
                        address, xbody, lat, lng, photo_url);
                dataList2.add(data2);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /**
     * 取得餐廳資訊
     */
    private void getRest(){
        String url =
                HomePageActivity.urlIP + "/fsit04/restaruant";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseRest(response);
                    }
                }, null);

        queue.add(stringRequest);
    }
    /**
     *  解析餐廳資訊
     * @param response
     * total_id = 地點ID, stitle = 地點名稱, type = 地點類型, CAT2 = 分類
     * MEMO_TIME = 營業時間, address = 地址, xbody = 簡介
     * lat = 緯度, lnt = 經度, imgUrl = 照片的URL
     */
    private void parseRest(String response){
        try {
            JSONArray array1 = new JSONArray(response);
            for(int i= 0;i<array1.length();i++) {
                ArrayList<String> photo_url = new ArrayList<>();
                JSONObject ob1 =array1.getJSONObject(i);
                String total_id = ob1.getString("total_id");
                String name = ob1.getString("stitle");
                String type= ob1.getString("type");
                String CAT2 = ob1.getString("CAT2");
                String MEMO_TIME = ob1.getString("MEMO_TIME");
                String address = ob1.getString("address");
                String xbody = ob1.getString("xbody");
                double lat = ob1.getDouble("lat");
                double lng = ob1.getDouble("lng");

                JSONArray imgs =ob1.getJSONArray("imgs");
                for(int y= 0;y<imgs.length();y++){
                    String imgUrl = imgs.getJSONObject(y).getString("url");
                    photo_url.add(imgUrl);
                }
                DataStation data3 = new DataStation(total_id, name, type, CAT2, MEMO_TIME,
                        address, xbody, lat, lng, photo_url);
                dataList3.add(data3);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
