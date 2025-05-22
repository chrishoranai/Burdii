package com.app.burdii.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.databinding.ItemMemberBinding // We will create this layout next

class MemberAdapter(private val onItemClick: (String) -> Unit) :
    ListAdapter<String, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return MemberViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val memberName = getItem(position)
        holder.bind(memberName)
    }

    class MemberViewHolder(private val binding: ItemMemberBinding, private val onItemClick: (String) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener { onItemClick(binding.memberNameTextView.text.toString()) }
        }

        fun bind(memberName: String) {
            // TODO: Bind memberName to a TextView in item_member.xml
            binding.memberNameTextView.text = memberName
        }
    }
}

class MemberDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}