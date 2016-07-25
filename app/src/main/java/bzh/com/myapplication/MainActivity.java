package bzh.com.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<String> origin = Arrays.asList("Q,W,E,R,T,Y,U,I,O,P,A,S,D,F,G,H,J,K,L,Z,X,C,V,B,N,M,Q,W,E,R,T,Y,U,I,O,P,A,S,D,F,G,H,J,K,L,Z,X,C,V,B,N,M,Q,W,E,R,T,Y,U,I,O,P,A,S,D,F,G,H,J,K,L,Z,X,C,V,B,N,M,Q,W,E,R,T,Y,U,I,O,P,A,S,D,F,G,H,J,K,L,Z,X,C,V,B,N,M,Q,W,E,R,T,Y,U,I,O,P,A,S,D,F,G,H,J,K,L,Z,X,C,V,B,N,M".split(","));
        List<String> strings = new ArrayList<>(origin);
        final MyAdapter adapter = new MyAdapter(strings);
        adapter.setOnRecyclerViewItemClickListener(new MyAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Log.d(TAG, "onItemClick() called with: " + " position = [" + position + "]");
                adapter.remove(position);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutFrozen(true);
    }

    static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

        final List<String> mData;

        MyAdapter(List<String> mData) {
            this.mData = mData;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, final int viewType) {
            Context context = parent.getContext();
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(R.layout.item_test, parent, false);
            final MyViewHolder viewHolder = new MyViewHolder(view);

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onRecyclerViewItemClickListener != null) {
                        int adapterPosition = viewHolder.getAdapterPosition();
//                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            onRecyclerViewItemClickListener.onItemClick(v, adapterPosition);
//                        }
                    }
                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder viewHolder, int position) {
            TextView textView = viewHolder.textView;
            textView.setText(mData.get(viewHolder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        private OnRecyclerViewItemClickListener onRecyclerViewItemClickListener;

        public void setOnRecyclerViewItemClickListener(OnRecyclerViewItemClickListener onRecyclerViewItemClickListener) {
            this.onRecyclerViewItemClickListener = onRecyclerViewItemClickListener;
        }

        public void remove(int position) {
            Log.d(TAG, "remove() called with: " + "position = [" + position + "]");
            mData.remove(position);
            notifyItemRemoved(position);
        }

        interface OnRecyclerViewItemClickListener {

            void onItemClick(View view, int position);
        }
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        public final TextView textView;

        public MyViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.textView);
        }
    }
}

