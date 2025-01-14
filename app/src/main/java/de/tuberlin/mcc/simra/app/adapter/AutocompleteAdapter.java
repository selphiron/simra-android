package de.tuberlin.mcc.simra.app.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.tuberlin.mcc.simra.app.entities.AddressPair;

public class AutocompleteAdapter extends ArrayAdapter<AddressPair> implements Filterable {
    private final List<AddressPair> suggestions;

    public void setSuggestions(List<AddressPair> additions) {
        suggestions.clear();
        suggestions.addAll(additions);
    }

    public AutocompleteAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        suggestions = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Nullable
    @Override
    public AddressPair getItem(int position) {
        return suggestions.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults f = new FilterResults();
                if (constraint != null) {
                    f.values = suggestions;
                    f.count = suggestions.size();
                }
                return f;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0)
                    notifyDataSetChanged();
                else
                    notifyDataSetInvalidated();
            }
        };
    }
}
