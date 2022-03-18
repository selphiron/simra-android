package de.tuberlin.mcc.simra.app.adapter;

import static de.tuberlin.mcc.simra.app.util.RoadUtil.getInstructionContent;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.osmdroid.bonuspack.routing.RoadNode;

import java.util.List;

import de.tuberlin.mcc.simra.app.R;

public class NavigationItemAdapter extends RecyclerView.Adapter<NavigationItemAdapter.ViewHolder> {

    private final List<RoadNode> navInstructions;
    private final Context context;

    public NavigationItemAdapter(List<RoadNode> navInstructions, Context context) {
        this.navInstructions = navInstructions;
        this.context = context;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        // inflate row layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View navRowView = inflater.inflate(R.layout.row_navigation, parent, false);
        return new ViewHolder(navRowView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageView navIcon = holder.navIcon;
        TextView title = holder.navItemTitle;
        TextView duration = holder.navItemDuration;
        RoadNode item = navInstructions.get(position);
        Pair<Pair<String, String>, Drawable> content = getInstructionContent(item, position, context);
        title.setText(content.first.first);
        duration.setText(content.first.second);
        navIcon.setImageDrawable(content.second);
    }

    @Override
    public int getItemCount() {
        return navInstructions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView navIcon;
        TextView navItemTitle, navItemDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            navIcon = itemView.findViewById(R.id.navigationStepImage);
            navItemTitle = itemView.findViewById(R.id.navigationStepTitle);
            navItemDuration = itemView.findViewById(R.id.navigationStepDuration);
        }
    }
}
