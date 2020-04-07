package com.rtg.finalproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder> {
    //Globals
    //SELF is a constant used to identify the chat layout for the sender
    private int SELF = 100;
    //Contains the list of messages to be shown on view
    private ArrayList<Message> messageArrayList;

    public class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder{
        TextView message = ((TextView) this.itemView.findViewById(R.id.message));

        private ViewHolder(View view) {
            super(view);
        }
    }

    //Create an instance of chat adapter
    public ChatAdapter(ArrayList<Message> messageArrayList){
        this.messageArrayList = messageArrayList;
    }

    //This function checks if the type is of self if not then loads the receiver view else load the sender view
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if(viewType == this.SELF){
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_self,parent,false);
        }else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_jarvis,parent,false);
        }
        return  new ViewHolder(itemView);
    }

    public int getItemViewType(int position){
        if(((Message) this.messageArrayList.get(position)).getId().equals("1")){
            return  this.SELF;
        }
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = (Message) this.messageArrayList.get(position);
        message.setMessage(message.getMessage());
        ((ViewHolder) holder).message.setText(message.getMessage());
    }

    @Override
    public int getItemCount() {
        return this.messageArrayList.size();
    }




}
