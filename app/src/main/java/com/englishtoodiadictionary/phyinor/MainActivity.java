package com.englishtoodiadictionary.phyinor;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    InterstitialAd mInterstitialAd;

    SearchView search;

    static DatabaseHelper myDbHelper;
    static boolean databaseOpened=false;

    SimpleCursorAdapter suggestionAdapter;

    ArrayList<History> historyList;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    RecyclerView.Adapter historyAdapter;

    RelativeLayout emptyHistory;
    Cursor cursorHistory;

    boolean doubleBackToExitPressedOnce = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });



        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);



        InterstitialAd.load(this, getString(R.string.inter_ad), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.e("Error",loadAdError.toString());
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                mInterstitialAd = interstitialAd;
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent();
                        mInterstitialAd = null;
                    }
                });
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mInterstitialAd!=null)
                    mInterstitialAd.show(MainActivity.this);
                else
                    Log.e("AdPending","Ad is not ready yet!");

            }
        },10000);



        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        search =  (SearchView) findViewById(R.id.search_view);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                search.setIconified(false);

            }
        });


        myDbHelper = new DatabaseHelper(this);

        if(myDbHelper.checkDataBase())
        {
            openDatabase();

        }
        else
        {
            LoadDatabaseAsync task = new LoadDatabaseAsync(MainActivity.this);
            task.execute();
        }



        // setup SimpleCursorAdapter

        final String[] from = new String[] {"en_word"};
        final int[] to = new int[] {R.id.suggestion_text};

        suggestionAdapter = new SimpleCursorAdapter(MainActivity.this,
                R.layout.suggestion_row, null, from, to, 0){
            @Override
            public void changeCursor(Cursor cursor) {
                super.swapCursor(cursor);
            }

        };

        search.setSuggestionsAdapter(suggestionAdapter);


        search.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionClick(int position) {

                // Add clicked text to search box
                CursorAdapter ca = search.getSuggestionsAdapter();
                Cursor cursor = ca.getCursor();
                cursor.moveToPosition(position);
                @SuppressLint("Range") String clicked_word =  cursor.getString(cursor.getColumnIndex("en_word"));
                search.setQuery(clicked_word,false);

                //search.setQuery("",false);

                search.clearFocus();
                search.setFocusable(false);

                Intent intent = new Intent(MainActivity.this, WordMeaningActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("en_word",clicked_word);
                intent.putExtras(bundle);
                startActivity(intent);

                return true;
            }

            @Override
            public boolean onSuggestionSelect(int position) {
                // Your code here
                return true;
            }
        });


        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query)
            {

                String text =  search.getQuery().toString();

                Pattern p = Pattern.compile("[A-Za-z \\-.]{1,25}");
                Matcher m = p.matcher(text);

                if(m.matches())
                {
                    Cursor c = myDbHelper.getMeaning(text);

                    if(c.getCount()==0)
                    {
                        showAlertDialog();
                    }

                    else
                    {
                        //search.setQuery("",false);
                        search.clearFocus();
                        search.setFocusable(false);

                        Intent intent = new Intent(MainActivity.this, WordMeaningActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("en_word",text);
                        intent.putExtras(bundle);
                        startActivity(intent);

                    }

                }

                else
                {
                    showAlertDialog();
                }



                return false;
            }


            @Override
            public boolean onQueryTextChange(final String s) {
                search.setIconifiedByDefault(false); //Give Suggestion list margins

                Pattern p = Pattern.compile("[A-Za-z \\-.]{1,25}");
                Matcher m = p.matcher(s);

                if(m.matches()) {
                    final Cursor cursorSuggestion=myDbHelper.getSuggestions(s);

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            suggestionAdapter.changeCursor(cursorSuggestion);
                        }
                    }, 100);
                }


                return false;
            }

        });


        emptyHistory = (RelativeLayout) findViewById(R.id.empty_history);

        //recycler View
        recyclerView = (RecyclerView)findViewById(R.id.recycler_view_history);
        layoutManager = new LinearLayoutManager(MainActivity.this);

        recyclerView.setLayoutManager(layoutManager);

        fetch_history();

    }


    protected static void openDatabase()
    {
        try {
            myDbHelper.openDataBase();
            databaseOpened=true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("Range")
    private void fetch_history()
    {
        historyList=new ArrayList<>();
        historyAdapter = new RecyclerViewAdapterHistory(this,historyList);
        recyclerView.setAdapter(historyAdapter);

        History h;

        if(databaseOpened)
        {
            cursorHistory=myDbHelper.getHistory();
            if (cursorHistory.moveToFirst()) {
                do {
                    h= new History(cursorHistory.getString(cursorHistory.getColumnIndex("word")),cursorHistory.getString(cursorHistory.getColumnIndex("en_definition")));
                    historyList.add(h);
                }
                while (cursorHistory.moveToNext());
            }

            historyAdapter.notifyDataSetChanged();
        }


        if (historyAdapter.getItemCount() == 0)
        {
            emptyHistory.setVisibility(View.VISIBLE);
        }
        else
        {
            emptyHistory.setVisibility(View.GONE);
        }
    }

    private void showAlertDialog()
    {
        search.setQuery("",false);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.MyDialogTheme);
        builder.setTitle("Word Not Found");
        builder.setMessage("Please search again");

        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // positive button logic
                    }
                });

        String negativeText = getString(android.R.string.cancel);
        builder.setNegativeButton(negativeText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        search.clearFocus();
                    }
                });

        AlertDialog dialog = builder.create();
        // display dialog
        dialog.show();

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu_main; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_exit) {
            System.exit(0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetch_history();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }







}
