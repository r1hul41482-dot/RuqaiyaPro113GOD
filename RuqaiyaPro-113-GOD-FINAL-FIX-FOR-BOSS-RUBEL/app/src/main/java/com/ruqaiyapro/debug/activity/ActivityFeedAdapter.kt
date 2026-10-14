package com.ruqaiyapro.debug.activity

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ruqaiyapro.debug.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityFeedAdapter : ListAdapter<ActivityLogEntity, ActivityFeedAdapter.ActivityViewHolder>(DiffCallback) {

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvLogTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvLogDesc)
        val tvTime: TextView = itemView.findViewById(R.id.tvLogTime)
        val tvTag: TextView = itemView.findViewById(R.id.tvLogTag)
        val tvStatus: TextView = itemView.findViewById(R.id.tvLogStatus)

        private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(item: ActivityLogEntity) {
            tvTitle.text = item.title
            tvDesc.text = item.description
            tvTime.text = sdf.format(Date(item.timestamp))
            tvTag.text = item.eventType
            tvStatus.text = item.status

            val statusColor = when (item.status) {
                "SUCCESS" -> Color.parseColor("#00E676")
                "FAILED" -> Color.parseColor("#FF1744")
                "WARNING" -> Color.parseColor("#FFD600")
                else -> Color.parseColor("#00E5FF")
            }
            tvStatus.setTextColor(statusColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity_log, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ActivityLogEntity>() {
        override fun areItemsTheSame(oldItem: ActivityLogEntity, newItem: ActivityLogEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ActivityLogEntity, newItem: ActivityLogEntity): Boolean = oldItem == newItem
    }
}
