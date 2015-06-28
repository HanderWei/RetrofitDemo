package me.chen_wei.retrofitdemo;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import me.chen_wei.retrofitdemo.api.DoubanAPI;
import me.chen_wei.retrofitdemo.model.Movie;
import me.chen_wei.retrofitdemo.model.Top250;
import me.chen_wei.retrofitdemo.view.EndlessScrollListener;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    Handler mHandler;
    Context mContext;

    @InjectView(R.id.lv_movies)
    ListView mMoviesLv;

    @InjectView(R.id.swipe_container)
    SwipeRefreshLayout refreshLayout;

    MoviesAdapter mAdapter;

    RestAdapter restAdapter;
    DoubanAPI   api;


    List<Movie> movies = new ArrayList<>();

    private static final String MOVIE_API = "https://api.douban.com";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mHandler = new Handler();
        mContext = this;
        init();
    }

    private void init() {
        refreshLayout.setEnabled(false);
        refreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                                              android.R.color.holo_green_light,
                                              android.R.color.holo_orange_light,
                                              android.R.color.holo_red_light);
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);
            }
        });
        mAdapter = new MoviesAdapter();
        View view = LayoutInflater.from(mContext)
                                  .inflate(R.layout.empty_view, null);
        addContentView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                        ViewGroup.LayoutParams.MATCH_PARENT));
        mMoviesLv.setEmptyView(view);
        mMoviesLv.setAdapter(mAdapter);
        restAdapter = new RestAdapter.Builder().setLogLevel(RestAdapter.LogLevel.FULL)
                                               .setEndpoint(MOVIE_API)
                                               .build();
        api = restAdapter.create(DoubanAPI.class);
        getResult(0, 20);

        mMoviesLv.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                if (totalItemsCount == 0) {
                    getResult(0, 20);
                } else {
                    getResult(totalItemsCount, 20);
                }
            }
        });
    }

    private void getResult(int start, int count) {
        Log.d(TAG, "get result ing...");
        api.getResult(start, count, new Callback<Top250>() {
            @Override
            public void success(Top250 top250, Response response) {
                List<Top250.SubjectsEntity> subjectsEntities = top250.getSubjects();
                for (Top250.SubjectsEntity subject : subjectsEntities) {
                    Movie movie = new Movie();
                    movie.setPoster(subject.getImages()
                                           .getSmall());
                    movie.setRating((float) subject.getRating()
                                                   .getAverage());
                    movie.setTitle(subject.getTitle());
                    List<String> genres = subject.getGenres();
                    String genresStr = "";
                    for (String s : genres) {
                        genresStr += s + " ";
                    }
                    movie.setGenres(genresStr);
                    movies.add(movie);
                }
                mAdapter.notifyDataSetChanged();

                if (refreshLayout.isRefreshing()) {
                    refreshLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            refreshLayout.setRefreshing(false);
                        }
                    });
                }
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.reset(this);
    }

    class MoviesAdapter extends BaseAdapter {

        public static final int VIEW_TYPE_LOADING  = 0;
        public static final int VIEW_TYPE_ACTIVITY = 1;

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position >= movies.size()) ? VIEW_TYPE_LOADING : VIEW_TYPE_ACTIVITY;
        }

        @Override
        public int getCount() {
            return null == movies || movies.size() == 0 ? 0 : movies.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            return (getItemViewType(position) == VIEW_TYPE_ACTIVITY) ? movies.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return (getItemViewType(position) == VIEW_TYPE_ACTIVITY) ? position : -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (getItemViewType(position) == VIEW_TYPE_LOADING) {
                return getFooterView(position, convertView, parent);
            }
            ViewHolder vh;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext)
                                            .inflate(R.layout.item_movie, parent, false);
                vh = new ViewHolder();
                vh.poster = (ImageView) convertView.findViewById(R.id.poster);
                vh.title = (TextView) convertView.findViewById(R.id.title);
                vh.genres = (TextView) convertView.findViewById(R.id.genres);
                vh.rating = (TextView) convertView.findViewById(R.id.rating);
                vh.number = (TextView) convertView.findViewById(R.id.number);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            final Movie movie = movies.get(position);

            Log.d(TAG, movie.toString());
            Picasso.with(mContext)
                   .load(movie.getPoster())
                   .into(vh.poster);
            vh.title.setText(movie.getTitle());
            vh.genres.setText(movie.getGenres());
            vh.rating.setText(String.format(getString(R.string.rating), movie.getRating()));
            vh.number.setText((position + 1) + "");
            return convertView;
        }

        private View getFooterView(int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(mContext)
                                      .inflate(R.layout.loading_view, null);
            return view;
        }

    }

    class ViewHolder {
        ImageView poster;
        TextView  title;
        TextView  genres;
        TextView  rating;
        TextView  number;
    }
}
