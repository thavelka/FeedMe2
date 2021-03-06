package com.thavelka.feedme.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.thavelka.feedme.R;
import com.thavelka.feedme.models.Listing;
import com.thavelka.feedme.ui.auth.AuthActivity;
import com.thavelka.feedme.utils.UserManager;

import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class ListingsActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener {

    private Realm realm;
    private RealmResults<Listing> listings;
    private RealmChangeListener<RealmResults<Listing>> changeListener;
    private ListingAdapter adapter;
    private String day = "1";

    @BindView(R.id.listings_navigation) BottomNavigationView bottomNavigationView;
    @BindView(R.id.listings_recyclerView) RecyclerView recyclerView;
    @BindView(R.id.listings_fab) FloatingActionButton addButton;
    @BindView(R.id.listings_empty_view) View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        setContentView(R.layout.activity_listings);
        ButterKnife.bind(this);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        day = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
        setTitle(Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US));

        listings = realm.where(Listing.class).contains("days", day).findAll();
        changeListener = element -> {
            if (element != null && element.isValid() && element.isLoaded() && adapter != null) {
                adapter.refresh();
            }
        };
        listings.addChangeListener(changeListener);
        adapter = new ListingAdapter(this, listings.where().equalTo("type", Listing.TYPE_FOOD).findAll(),
                listing -> Toast.makeText(this, "Tapped listing", Toast.LENGTH_SHORT).show());
        adapter.setEmptyView(emptyView);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        // swipe delete (temporary)
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                Listing listing = listings.get(position);
                adapter.notifyItemRemoved(position);
                realm.executeTransaction(realm1 -> listing.deleteFromRealm());
            }
        });
        helper.attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onDestroy() {
        if (changeListener != null) listings.removeChangeListener(changeListener);
        super.onDestroy();
        if (realm != null) realm.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.listings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                UserManager.logoutActiveUser();
                Intent i = new Intent(this, AuthActivity.class);
                startActivity(i);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_food:
                adapter.setListings(listings.where().equalTo("type", Listing.TYPE_FOOD).findAll());
                return true;
            case R.id.navigation_drinks:
                adapter.setListings(listings.where().equalTo("type", Listing.TYPE_DRINK).findAll());
                return true;
            case R.id.navigation_entertainment:
                adapter.setListings(listings.where().equalTo("type", Listing.TYPE_ENTERTAINMENT).findAll());
                return true;
        }
        return false;
    }

    @OnClick(R.id.listings_fab) void onClickFab() {
        Intent i = new Intent(this, EditListingActivity.class);
        switch (bottomNavigationView.getSelectedItemId()) {
            case R.id.navigation_food:
                i.putExtra(EditListingActivity.ARG_LISTING_TYPE, Listing.TYPE_FOOD);
                break;
            case R.id.navigation_drinks:
                i.putExtra(EditListingActivity.ARG_LISTING_TYPE, Listing.TYPE_DRINK);
                break;
            case R.id.navigation_entertainment:
                i.putExtra(EditListingActivity.ARG_LISTING_TYPE, Listing.TYPE_ENTERTAINMENT);
                break;
        }
        startActivity(i);
    }
}
