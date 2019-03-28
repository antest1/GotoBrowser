package com.antest1.gotobrowser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import androidx.recyclerview.widget.RecyclerView;

public class SubtitleLocaleAdapter extends RecyclerView.Adapter<SubtitleLocaleAdapter.ItemViewHolder> {
    ArrayList<JsonObject> mItems;
    private OnItemClickListener selector, downloader;

    public interface OnItemClickListener {
        void onItemClick(JsonObject item);
    }

    public SubtitleLocaleAdapter(ArrayList<JsonObject> items, OnItemClickListener selector, OnItemClickListener downloader) {
        this.mItems = items;
        this.selector = selector;
        this.downloader = downloader;
    }

    public void clearLocaleData() {
        mItems.clear();
    }

    public void addLocaleData(JsonObject data) {
        mItems.add(data);
        Collections.sort(mItems, new LocaleSort());
    }

    public void itemCommitUpdate(String locale_code) {
        for (int i = 0; i < mItems.size(); i++) {
            JsonObject item = mItems.get(i);
            if (item.get("locale_code").getAsString().equals(locale_code)) {
                item.addProperty("current_commit", item.get("latest_commit").getAsString());
            }
            mItems.set(i, item);
        }
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtitle, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        JsonObject item = mItems.get(position);
        holder.bind(item, position, selector, downloader);
        if (item.get("selected").getAsBoolean()) {
            holder.selected.setBackgroundResource(R.color.colorAccent);
        } else {
            holder.selected.setBackgroundResource(R.color.colorListItem);
        }
        holder.localeLabel.setText(item.get("locale_label").getAsString());
        String current_commit = item.get("current_commit").getAsString();
        if (VersionDatabase.isDefaultValue(current_commit)) {
            current_commit = "(none)";
        } else {
            current_commit = current_commit.substring(0, 7);
        }
        String latest_commit = item.get("latest_commit").getAsString().substring(0, 7);
        String locale_info_text = String.format(Locale.US,
                "Current: %s / Latest: %s", current_commit, latest_commit);
        holder.localeInfo.setText(locale_info_text);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView selected, localeLabel, localeInfo, downloadButton;
        public ItemViewHolder(View itemView) {
            super(itemView);
            selected = itemView.findViewById(R.id.subtitle_selected);
            localeLabel = itemView.findViewById(R.id.subtitle_locale_label);
            localeInfo = itemView.findViewById(R.id.subtitle_locale_info);
            downloadButton = itemView.findViewById(R.id.subtitle_download);

        }

        public void bind(final JsonObject item, int position, final OnItemClickListener selector, final OnItemClickListener downloader) {
            itemView.setOnClickListener(v -> {
                for (int i = 0; i < mItems.size(); i++) {
                    mItems.get(i).addProperty("selected", i == position);
                }
                selector.onItemClick(item);
            });
            itemView.findViewById(R.id.subtitle_download).setOnClickListener(v -> downloader.onItemClick(item));
        }
    }

    class LocaleSort implements Comparator<JsonObject> { @Override
        public int compare(JsonObject a, JsonObject b) {
            return b.get("locale_code").getAsString().compareTo(a.get("locale_code").getAsString());
        }
    }
}
